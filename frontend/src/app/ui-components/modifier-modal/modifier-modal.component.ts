import { Component, Input, Output, EventEmitter } from '@angular/core';

export interface ModifierOptionItem {
  id: string;
  name: string;
  priceDelta: number;
}

export interface ModifierGroupItem {
  id: string;
  name: string;
  required: boolean;
  minSelection: number;
  maxSelection: number;
  options: ModifierOptionItem[];
}

@Component({
  selector: 'resto-modifier-modal',
  templateUrl: './modifier-modal.component.html',
  styleUrls: ['./modifier-modal.component.css'],
  standalone: false
})
export class ModifierModalComponent {
  @Input() productName: string = '';
  @Input() modifierGroups: ModifierGroupItem[] = [];
  @Input() isOpen: boolean = false;

  @Output() confirm = new EventEmitter<ModifierOptionItem[]>();
  @Output() dismiss = new EventEmitter<void>();

  selectedOptions: Map<string, ModifierOptionItem> = new Map();

  toggleOption(option: ModifierOptionItem): void {
    if (this.selectedOptions.has(option.id)) {
      this.selectedOptions.delete(option.id);
    } else {
      this.selectedOptions.set(option.id, option);
    }
  }

  isOptionSelected(optionId: string): boolean {
    return this.selectedOptions.has(optionId);
  }

  get isValid(): boolean {
    for (const group of this.modifierGroups) {
      if (group.required) {
        const selectedInGroup = group.options.filter(o => this.selectedOptions.has(o.id));
        if (selectedInGroup.length < group.minSelection) {
          return false;
        }
      }
    }
    return true;
  }

  confirmSelection(): void {
    if (this.isValid) {
      this.confirm.emit(Array.from(this.selectedOptions.values()));
      this.selectedOptions.clear();
    }
  }

  closeModal(): void {
    this.selectedOptions.clear();
    this.dismiss.emit();
  }
}
