import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';

@Component({
  selector: 'resto-pin-lockscreen',
  templateUrl: './pin-lockscreen.component.html',
  styleUrls: ['./pin-lockscreen.component.css'],
  standalone: false
})
export class PinLockscreenComponent implements OnChanges {
  @Input() users: Array<{ id: string; name: string; avatarUrl?: string }> = [];
  @Output() loginSuccess = new EventEmitter<{ userId: string; pin: string }>();

  selectedUserId: string | null = null;
  enteredPin = '';

  get pinMask(): string {
    return '•'.repeat(Math.min(this.enteredPin.length, 4));
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['users'] && this.users?.length && !this.selectedUserId) {
      this.selectedUserId = this.users[0].id;
    }
  }

  selectUser(userId: string): void {
    this.selectedUserId = userId;
    this.enteredPin = '';
  }

  pressDigit(digit: string): void {
    if (this.enteredPin.length >= 4) {
      return;
    }
    if (!this.selectedUserId && this.users.length) {
      this.selectedUserId = this.users[0].id;
    }
    this.enteredPin += digit;
    if (this.enteredPin.length === 4) {
      this.submitPin();
    }
  }

  clearPin(): void {
    this.enteredPin = '';
  }

  submitPin(): void {
    const userId = this.selectedUserId || this.users[0]?.id || localStorage.getItem('user_id');
    if (userId && this.enteredPin.length === 4) {
      this.loginSuccess.emit({
        userId,
        pin: this.enteredPin
      });
    }
  }
}
