import { Routes } from '@angular/router';
import { SellerProfileComponent } from './components/seller-profile/seller-profile.component'; // SellerProfileComponent yolu varsayılan

export const SELLERS_ROUTES: Routes = [
  {
    path: '', // /sellers/:sellerId'nin ana yolu (sellerId app.routes.ts'den gelecek)
    component: SellerProfileComponent,
    // title: 'Seller Profile | Fibiyo' // Dinamik başlık component içinde ayarlanabilir
  }
  // Gelecekte başka satıcı ile ilgili public yollar buraya eklenebilir.
];
