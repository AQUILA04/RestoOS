import { Component, Input, Output, EventEmitter } from '@angular/core';

export interface TableNode {
  id: string;
  tableNumber: string;
  capacity: number;
  status: 'AVAILABLE' | 'OCCUPIED' | 'RESERVED' | 'OUT_OF_SERVICE';
  zoneId: string;
  posX?: number;
  posY?: number;
}

export interface ZoneTab {
  id: string;
  name: string;
}

@Component({
  selector: 'resto-floor-plan',
  templateUrl: './floor-plan.component.html',
  styleUrls: ['./floor-plan.component.css'],
  standalone: false
})
export class FloorPlanComponent {
  @Input() zones: ZoneTab[] = [];
  @Input() tables: TableNode[] = [];
  @Input() activeZoneId: string | null = null;
  @Output() onTableSelect = new EventEmitter<TableNode>();
  @Output() onZoneSelect = new EventEmitter<string>();

  selectZone(zoneId: string): void {
    this.activeZoneId = zoneId;
    this.onZoneSelect.emit(zoneId);
  }

  selectTable(table: TableNode): void {
    this.onTableSelect.emit(table);
  }

  get filteredTables(): TableNode[] {
    if (!this.activeZoneId) return this.tables;
    return this.tables.filter(t => t.zoneId === this.activeZoneId);
  }
}
