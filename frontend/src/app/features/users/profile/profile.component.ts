import { Component, OnInit, inject, effect } from '@angular/core'; // inject ve effect eklendi
import { CommonModule } from '@angular/common'; // CommonModule eklendi
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms'; // ReactiveFormsModule eklendi
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../user.service';
// Import user.model.ts'den UserResponse ve UserProfileUpdateRequest'i kullanacak şekilde güncellendi
import { UserResponse, UserProfileUpdateRequest } from '../../../shared/models/user.model';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar'; // MatSnackBarModule eklendi
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-profile',
  standalone: true, // standalone yapıldı
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  // FormBuilder, AuthService, UserService, MatSnackBar inject ile alınabilir
  // ancak constructor'da bırakmak da sorun değil. Şimdilik constructor'da bırakalım.
  profileForm!: FormGroup;
  currentUser: UserResponse | null = null;
  isLoading = false;
  isEditing = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private userService: UserService,
    private snackBar: MatSnackBar
  ) {
    // effect ile currentUser signal'ındaki değişiklikleri dinle
    effect(() => {
      const user = this.authService.currentUser();
      if (user) {
        this.currentUser = user;
        this.populateForm(user);
      } else {
        this.currentUser = null;
        // Kullanıcı null ise formu sıfırla veya uygun bir işlem yap
        this.profileForm.reset();
      }
    });
  }

  ngOnInit(): void {
    this.profileForm = this.fb.group({
      firstName: [{ value: '', disabled: !this.isEditing }, Validators.required],
      lastName: [{ value: '', disabled: !this.isEditing }, Validators.required],
      username: [{ value: '', disabled: true }, Validators.required],
      email: [{ value: '', disabled: true }, [Validators.required, Validators.email]],
      // Display only, not editable via this form
      roles: [{ value: [], disabled: true }],
      isActive: [{ value: false, disabled: true }],
      subscriptionType: [{ value: '', disabled: true }],
      loyaltyPoints: [{ value: 0, disabled: true }],
      imageGenQuota: [{ value: null, disabled: true }]
    });

    // İlk yüklemede formu doldurmak için currentUser'ın mevcut değerini kullan
    const initialUser = this.authService.currentUser();
    if (initialUser) {
      this.currentUser = initialUser;
      this.populateForm(initialUser);
    }
  }

  populateForm(user: UserResponse): void {
    this.profileForm.patchValue({
      firstName: user.firstName,
      lastName: user.lastName,
      username: user.username,
      email: user.email,
      roles: user.roles?.join(', '),
      isActive: user.isActive,
      subscriptionType: user.subscriptionType || 'N/A',
      loyaltyPoints: user.loyaltyPoints || 0,
      imageGenQuota: user.imageGenQuota === null || user.imageGenQuota === undefined ? 'N/A' : user.imageGenQuota
    });
  }

  toggleEdit(): void {
    this.isEditing = !this.isEditing;
    if (this.isEditing) {
      this.profileForm.get('firstName')?.enable();
      this.profileForm.get('lastName')?.enable();
    } else {
      this.profileForm.get('firstName')?.disable();
      this.profileForm.get('lastName')?.disable();
      // Reset form to current user data if changes were made but not saved
      if (this.currentUser) {
        this.populateForm(this.currentUser);
      }
    }
  }

  onSubmit(): void {
    if (this.profileForm.invalid || !this.isEditing) {
      return;
    }

    this.isLoading = true;
    const updateRequest: UserProfileUpdateRequest = {
      firstName: this.profileForm.value.firstName,
      lastName: this.profileForm.value.lastName
    };

    this.userService.updateUserProfile(updateRequest).subscribe({
      next: (updatedUserResponse: UserResponse) => { // Dönen tip UserResponse olmalı
        this.isLoading = false;
        this.isEditing = false;
        this.profileForm.get('firstName')?.disable();
        this.profileForm.get('lastName')?.disable();
        // authService.updateCurrentUser, güncellenmiş tam UserResponse bekler.
        // Eğer updatedUserResponse sadece kısmi alanlar içeriyorsa,
        // mevcut currentUser ile birleştirip göndermek gerekebilir.
        // Şimdilik updatedUserResponse'un tam olduğunu varsayıyoruz.
        this.authService.updateCurrentUser(updatedUserResponse);
        this.snackBar.open('Profile updated successfully!', 'Close', { duration: 3000 });
      },
      error: (err) => {
        this.isLoading = false;
        this.snackBar.open(`Error updating profile: ${err.error?.message || 'Unknown error'}`, 'Close', { duration: 5000 });
      }
    });
  }
}
