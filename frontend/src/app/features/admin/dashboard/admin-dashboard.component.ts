// src/app/features/admin/dashboard/admin-dashboard.component.ts
import { Component, OnInit, inject, signal } from '@angular/core'; // signal import edildi
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NgxChartsModule, ScaleType } from '@swimlane/ngx-charts'; // NgxChartsModule import edildi
// import { AdminService } from '../admin.service'; // Gerçek veri için
import { OrderService } from '../../orders/order.service'; // Veya genel bir OrderService
import { OrderResponseDto } from '../../../shared/models/order.model'; // OrderResponseDto import edildi

// Örnek veri tipi (gerçek API yanıtına göre güncellenmeli)
interface ChartData {
  name: string;
  value: number;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, NgxChartsModule], // NgxChartsModule eklendi
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  // private adminService = inject(AdminService); // Gerçek veri için
  private orderService = inject(OrderService); // Gerçek veri için

  // Grafik için örnek veriler
  salesData: ChartData[] = []; // Başlangıçta boş, API'den yüklenecek

  isLoadingChart = signal(true); // Grafik yükleme durumu için sinyal

  // Grafik seçenekleri
  view: [number, number] = [700, 400]; // Grafik boyutu [width, height]
  showXAxis = true;
  showYAxis = true;
  gradient = false;
  showLegend = true;
  showXAxisLabel = true;
  xAxisLabel = 'Month';
  showYAxisLabel = true;
  yAxisLabel = 'Sales (TL)';
  colorScheme = {
    name: 'cool', // Renk şeması adı
    selectable: true,
    group: ScaleType.Ordinal, // ScaleType import edilmeli
    domain: ['#5AA454', '#A10A28', '#C7B42C', '#AAAAAA', '#2E4C6D'], // Örnek renkler
  };

  constructor() {
    // Gerçek veri yükleme mantığı buraya veya ngOnInit'e eklenecek
  }

  ngOnInit(): void {
    this.loadSalesData(); // Gerçek veri yükleme fonksiyonu çağrılabilir
  }

  loadSalesData(): void {
    this.isLoadingChart.set(true);
    // Tüm siparişleri çekmek için size değerini yüksek tutuyoruz, idealde backend'de gruplanmış veri gelmeli.
    this.orderService.getAllOrdersForAdmin(0, 10000, 'orderDate,asc').subscribe({
      next: (response) => {
        this.salesData = this.processOrderDataForChart(response.content);
        this.isLoadingChart.set(false);
      },
      error: (err) => {
        console.error('Error loading sales data for chart:', err);
        // Hata durumunda kullanıcıya bilgi verilebilir.
        this.salesData = [ { name: 'Error loading data', value: 0 }];
        this.isLoadingChart.set(false);
      }
    });
  }

  processOrderDataForChart(orders: OrderResponseDto[]): ChartData[] {
    // Bu fonksiyon, sipariş verilerini aylık satış toplamlarına dönüştürmelidir.
    const monthlySales: { [key: string]: { name: string, value: number, order: number } } = {};
    const monthOrder: { [key: string]: number } = {
      'January': 1, 'February': 2, 'March': 3, 'April': 4, 'May': 5, 'June': 6,
      'July': 7, 'August': 8, 'September': 9, 'October': 10, 'November': 11, 'December': 12
    };

    orders.forEach(order => {
      // Sadece tamamlanmış veya ödenmiş siparişleri dikkate alabiliriz.
      // Örnek: if (order.status === 'DELIVERED' || order.paymentStatus === 'PAID')
      const orderDate = new Date(order.createdAt); // orderDate -> createdAt
      const year = orderDate.getFullYear();
      const monthName = orderDate.toLocaleString('default', { month: 'long' });
      const key = `${year}-${monthName}`; // Yıl ve ay bazında gruplama

      if (!monthlySales[key]) {
        monthlySales[key] = { name: `${monthName} ${year}`, value: 0, order: monthOrder[monthName] + year * 100 };
      }
      monthlySales[key].value += order.finalAmount ?? 0; // Null kontrolü eklendi
    });

    return Object.values(monthlySales)
                 .sort((a, b) => a.order - b.order) // Aylara göre sırala
                 .map(item => ({ name: item.name, value: item.value }));
  }

  onSelect(event: any): void {
    console.log('Chart item selected', event);
  }
}
