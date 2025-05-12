import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
// import { ReactiveFormsModule } from '@angular/forms'; // Filtreler için
// import { NgChartsModule } from 'ng2-charts'; // Grafik için

@Component({
  selector: 'app-product-performance-report',
  standalone: true,
  imports: [CommonModule /*, ReactiveFormsModule, NgChartsModule */],
  templateUrl: './product-performance-report.component.html',
  styleUrl: './product-performance-report.component.scss'
})
export class ProductPerformanceReportComponent {

}
