import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { AppComponent } from './app.component';
import { APP_ROUTES } from './app.routes';
import { UiComponentsModule } from './ui-components/ui-components.module';
import { JwtInterceptor } from './core/interceptors/jwt.interceptor';
import { PosPageComponent } from './pages/pos/pos-page.component';
import { PosOrderPageComponent } from './pages/pos/pos-order-page.component';
import { KdsPageComponent } from './pages/kds/kds-page.component';
import { AdminCatalogPageComponent } from './pages/admin/admin-catalog-page.component';
import { AdminDashboardPageComponent } from './pages/admin/admin-dashboard-page.component';

@NgModule({
  declarations: [
    AppComponent,
    PosPageComponent,
    PosOrderPageComponent,
    KdsPageComponent,
    AdminCatalogPageComponent,
    AdminDashboardPageComponent,
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    FormsModule,
    RouterModule.forRoot(APP_ROUTES),
    UiComponentsModule,
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true },
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
