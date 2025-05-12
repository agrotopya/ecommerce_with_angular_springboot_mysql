import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, ValidatorFn, AbstractControl, ValidationErrors } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ApiResponse } from '../../../shared/models/user.model';
import { HttpErrorResponse } from '@angular/common/http';

// Custom validator for matching passwords
export const passwordMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password');
  const confirmPassword = control.get('confirmPassword');
  return password && confirmPassword && password.value !== confirmPassword.value ? { passwordMismatch: true } : null;
};

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss'
})
export class ResetPasswordComponent implements OnInit {
  resetPasswordForm: FormGroup;
  authService = inject(AuthService);
  fb = inject(FormBuilder);
  router = inject(Router);
  route = inject(ActivatedRoute);

  token: string | null = null;
  isLoading = false;
  message: string | null = null;
  isError = false;

  constructor() {
    this.resetPasswordForm = this.fb.group({
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: passwordMatchValidator });
  }

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(params => {
      this.token = params.get('token');
      if (!this.token) {
        this.message = 'Invalid or missing password reset token.';
        this.isError = true;
        // Optionally redirect or disable form
      }
    });
  }

  onSubmit(): void {
    if (this.resetPasswordForm.invalid) {
      this.resetPasswordForm.markAllAsTouched();
      return;
    }
    if (!this.token) {
      this.message = 'Password reset token is missing. Please request a new link.';
      this.isError = true;
      return;
    }

    this.isLoading = true;
    this.message = null;
    this.isError = false;
    const newPassword = this.resetPasswordForm.value.password;

    this.authService.resetPassword(this.token, newPassword).subscribe({
      next: (response: ApiResponse) => {
        this.isLoading = false;
        this.message = response.message || 'Your password has been reset successfully. You can now login.';
        this.isError = false;
        this.resetPasswordForm.reset();
        // Optionally redirect to login page after a delay
        setTimeout(() => this.router.navigate(['/auth/login']), 3000);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading = false;
        const apiError = err.error as ApiResponse;
        this.message = apiError?.message || err.message || 'An error occurred. Please try again.';
        this.isError = true;
      }
    });
  }
}
