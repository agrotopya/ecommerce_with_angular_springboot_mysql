// src/app/features/admin/user-management/user-management.component.ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminService } from '../admin.service';
import { User } from '../../../shared/models/user.model'; // UserResponse -> User
import { Role } from '../../../shared/enums/role.enum';
import { NotificationService } from '../../../core/services/notification.service'; // Eklendi
// import { PaginatorComponent } from '../../../shared/components/paginator/paginator.component'; // İleride eklenecek
import { FormsModule } from '@angular/forms'; // Filtreleme için Eklendi
import { Page } from '../../../shared/models/page.model'; // Page importu eklendi
import { HttpErrorResponse } from '@angular/common/http'; // Eklendi

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule], // FormsModule eklendi
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss']
})
export class UserManagementComponent implements OnInit {
  private adminService = inject(AdminService);
  private notificationService = inject(NotificationService); // Aktif edildi

  usersPage = signal<Page<User> | null>(null); // users -> usersPage, Page<User> tipi
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);
  roles = Object.values(Role);
  public Role = Role;

  // Sayfalama ve filtreleme
  currentPage = signal(0);
  pageSize = signal(10); // Varsayılan sayfa boyutu
  // totalElements = signal(0); // Artık Page nesnesinden gelecek
  searchTerm = signal('');
  selectedRole = signal<Role | undefined>(undefined);
  selectedStatus = signal<boolean | undefined>(undefined); // true, false, undefined (all)
  currentSort = signal('id,asc');

  // Template'de [(ngModel)] için
  filterSearchTerm: string = '';
  filterSelectedRole: string = ''; // '' for all roles
  filterSelectedStatus: string = ''; // '' for all, 'true' for active, 'false' for inactive

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const role = this.filterSelectedRole ? (this.filterSelectedRole as Role) : undefined;
    let status: boolean | undefined;
    if (this.filterSelectedStatus === 'true') status = true;
    if (this.filterSelectedStatus === 'false') status = false;

    this.adminService.getUsers(
      this.currentPage(),
      this.pageSize(),
      this.currentSort(),
      this.searchTerm() || undefined, // Boş string ise undefined gönder
      role,
      status
    ).subscribe({
      next: (response: Page<User>) => { // Tip eklendi
        this.usersPage.set(response); // users -> usersPage
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => { // Tip eklendi
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load users.');
        this.notificationService.showError(err.error?.message || 'Failed to load users.');
        console.error('Error loading users:', err);
      }
    });
  }

  updateUserStatus(userId: string | number, isActive: boolean): void {
    this.adminService.updateUserStatus(userId, isActive).subscribe({
      next: () => {
        this.notificationService.showSuccess(`User status updated successfully.`);
        this.loadUsers(); // Listeyi yenile
      },
      error: (err: HttpErrorResponse) => {
        const errorMsg = err.error?.message || 'Failed to update user status.';
        this.errorMessage.set(errorMsg);
        this.notificationService.showError(errorMsg);
        console.error('Error updating user status:', err);
      }
    });
  }

  trackByUser(index: number, user: User): number {
    return user.id;
  }

  updateUserRole(userId: string | number, newRole: Role): void { // event parametresi newRole: Role olarak değiştirildi
    // newRole zaten Role tipinde olduğu için ek bir cast veya kontrol (Object.values) burada gereksiz.
    // Ancak, adminin kendi rolünü değiştirmesini veya son adminin rolünü değiştirmesini engellemek gibi
    // iş mantığı kontrolleri burada veya serviste eklenebilir.
    // Örneğin:
    // const currentUser = this.authService.currentUser(); // AuthService inject edilmeli
    // if (currentUser && currentUser.id === userId && currentUser.roles.includes(Role.ADMIN) && newRole !== Role.ADMIN) {
    //   this.notificationService.showError('You cannot remove the ADMIN role from yourself.');
    //   return;
    // }

    this.adminService.updateUserRole(userId, newRole).subscribe({
      next: () => {
          this.notificationService.showSuccess(`User role updated successfully.`);
          this.loadUsers(); // Listeyi yenile
        },
        error: (err: HttpErrorResponse) => {
          const errorMsg = err.error?.message || 'Failed to update user role.';
          this.errorMessage.set(errorMsg);
          this.notificationService.showError(errorMsg);
          console.error('Error updating user role:', err);
        }
      });
    // } else { // Bu else bloğu newRole kontrolü için gereksiz, çünkü newRole her zaman Role tipinde olacak.
    //   this.notificationService.showError('Invalid role selected.');
    // } // Bu else bloğu kaldırıldı. newRole her zaman Role tipinde olacak.
  } // Eksik kapanış parantezi buraya eklendi (aslında bir önceki else bloğu kaldırılınca sorun çözülüyor)

  onPageChange(page: number): void {
    if (page >= 0 && page < (this.usersPage()?.totalPages ?? 0)) {
      this.currentPage.set(page);
      this.loadUsers();
    }
  }

  applyFilters(): void {
    this.currentPage.set(0); // Filtre uygulandığında ilk sayfaya dön
    this.searchTerm.set(this.filterSearchTerm);
    // selectedRole ve selectedStatus zaten [(ngModel)] ile güncelleniyor,
    // loadUsers içinde filterSelectedRole ve filterSelectedStatus kullanılacak.
    this.loadUsers();
  }

  clearFilters(): void {
    this.filterSearchTerm = '';
    this.filterSelectedRole = '';
    this.filterSelectedStatus = '';
    this.searchTerm.set('');
    this.selectedRole.set(undefined);
    this.selectedStatus.set(undefined);
    this.currentPage.set(0);
    this.loadUsers();
  }

  get pageNumbers(): number[] {
    const totalPages = this.usersPage()?.totalPages ?? 0;
    return Array(totalPages).fill(0).map((x, i) => i);
  }

  // Kullanıcının mevcut ana rolünü belirlemek için yardımcı metot
  getUserDisplayRole(user: User): Role {
    if (!user || !user.roles || !Array.isArray(user.roles) || user.roles.length === 0) {
      return Role.CUSTOMER; // Varsayılan rol
    }

    // Backend'den gelen roller "ROLE_ADMIN", "ROLE_SELLER", "ROLE_CUSTOMER" formatında
    if (user.roles.some((roleStr: string) => roleStr === 'ROLE_ADMIN')) {
      return Role.ADMIN;
    }
    if (user.roles.some((roleStr: string) => roleStr === 'ROLE_SELLER')) {
      return Role.SELLER;
    }
    // Diğer tüm durumlar için veya sadece ROLE_CUSTOMER varsa
    return Role.CUSTOMER;
  }
}
