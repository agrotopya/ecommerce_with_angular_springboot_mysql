import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-feel-upload-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './feel-upload-form.component.html',
  styleUrl: './feel-upload-form.component.scss'
})
export class FeelUploadFormComponent {

}
