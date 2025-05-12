// src/app/features/auth/register/register.component.ts
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { RegisterRequest } from '../../../shared/models/user.model';
import { Role } from '../../../shared/enums/role.enum'; // Role enum import et

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'],
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  registerForm: FormGroup;
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  // Role enum'ını template'te kullanmak için
  RoleEnum = Role; // EKLENDİ

  constructor() {
    this.registerForm = this.fb.group({
      firstName: ['', [Validators.required]], // EKLENDİ (Backend DTO'suna göre)
      lastName: ['', [Validators.required]],  // EKLENDİ (Backend DTO'suna göre)
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      role: [Role.CUSTOMER, [Validators.required]] // EKLENDİ, varsayılan olarak CUSTOMER
    });
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const userData: RegisterRequest = this.registerForm.value;
    console.log('Submitting registration data:', userData); // Gönderilen veriyi logla

    this.authService.register(userData).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        // Backend'den gelen mesajı kullan veya varsayılan bir mesaj göster
        this.successMessage.set(response.message || 'Registration successful! Please login.');
        this.registerForm.reset({ role: Role.CUSTOMER }); // Formu temizle, rolü varsayılana döndür
      },
      error: (err) => {
        this.isLoading.set(false);
        if (err.error && err.error.message) {
          // Eğer backend'den gelen `err.error.data` bir map ise ve validasyon hatalarını içeriyorsa,
          // bu hataları daha detaylı gösterebilirsin.
          if (typeof err.error.data === 'object' && err.error.data !== null) {
            let validationErrors = '';
            for (const key in err.error.data) {
              validationErrors += `${key}: ${err.error.data[key]} `;
            }
            this.errorMessage.set(validationErrors.trim() || err.error.message);
          } else {
            this.errorMessage.set(err.error.message);
          }
        } else if (err.status === 400 || err.status === 409) {
          this.errorMessage.set(err.error?.message || 'Registration failed. Username or email may already exist.');
        } else {
          this.errorMessage.set('An unexpected error occurred during registration. Please try again.');
        }
        console.error('Registration failed:', err);
      },
    });
  }
}
