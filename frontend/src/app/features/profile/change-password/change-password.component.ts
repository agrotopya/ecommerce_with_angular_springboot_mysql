import { Component, OnInit, inject } from '@angular/core'; // inject eklendi
import { CommonModule } from '@angular/common'; // CommonModule eklendi
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms'; // ReactiveFormsModule eklendi
import { Router, RouterLink } from '@angular/router'; // RouterLink eklendi
import { AuthService } from '../../../core/services/auth.service'; // UserService yerine AuthService
import { ChangePasswordRequest, ApiResponse } from '../../../shared/models/user.model'; // ApiResponse eklendi
import { HttpErrorResponse } from '@angular/common/http'; // HttpErrorResponse eklendi
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar'; // MatSnackBarModule eklendi
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';


@Component({
  selector: 'app-change-password',
  standalone: true, // standalone yapıldı
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink, // RouterLink eklendi
    MatSnackBarModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './change-password.component.html', // Bu yol da yeni konuma göre ayarlanacak
  styleUrls: ['./change-password.component.scss'] // Bu yol da yeni konuma göre ayarlanacak
})
export class ChangePasswordComponent implements OnInit {
  // fb, userService, snackBar, router inject ile alınabilir. Şimdilik constructor'da bırakıyoruz.
  changePasswordForm!: FormGroup;
  isLoading = false;
  hideCurrentPassword = true;
  hideNewPassword = true;
  hideConfirmPassword = true;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService, // userService -> authService
    private snackBar: MatSnackBar,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.changePasswordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(6)]], // Add more validators as needed
      confirmPassword: ['', Validators.required]
    }, { validator: this.passwordMatchValidator });
  }

  passwordMatchValidator(form: FormGroup) {
    const newPassword = form.get('newPassword')?.value;
    const confirmPassword = form.get('confirmPassword')?.value;
    return newPassword === confirmPassword ? null : { mismatch: true };
  }

  onSubmit(): void {
    if (this.changePasswordForm.invalid) {
      return;
    }

    this.isLoading = true;
    const request: ChangePasswordRequest = {
      currentPassword: this.changePasswordForm.value.currentPassword,
      newPassword: this.changePasswordForm.value.newPassword
    };

    this.authService.changePassword(request).subscribe({ // userService -> authService
      next: (response: ApiResponse) => { // response tipi ApiResponse oldu
        this.isLoading = false;
        if (response.success) {
          this.snackBar.open('Password changed successfully!', 'Close', { duration: 3000 });
          this.router.navigate(['/profile']);
        } else {
          this.snackBar.open(response.message || 'Failed to change password.', 'Close', { duration: 5000 });
        }
      },
      error: (err: HttpErrorResponse) => { // err tipi HttpErrorResponse oldu
        this.isLoading = false;
        const apiError = err.error as ApiResponse; // Hata mesajını almak için
        this.snackBar.open(`Error changing password: ${apiError?.message || err.message || 'Unknown error'}`, 'Close', { duration: 5000 });
      }
    });
  }
}
