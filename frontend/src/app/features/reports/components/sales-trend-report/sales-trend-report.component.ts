import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
// import { ReactiveFormsModule } from '@angular/forms'; // Filtreler için
// import { NgChartsModule } from 'ng2-charts'; // Grafik için

@Component({
  selector: 'app-sales-trend-report',
  standalone: true,
  imports: [CommonModule /*, ReactiveFormsModule, NgChartsModule */],
  templateUrl: './sales-trend-report.component.html',
  styleUrl: './sales-trend-report.component.scss'
})
export class SalesTrendReportComponent {

}
