import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NgxChartsModule, ScaleType } from '@swimlane/ngx-charts';
import { OrderService } from '../../orders/order.service';
// OrderResponseDto artık direkt kullanılmayacak, MonthlySalesDto kullanılacak
// import { OrderResponseDto } from '../../../shared/models/order.model';
import { AuthService } from '../../../core/services/auth.service'; // Mevcut kullanıcı ID'sini almak için
import { MonthlySalesDto } from '../../../shared/models/monthly-sales.model'; // Yeni DTO import edildi

// ChartData arayüzü MonthlySalesDto ile aynı yapıda olduğu için kaldırılabilir veya MonthlySalesDto kullanılabilir.
// Şimdilik ChartData kalsın, gelen veriyi map ederiz.
interface ChartData {
  name: string;
  value: number;
}

@Component({
  selector: 'app-seller-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, NgxChartsModule],
  templateUrl: './seller-dashboard.component.html',
  styleUrls: ['./seller-dashboard.component.scss']
})
export class SellerDashboardComponent implements OnInit {
  private orderService = inject(OrderService);
  private authService = inject(AuthService);

  salesData: ChartData[] = []; // Bu ChartData[] olarak kalabilir, MonthlySalesDto[] -> ChartData[] dönüşümü yaparız
  isLoadingChart = signal(true);

  view: [number, number] = [700, 400];
  showXAxis = true;
  showYAxis = true;
  gradient = false;
  showLegend = true;
  showXAxisLabel = true;
  xAxisLabel = 'Month';
  showYAxisLabel = true;
  yAxisLabel = 'My Sales (TL)';
  colorScheme = {
    name: 'vivid',
    selectable: true,
    group: ScaleType.Ordinal,
    domain: ['#647c8a', '#3f51b5', '#2196f3', '#00b862', '#afdf0a'],
  };

  constructor() {
    console.log('SellerDashboardComponent: Constructor called');
  }

  ngOnInit(): void {
    console.log('SellerDashboardComponent: ngOnInit called');
    this.loadSellerSalesData();
  }

  loadSellerSalesData(): void {
    this.isLoadingChart.set(true);
    const currentUser = this.authService.currentUser();
    console.log('SellerDashboard: currentUser from AuthService:', currentUser);

    if (!currentUser || !currentUser.id) {
      console.error('SellerDashboard: Seller ID not found. Cannot load sales summary.');
      this.isLoadingChart.set(false);
      this.salesData = [{ name: 'Error: Seller not identified', value: 0 }];
      return;
    }
    console.log('SellerDashboard: Attempting to load sales summary for sellerId:', currentUser.id);

    this.orderService.getSellerMonthlySalesSummary().subscribe({
      next: (summaryData: MonthlySalesDto[]) => {
        console.log('SellerDashboard: Raw sales summary from service:', summaryData);
        // MonthlySalesDto[]'i ChartData[]'e map edelim
        this.salesData = summaryData.map(dto => ({
          name: dto.monthYear, // Backend "YYYY-MM" formatında dönecek, bu grafikte iyi görünebilir
          value: dto.totalSales
        }));
        console.log('SellerDashboard: Processed salesData for chart:', this.salesData);
        this.isLoadingChart.set(false);
        console.log('SellerDashboard: isLoadingChart set to false.');
      },
      error: (err) => {
        console.error('SellerDashboard: Error loading seller sales summary:', err);
        this.salesData = [{ name: 'Error loading sales data', value: 0 }];
        this.isLoadingChart.set(false);
        console.log('SellerDashboard: isLoadingChart set to false due to error.');
      }
    });
  }

  // processOrderDataForChart metodu artık gerekli değil, çünkü backend'den hazır gruplanmış veri geliyor.
  // Eğer backend'den ham sipariş listesi gelseydi bu metod kullanılabilirdi.
  // Şimdilik bu metodu silebiliriz veya yorum satırına alabiliriz.
  /*
  processOrderDataForChart(orders: OrderResponseDto[], sellerId: number): ChartData[] {
    // ... eski kod ...
  }
  */

  onSelect(event: any): void {
    console.log('Chart item selected', event);
  }
}
