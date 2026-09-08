import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'resto-pin-lockscreen',
  templateUrl: './pin-lockscreen.component.html',
  styleUrls: ['./pin-lockscreen.component.css'],
  standalone: false
})
export class PinLockscreenComponent {
  @Input() users: Array<{ id: string; name: string; avatarUrl?: string }> = [];
  @Output() onLoginSuccess = new EventEmitter<{ userId: string; pin: string }>();

  selectedUserId: string | null = null;
  enteredPin: string = '';

  selectUser(userId: string): void {
    this.selectedUserId = userId;
    this.enteredPin = '';
  }

  pressDigit(digit: string): void {
    if (!this.selectedUserId || this.enteredPin.length >= 4) return;
    this.enteredPin += digit;

    if (this.enteredPin.length === 4) {
      this.submitPin();
    }
  }

  clearPin(): void {
    this.enteredPin = '';
  }

  submitPin(): void {
    if (this.selectedUserId && this.enteredPin.length === 4) {
      this.onLoginSuccess.emit({
        userId: this.selectedUserId,
        pin: this.enteredPin
      });
    }
  }
}
