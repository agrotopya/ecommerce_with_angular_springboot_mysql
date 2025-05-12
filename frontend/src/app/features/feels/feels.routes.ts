import { Routes } from '@angular/router';
import { FeelListComponent } from './components/feel-list/feel-list.component';
import { FeelDetailComponent } from './components/feel-detail/feel-detail.component';
// AuthGuard ve RoleGuard gerekirse import edilecek
// import { AuthGuard } from '@core/guards/auth.guard';
// import { RoleGuard } from '@core/guards/role.guard';
// import { Role } from '@shared/enums/role.enum';

export const FEEL_ROUTES: Routes = [
  {
    path: '',
    component: FeelListComponent,
    title: 'Discover Feels | Fibiyo'
  },
  {
    path: ':id', // veya :feelId
    component: FeelDetailComponent,
    // title: (route) => `Feel by ${route.data?.['feel']?.sellerUsername || 'User'} | Fibiyo` // Dinamik başlık için resolver gerekebilir
    // Resolve eklenebilir: feel: FeelResolver
  },
  // Satıcıya özel feel oluşturma ve yönetme rotaları seller.routes.ts içinde olacak.
  // Örnek:
  // {
  //   path: 'manage/my-feels',
  //   component: MyFeelListComponent, // Satıcının kendi feel'lerini listelediği component
  //   canActivate: [AuthGuard, RoleGuard],
  //   data: { roles: [Role.SELLER] },
  //   title: 'My Feels | Fibiyo Seller'
  // },
  // {
  //   path: 'manage/create',
  //   component: CreateFeelComponent, // Feel oluşturma formu
  //   canActivate: [AuthGuard, RoleGuard],
  //   data: { roles: [Role.SELLER] },
  //   title: 'Create New Feel | Fibiyo Seller'
  // }
];
