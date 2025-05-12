import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http'; // Eklendi
import { AdminService } from '../../admin.service'; // Yol düzeltildi
import { OrderResponseDto } from '../../../../shared/models/order.model';
import { OrderStatus } from '../../../../shared/enums/order-status.enum';
import { NotificationService } from '../../../../core/services/notification.service';

@Component({
  selector: 'app-admin-order-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './admin-order-detail.component.html',
  styleUrls: ['./admin-order-detail.component.scss']
})
export class AdminOrderDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private adminService = inject(AdminService);
  private notificationService = inject(NotificationService);
  private fb = inject(FormBuilder);

  order = signal<OrderResponseDto | null>(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);
  isUpdatingStatus = signal(false);

  updateStatusForm: FormGroup;
  orderStatusOptions = Object.values(OrderStatus);
  OrderStatusEnum = OrderStatus; // For template access

  constructor() {
    this.updateStatusForm = this.fb.group({
      newStatus: ['', Validators.required],
      trackingNumber: [''] // Only visible/required if status is SHIPPED
    });
  }

  ngOnInit(): void {
    const orderIdParam = this.route.snapshot.paramMap.get('id');
    if (orderIdParam) {
      this.loadOrderDetail(+orderIdParam);
    } else {
      this.isLoading.set(false);
      this.errorMessage.set('Order ID not found in route.');
      this.notificationService.showError('Order ID not found.');
    }
  }

  loadOrderDetail(orderId: number): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.adminService.getOrderDetailsForAdmin(orderId).subscribe({
      next: (data: OrderResponseDto) => { // Tip eklendi
        this.order.set(data);
        this.updateStatusForm.patchValue({ newStatus: data.status });
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => { // Tip eklendi
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load order details.');
        this.notificationService.showError(err.error?.message || 'Failed to load order details.');
        console.error(`Error loading order details for admin, ID ${orderId}:`, err);
      }
    });
  }

  onStatusChange(event: Event): void {
    const selectedStatus = (event.target as HTMLSelectElement).value as OrderStatus;
    const trackingControl = this.updateStatusForm.get('trackingNumber');
    if (selectedStatus === OrderStatus.SHIPPED) {
      trackingControl?.setValidators([Validators.required, Validators.minLength(3)]);
    } else {
      trackingControl?.clearValidators();
    }
    trackingControl?.updateValueAndValidity();
  }

  submitUpdateStatus(): void {
    if (this.updateStatusForm.invalid) {
      this.notificationService.showError('Please select a status and provide tracking number if required.');
      return;
    }

    const currentOrder = this.order();
    if (!currentOrder) return;

    this.isUpdatingStatus.set(true);
    const newStatus = this.updateStatusForm.value.newStatus as OrderStatus;
    const trackingNumber = this.updateStatusForm.value.trackingNumber;

    // Önce durumu güncelle, sonra gerekirse kargo numarasını ekle (veya tek bir serviste birleştir)
    this.adminService.updateOrderStatusForAdmin(currentOrder.id, newStatus).subscribe({
      next: (updatedOrder: OrderResponseDto) => { // Tip eklendi
        if (newStatus === OrderStatus.SHIPPED && trackingNumber) {
          this.adminService.addTrackingNumberForAdmin(currentOrder.id, trackingNumber).subscribe({
            next: (orderWithTracking: OrderResponseDto) => { // Tip eklendi
              this.order.set(orderWithTracking);
              this.notificationService.showSuccess('Order status and tracking number updated successfully!');
              this.isUpdatingStatus.set(false);
            },
            error: (errTrack: HttpErrorResponse) => this.handleUpdateError(errTrack, 'tracking number') // Tip eklendi
          });
        } else {
          this.order.set(updatedOrder);
          this.notificationService.showSuccess('Order status updated successfully!');
          this.isUpdatingStatus.set(false);
        }
      },
      error: (errStatus: HttpErrorResponse) => this.handleUpdateError(errStatus, 'status') // Tip eklendi
    });
  }

  private handleUpdateError(error: HttpErrorResponse, type: string): void { // Tip eklendi
    this.isUpdatingStatus.set(false);
    const message = error.error?.message || `Failed to update order ${type}.`;
    this.notificationService.showError(message);
    console.error(`Error updating order ${type}:`, error);
  }
}
