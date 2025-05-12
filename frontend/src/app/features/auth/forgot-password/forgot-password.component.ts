import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ApiResponse } from '../../../shared/models/user.model'; // ApiResponse import edildi
import { HttpErrorResponse } from '@angular/common/http'; // HttpErrorResponse import edildi

@Component({
  selector: 'app-forgot-password',
  standalone: true, // standalone yapıldı
  imports: [CommonModule, ReactiveFormsModule], // Gerekli modüller eklendi
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.scss'
})
export class ForgotPasswordComponent {
  forgotPasswordForm: FormGroup;
  authService = inject(AuthService); // AuthService inject edildi
  fb = inject(FormBuilder); // FormBuilder inject edildi
  router = inject(Router); // Router inject edildi

  isLoading = false;
  message: string | null = null;
  isError = false;

  constructor() {
    this.forgotPasswordForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  onSubmit(): void {
    if (this.forgotPasswordForm.invalid) {
      this.forgotPasswordForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.message = null;
    this.isError = false;
    const email = this.forgotPasswordForm.value.email;

    this.authService.forgotPassword(email).subscribe({
      next: (response: ApiResponse) => { // response tipi eklendi
        this.isLoading = false;
        this.message = response.message || 'If an account with that email exists, a password reset link has been sent.';
        this.isError = false;
        this.forgotPasswordForm.reset();
      },
      error: (err: HttpErrorResponse) => { // err tipi eklendi
        this.isLoading = false;
        // err.error genellikle backend'den gelen hata objesini içerir.
        // err.error.message veya err.message kontrolü yapılabilir.
        const apiError = err.error as ApiResponse; // Backend'den ApiResponse formatında hata gelebilir
        this.message = apiError?.message || err.message || 'An error occurred. Please try again.';
        this.isError = true;
      }
    });
  }
}
