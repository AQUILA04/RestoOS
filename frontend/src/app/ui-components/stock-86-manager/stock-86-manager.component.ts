import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'resto-stock-86-manager',
  templateUrl: './stock-86-manager.component.html',
  styleUrls: ['./stock-86-manager.component.css'],
  standalone: false
})
export class Stock86ManagerComponent {
  @Input() products: Array<{ id: string; name: string; is86: boolean; imageUrl?: string }> = [];
  @Output() toggle86 = new EventEmitter<{ productId: string; is86: boolean }>();

  toggle(product: { id: string; name: string; is86: boolean }): void {
    const newStatus = !product.is86;
    product.is86 = newStatus;
    this.toggle86.emit({ productId: product.id, is86: newStatus });
  }
}
