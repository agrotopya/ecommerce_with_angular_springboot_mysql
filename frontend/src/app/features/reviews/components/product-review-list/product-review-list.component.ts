import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatListModule } from '@angular/material/list'; // Yorum listesi için

@Component({
  selector: 'app-product-review-list',
  standalone: true,
  imports: [CommonModule, MatListModule],
  templateUrl: './product-review-list.component.html',
  styleUrl: './product-review-list.component.scss'
})
export class ProductReviewListComponent {

}
