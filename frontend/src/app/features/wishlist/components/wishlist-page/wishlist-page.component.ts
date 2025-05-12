import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
// ProductCardComponent'i import etmemiz gerekecek
// import { ProductCardComponent } from '../../../products/product-card/product-card.component'; // Örnek yol

@Component({
  selector: 'app-wishlist-page',
  standalone: true,
  imports: [CommonModule /*, ProductCardComponent */], // ProductCardComponent eklenecek
  templateUrl: './wishlist-page.component.html',
  styleUrl: './wishlist-page.component.scss'
})
export class WishlistPageComponent {
  // TODO: Add WishlistService and fetch wishlist items
}
