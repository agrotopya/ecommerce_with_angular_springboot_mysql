// src/app/features/seller/dashboard/dashboard.component.ts
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router'; // RouterLink kaldırıldı

@Component({
  selector: 'app-seller-dashboard', // app-seller-dashboard -> app-dashboard olarak kalabilir, selector önemli değil şimdilik
  standalone: true,
  imports: [CommonModule], // RouterLink kaldırıldı
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent {
  private router = inject(Router);

  constructor() { }

  navigateToMyProducts(): void {
    console.log('Attempting to navigate to /seller/products');
    this.router.navigate(['/seller/products']).then(success => {
      if (success) {
        console.log('Navigation to /seller/products successful');
      } else {
        console.error('Navigation to /seller/products failed');
      }
    }).catch(err => {
      console.error('Error during navigation to /seller/products:', err);
    });
  }

  // Manage Orders için de benzer bir metod eklenebilir (şimdilik alert ile bırakılmıştı)
  navigateToMyOrders(): void {
    // Şimdilik alert, ileride gerçek yönlendirme
    alert('Order management coming soon!');
    // this.router.navigate(['/seller/orders']);
  }
}
