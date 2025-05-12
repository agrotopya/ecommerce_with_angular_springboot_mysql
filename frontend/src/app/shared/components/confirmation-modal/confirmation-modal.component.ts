import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-confirmation-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './confirmation-modal.component.html',
  styleUrls: ['./confirmation-modal.component.scss']
})
export class ConfirmationModalComponent {
  @Input() title: string = 'Confirm Action';
  @Input() message: string = 'Are you sure you want to proceed?';
  @Input() confirmButtonText: string = 'Confirm';
  @Input() cancelButtonText: string = 'Cancel';
  @Input() confirmButtonClass: string = 'btn-danger'; // Default to danger for delete actions
  @Input() cancelButtonClass: string = 'btn-secondary';

  @Output() confirmed = new EventEmitter<boolean>();

  isVisible = signal(false); // Modalın görünürlüğünü kontrol etmek için

  constructor() {}

  open(): void {
    this.isVisible.set(true);
  }

  close(): void {
    this.isVisible.set(false);
  }

  onConfirm(): void {
    this.confirmed.emit(true);
    this.close();
  }

  onCancel(): void {
    this.confirmed.emit(false);
    this.close();
  }
}
