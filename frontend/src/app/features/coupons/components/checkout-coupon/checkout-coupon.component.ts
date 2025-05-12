import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // veya ReactiveFormsModule

@Component({
  selector: 'app-checkout-coupon',
  standalone: true,
  imports: [CommonModule, FormsModule], // veya ReactiveFormsModule
  templateUrl: './checkout-coupon.component.html',
  styleUrl: './checkout-coupon.component.scss'
})
export class CheckoutCouponComponent {

}
