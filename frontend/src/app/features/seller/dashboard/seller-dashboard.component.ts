import { Component } from '@angular/core';
import { CommonModule } from '@angular/common'; // CommonModule eklendi
import { RouterModule } from '@angular/router'; // RouterModule eklendi (genellikle dashboard'larda linkler olur)

@Component({
  selector: 'app-seller-dashboard',
  standalone: true, // standalone yapıldı
  imports: [CommonModule, RouterModule], // Temel importlar eklendi
  templateUrl: './seller-dashboard.component.html',
  styleUrl: './seller-dashboard.component.scss' // styleUrl -> styleUrls olmalı, ancak scss dosyası yoksa sorun değil
})
export class SellerDashboardComponent {

}
