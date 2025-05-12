// src/app/features/profile/profile/profile.component.ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { User, UserProfileUpdateRequest } from '../../../shared/models/user.model'; // UserResponse -> User
import { UserService } from '../user.service';
// import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private userService = inject(UserService);
  // private notificationService = inject(NotificationService);

  profileForm!: FormGroup;
  currentUser = this.authService.currentUser; // AuthService'ten mevcut kullanıcıyı al
  isLoading = signal(false);
  isEditing = signal(false);
  errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.initForm();
    this.loadUserProfile();
  }

  private initForm(): void {
    this.profileForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      // email: [{ value: '', disabled: true }], // E-posta genellikle değiştirilemez
      // username: [{ value: '', disabled: true }] // Kullanıcı adı genellikle değiştirilemez
    });
  }

  private loadUserProfile(): void {
    const user = this.currentUser();
    if (user) {
      this.profileForm.patchValue({
        firstName: user.firstName,
        lastName: user.lastName,
        // email: user.email,
        // username: user.username
      });
    } else {
      // Eğer AuthService'ten kullanıcı bilgisi gelmiyorsa, servisten çekmeyi deneyebiliriz
      // Ancak bu senaryo genellikle login sonrası kullanıcı bilgisi AuthService'te olmalı
      console.warn('ProfileComponent: Current user data not found in AuthService.');
      // this.fetchProfileFromServer(); // Gerekirse backend'den çek
    }
  }

  toggleEdit(): void {
    this.isEditing.set(!this.isEditing());
    if (!this.isEditing()) {
      this.loadUserProfile(); // Düzenlemeden çıkarken formu sıfırla/yeniden yükle
    }
  }

  onSubmit(): void {
    if (this.profileForm.invalid) {
      this.markFormGroupTouched(this.profileForm);
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const updateRequest: UserProfileUpdateRequest = {
      firstName: this.profileForm.value.firstName,
      lastName: this.profileForm.value.lastName
    };

    this.userService.updateUserProfile(updateRequest).subscribe({
      next: (updatedUser) => {
        this.isLoading.set(false);
        this.isEditing.set(false);
        this.authService.updateCurrentUser(updatedUser); // AuthService'teki kullanıcıyı güncelle
        // this.notificationService.showSuccess('Profile updated successfully!');
        console.log('Profile updated:', updatedUser);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set('Failed to update profile. Please try again.');
        console.error('Error updating profile:', err);
        // this.notificationService.showError('Failed to update profile.');
      }
    });
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if ((control as FormGroup).controls) {
        this.markFormGroupTouched(control as FormGroup);
      }
    });
  }
}
