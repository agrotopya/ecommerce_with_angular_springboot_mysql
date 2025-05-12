import { Routes } from '@angular/router';

export const REVIEW_ROUTES: Routes = [
  // ProductReviewFormComponent ve ProductReviewListComponent genellikle
  // ProductDetailComponent içine gömülür ve kendi rotalarına sahip olmazlar.
  // Eğer ayrı bir "tüm yorumları gör" sayfası veya "yorum yaz" sayfası olacaksa,
  // buraya eklenebilir.
  //
  // Örnek:
  // {
  //   path: 'product/:productId', // /reviews/product/:productId
  //   component: ProductReviewListComponent
  // },
  // {
  //   path: 'product/:productId/new', // /reviews/product/:productId/new
  //   component: ProductReviewFormComponent,
  //   canActivate: [AuthGuard]
  // }
];
