import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config'; // appConfig'i app.config.ts dosyasından import et

bootstrapApplication(AppComponent, appConfig) // appConfig'i bootstrapApplication fonksiyonuna ikinci parametre olarak ver
  .catch(err => console.error(err));
