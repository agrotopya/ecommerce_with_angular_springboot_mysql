import { Component, OnInit, ViewChild, AfterViewInit, inject } from '@angular/core'; // inject eklendi
import { CommonModule } from '@angular/common'; // CommonModule eklendi
import { HttpClientModule, HttpParams } from '@angular/common/http'; // HttpClientModule eklendi
import { MatTableDataSource, MatTableModule } from '@angular/material/table'; // MatTableModule eklendi
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator'; // MatPaginatorModule eklendi
import { MatSort, MatSortModule } from '@angular/material/sort'; // MatSortModule eklendi
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar'; // MatSnackBarModule eklendi
import { MatDialog, MatDialogModule } from '@angular/material/dialog'; // MatDialogModule eklendi
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button'; // Butonlar için
import { MatTooltipModule } from '@angular/material/tooltip'; // Tooltip için

import { UserService } from '../user.service';
import { UserResponse } from '../../../shared/models/user.model'; // user.model.ts'e yönlendirildi
import { Page } from '../../../shared/models/page.model';
// Import a role editing dialog component if you create one, e.g., EditUserRolesDialogComponent

@Component({
  selector: 'app-user-list',
  standalone: true, // standalone yapıldı
  imports: [
    CommonModule,
    HttpClientModule, // HttpParams için gerekebilir, ancak genellikle ApiService içinde yönetilir. Şimdilik ekleyelim.
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    MatButtonModule,
    MatTooltipModule
  ],
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss']
})
export class UserListComponent implements OnInit, AfterViewInit {
  // userService, snackBar, dialog inject ile alınabilir. Şimdilik constructor'da bırakıyoruz.
  displayedColumns: string[] = ['id', 'username', 'email', 'roles', 'isActive', 'actions'];
  dataSource: MatTableDataSource<UserResponse> = new MatTableDataSource<UserResponse>();
  isLoading = true;
  totalElements = 0;
  availableRoles: string[] = ['ROLE_CUSTOMER', 'ROLE_SELLER', 'ROLE_ADMIN']; // As per API docs

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private userService: UserService,
    private snackBar: MatSnackBar,
    public dialog: MatDialog
  ) { }

  ngOnInit(): void {
    this.loadUsers();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;

    // If sorting and pagination are backend-driven, this needs adjustment
    // For client-side, this is fine after data is loaded.
    // For server-side, listen to paginator.page and sort.sortChange events
    this.paginator.page.subscribe(() => this.loadUsers(false));
    this.sort.sortChange.subscribe(() => {
      this.paginator.pageIndex = 0;
      this.loadUsers(false);
    });
  }

  loadUsers(showLoading: boolean = true): void {
    if (showLoading) {
      this.isLoading = true;
    }
    let params = new HttpParams()
      .set('page', this.paginator ? this.paginator.pageIndex.toString() : '0')
      .set('size', this.paginator ? this.paginator.pageSize.toString() : '10');

    if (this.sort && this.sort.active && this.sort.direction) {
      params = params.set('sort', `${this.sort.active},${this.sort.direction}`);
    }

    this.userService.getAllUsers(params).subscribe({
      next: (page: Page<UserResponse>) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.snackBar.open(`Error loading users: ${err.error?.message || 'Unknown error'}`, 'Close', { duration: 5000 });
      }
    });
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    // For client-side filtering:
    // this.dataSource.filter = filterValue.trim().toLowerCase();
    // if (this.dataSource.paginator) {
    //   this.dataSource.paginator.firstPage();
    // }

    // For server-side filtering (requires API support for 'search' param):
    this.paginator.pageIndex = 0;
    let params = new HttpParams()
      .set('page', '0')
      .set('size', this.paginator ? this.paginator.pageSize.toString() : '10')
      .set('search', filterValue.trim().toLowerCase()); // Add search param
    if (this.sort && this.sort.active && this.sort.direction) {
      params = params.set('sort', `${this.sort.active},${this.sort.direction}`);
    }
    this.isLoading = true;
    this.userService.getAllUsers(params).subscribe({
        next: (page: Page<UserResponse>) => {
            this.dataSource.data = page.content;
            this.totalElements = page.totalElements;
            this.isLoading = false;
        },
        error: (err) => {
            this.isLoading = false;
            this.snackBar.open(`Error filtering users: ${err.error?.message || 'Unknown error'}`, 'Close', { duration: 5000 });
        }
    });
  }

  toggleUserStatus(user: UserResponse): void {
    this.isLoading = true;
    this.userService.updateUserStatus(user.id, !user.isActive).subscribe({
      next: (updatedUser) => {
        const index = this.dataSource.data.findIndex(u => u.id === user.id);
        if (index > -1) {
          this.dataSource.data[index] = updatedUser;
          this.dataSource.data = [...this.dataSource.data]; // Trigger change detection
        }
        this.snackBar.open(`User ${updatedUser.username} status updated.`, 'Close', { duration: 3000 });
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.snackBar.open(`Error updating status: ${err.error?.message || 'Unknown error'}`, 'Close', { duration: 5000 });
      }
    });
  }

  // Basic role update - for more complex, use a dialog
  updateUserRoles(user: UserResponse, newRoles: string[]): void {
    if (JSON.stringify(user.roles.sort()) === JSON.stringify(newRoles.sort())) {
        this.snackBar.open('No changes in roles.', 'Close', { duration: 2000 });
        return;
    }
    this.isLoading = true;
    this.userService.updateUserRoles(user.id, newRoles).subscribe({
      next: (updatedUser) => {
        const index = this.dataSource.data.findIndex(u => u.id === user.id);
        if (index > -1) {
          this.dataSource.data[index] = updatedUser;
          this.dataSource.data = [...this.dataSource.data]; // Trigger change detection
        }
        this.snackBar.open(`User ${updatedUser.username} roles updated.`, 'Close', { duration: 3000 });
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.snackBar.open(`Error updating roles: ${err.error?.message || 'Unknown error'}`, 'Close', { duration: 5000 });
      }
    });
  }

  // Placeholder for opening a role editing dialog
  // openEditRolesDialog(user: UserResponse): void {
  //   const dialogRef = this.dialog.open(EditUserRolesDialogComponent, {
  //     width: '400px',
  //     data: { userId: user.id, currentRoles: user.roles, availableRoles: this.availableRoles }
  //   });
  //   dialogRef.afterClosed().subscribe(result => {
  //     if (result) { // result could be the new roles array
  //       this.updateUserRoles(user, result);
  //     }
  //   });
  // }
}
