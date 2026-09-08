import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PinLockscreenComponent } from './pin-lockscreen/pin-lockscreen.component';
import { FloorPlanComponent } from './floor-plan/floor-plan.component';
import { PosLayoutComponent } from './pos-layout/pos-layout.component';
import { ModifierModalComponent } from './modifier-modal/modifier-modal.component';
import { KdsGridComponent } from './kds-grid/kds-grid.component';
import { Stock86ManagerComponent } from './stock-86-manager/stock-86-manager.component';
import { OfflineStatusComponent } from './offline-status/offline-status.component';
import { ButtonComponent } from './button/button.component';

@NgModule({
  declarations: [
    PinLockscreenComponent,
    FloorPlanComponent,
    PosLayoutComponent,
    ModifierModalComponent,
    KdsGridComponent,
    Stock86ManagerComponent,
    OfflineStatusComponent,
    ButtonComponent,
  ],
  imports: [CommonModule, FormsModule],
  exports: [
    PinLockscreenComponent,
    FloorPlanComponent,
    PosLayoutComponent,
    ModifierModalComponent,
    KdsGridComponent,
    Stock86ManagerComponent,
    OfflineStatusComponent,
    ButtonComponent,
  ],
})
export class UiComponentsModule {}
