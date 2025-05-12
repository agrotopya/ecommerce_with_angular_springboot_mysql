import { Component, Input } from '@angular/core'; // Input eklendi
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button'; // Buton için

@Component({
  selector: 'app-wishlist-button',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule],
  templateUrl: './wishlist-button.component.html',
  styleUrl: './wishlist-button.component.scss'
})
export class WishlistButtonComponent {
  @Input() productId!: number;
  @Input() isInWishlist: boolean = false;
  // TODO: Add WishlistService and implement toggleWishlist method
}
