import { Component, Input, Output, EventEmitter } from '@angular/core';

export interface CartItem {
  productId: string;
  name: string;
  unitPrice: number;
  quantity: number;
  modifiers: string[];
}

@Component({
  selector: 'resto-pos-layout',
  templateUrl: './pos-layout.component.html',
  styleUrls: ['./pos-layout.component.css'],
  standalone: false
})
export class PosLayoutComponent {
  @Input() categories: Array<{ id: string; name: string }> = [];
  @Input() products: Array<{ id: string; name: string; price: number; is86: boolean }> = [];
  @Input() cartItems: CartItem[] = [];
  @Input() activeCategoryId: string | null = null;

  @Output() onCategorySelect = new EventEmitter<string>();
  @Output() onProductSelect = new EventEmitter<any>();
  @Output() onCheckout = new EventEmitter<void>();

  selectCategory(id: string): void {
    this.activeCategoryId = id;
    this.onCategorySelect.emit(id);
  }

  selectProduct(product: any): void {
    if (!product.is86) {
      this.onProductSelect.emit(product);
    }
  }

  get cartTotal(): number {
    return this.cartItems.reduce((sum, item) => sum + (item.unitPrice * item.quantity), 0);
  }
}
