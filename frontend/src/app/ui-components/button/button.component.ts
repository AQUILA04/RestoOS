import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'resto-button',
  templateUrl: './button.component.html',
  styleUrls: ['./button.component.css'],
  standalone: false
})
export class ButtonComponent {
  @Input() label: string = '';
  @Input() variant: 'primary' | 'secondary' | 'danger' = 'primary';
  @Input() disabled: boolean = false;
  @Output() pressed = new EventEmitter<Event>();

  handleClick(event: Event): void {
    if (!this.disabled) {
      this.pressed.emit(event);
    }
  }
}
