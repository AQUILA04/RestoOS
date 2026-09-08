import { Component, Input } from '@angular/core';

@Component({
  selector: 'resto-offline-status',
  templateUrl: './offline-status.component.html',
  styleUrls: ['./offline-status.component.css'],
  standalone: false
})
export class OfflineStatusComponent {
  @Input() isOnline: boolean = true;
  @Input() pendingCount: number = 0;
}
