// src/app/features/cart/cart-view/cart-view.component.ts
import { Component, inject, OnInit, Signal, signal } from '@angular/core'; // signal eklendi
import { CommonModule, CurrencyPipe , NgOptimizedImage} from '@angular/common';
import { RouterLink } from '@angular/router';
import { CartService } from '../cart.service';
import { Cart, CartItem, UpdateCartItemRequest } from '../../../shared/models/cart.model';
import { FormsModule } from '@angular/forms'; // Miktar güncelleme için
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-cart-view',
  standalone: true,
  imports: [CommonModule, RouterLink, CurrencyPipe, FormsModule, NgOptimizedImage],
  templateUrl: './cart-view.component.html',
  styleUrls: ['./cart-view.component.scss'],
})
export class CartViewComponent  {
  cartService = inject(CartService);
  authService = inject(AuthService);
  cart: Signal<Cart | null> = this.cartService.cart;
  isLoading = signal(false);

  // ngOnInit(): void {
  //   // CartService constructor'ı zaten login durumuna göre yüklüyor.
  //   // Eğer cart hala null ise ve kullanıcı login olmuşsa, bir yükleme denemesi yapılabilir.
  //   // Ancak effect bunu zaten halletmeli.
  //   // Giriş yapılıp yapılmadığını kontrol et (AuthService üzerinden)

  //   if (this.authService.isAuthenticated()) {
  //     if (!this.cartService.cart()) {
  //       this.isLoading.set(true);
  //       this.cartService.loadCart().subscribe({
  //         complete: () => this.isLoading.set(false),
  //         error: () => this.isLoading.set(false)
  //       });
  //     }
  //   } else {
  //     // Kullanıcı giriş yapmamışsa sepeti göstermeye gerek yok,
  //     // zaten bu sayfaya guard ile gelinmemeli.
  //     console.warn('CartViewComponent: User not authenticated. Cart will not be loaded.');
  //   }
  // }

  updateQuantity(item: CartItem, event: Event): void { // İkinci parametre 'event: Event' olarak değiştirildi
    const inputElement = event.target as HTMLInputElement;
    const newQuantity = parseInt(inputElement.value, 10);

    if (isNaN(newQuantity) || newQuantity < 1) {
      alert('Quantity must be at least 1. To remove, use the delete button.');
      inputElement.value = item.quantity.toString(); // Input'u eski değere döndür
      return;
    }

    if (item.productStock !== undefined && newQuantity > item.productStock) {
      alert(`Cannot add more than ${item.productStock} items for ${item.productName || 'this product'}.`);
      inputElement.value = item.quantity.toString(); // Input'u eski değere döndür
      return;
    }

    const request: UpdateCartItemRequest = { quantity: newQuantity };
    this.isLoading.set(true);
    this.cartService.updateItemQuantity(item.productId, request).subscribe({
      next: (updatedCart) => {
        console.log(`Quantity updated for product ${item.productId}`, updatedCart);
        // Sepet servisi cart sinyalini güncellediği için input'un değeri
        // otomatik olarak güncellenmeli (eğer [value]="item.quantity" doğru bağlanmışsa
        // ve item referansı güncelleniyorsa).
        // Eğer otomatik güncellenmiyorsa, burada inputElement.value'yu tekrar set etmeye gerek yok
        // çünkü sayfa zaten yeni cart verisiyle render olacak.
      },
      error: (err) => {
        console.error(`Failed to update quantity for ${item.productId}`, err);
        inputElement.value = item.quantity.toString(); // Hata durumunda input'u eski değere döndür
        this.isLoading.set(false);
        const errorMessage = err.error?.message || err.message || 'An error occurred while updating quantity.';
        alert(`Error: ${errorMessage}`);
      },
      complete: () => this.isLoading.set(false)
    });
  }


  parseQuantity(value: string): number {
    const num = parseInt(value, 10);
    return isNaN(num) || num < 1 ? 1 : num; // Geçersizse veya 1'den küçükse 1'e fallback yap
  }

  removeItemFromCart(productId: number): void {
    if (confirm('Are you sure you want to remove this item from your cart?')) {
      this.isLoading.set(true);
      this.cartService.removeItem(productId).subscribe({
        error: (err) => {
          console.error(`Failed to remove item ${productId}`, err);
          this.isLoading.set(false);
          alert('Failed to remove item.');
        },
        complete: () => this.isLoading.set(false)
      });
    }
  }

  clearUserCart(): void {
    if (confirm('Are you sure you want to clear your cart?')) {
      this.isLoading.set(true);
      this.cartService.clearCart().subscribe({
        error: (err) => {
          console.error('Failed to clear cart', err);
          this.isLoading.set(false);
          alert('Failed to clear cart.');
        },
        complete: () => this.isLoading.set(false)
      });
    }
  }
}
