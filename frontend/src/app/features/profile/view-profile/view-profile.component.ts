// src/app/features/profile/view-profile/view-profile.component.ts
import { Component, inject, OnInit, WritableSignal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { UserResponse } from '../../../shared/models/user.model';
import { RouterLink } from '@angular/router'; // Linkler için

@Component({
  selector: 'app-view-profile',
  standalone: true,
  imports: [CommonModule, RouterLink], // Added RouterLink back if it was intended
  templateUrl: './view-profile.component.html',
  styleUrls: ['./view-profile.component.scss'],
})
export class ViewProfileComponent implements OnInit {
  authService = inject(AuthService);
  // Sinyal olarak direkt authService'ten alalım
  user: WritableSignal<UserResponse | null> = this.authService.currentUser;

  ngOnInit(): void {
    // APP_INITIALIZER should ensure that by the time this component initializes (on the client),
    // authService.initAuthState() has already been called and currentUser signal is populated.
    if (!this.user()) {
      console.warn(
        'ViewProfileComponent: User data not found in AuthService on init. This might indicate an issue with auth state loading or the user is not logged in.'
      );
      // If initAuthState is idempotent and safe to call multiple times, this could be a fallback.
      // However, if APP_INITIALIZER is working, this should not be necessary.
      // If a user navigates here without being authenticated, a route guard should prevent access.
      // this.authService.initAuthState(); // Consider if this is truly needed or if guards are better.
    } else {
      console.log('ViewProfileComponent: User data successfully retrieved from AuthService signal:', this.user());
    }
  }
}
