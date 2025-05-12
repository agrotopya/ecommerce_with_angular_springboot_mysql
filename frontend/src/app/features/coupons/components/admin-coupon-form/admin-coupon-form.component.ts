import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-admin-coupon-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-coupon-form.component.html',
  styleUrl: './admin-coupon-form.component.scss'
})
export class AdminCouponFormComponent {

}
