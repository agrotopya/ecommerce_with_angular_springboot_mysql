// src/app/features/auth/login/login.component.ts
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { LoginRequest } from '../../../shared/models/user.model'; // Model import edildi
import { ActivatedRoute } from '@angular/router'; // ActivatedRoute eklendi

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
 private route = inject(ActivatedRoute); // ActivatedRoute enjekte edildi
  private returnUrl: string;



  loginForm: FormGroup;
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  constructor() {
    this.loginForm = this.fb.group({
      usernameOrEmail: ['', [Validators.required]],
      password: ['', [Validators.required]],
    });

    // Giriş sayfasında returnUrl'i al
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';
  }


   onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);
    const credentials: LoginRequest = this.loginForm.value;

    this.authService.login(credentials).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.router.navigate([this.returnUrl]); // returnUrl'e veya ana sayfaya yönlendir
      },
      error: (err) => {
        // ... hata yönetimi aynı kalır ...
        this.isLoading.set(false);
        if (err.status === 401 || err.status === 400) {
          this.errorMessage.set('Invalid username/email or password.');
        } else if (err.error && err.error.message) {
          this.errorMessage.set(err.error.message);
        } else {
          this.errorMessage.set('An unexpected error occurred. Please try again.');
        }
        console.error('Login failed:', err);
      },
    });
  }
}

/*CommonModule (ngIf, ngFor için), ReactiveFormsModule ve RouterLink import edildi.
FormBuilder ile loginForm oluşturuldu ve validasyon kuralları eklendi.
onSubmit metodu:
Form geçerli değilse işlemi durdurur.
isLoading sinyalini true yapar.
AuthService.login() çağrılır.
Başarılı olursa, kullanıcı ana sayfaya yönlendirilir.
Hata olursa, errorMessage sinyali güncellenir. */
