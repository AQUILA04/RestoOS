import { TestBed } from '@angular/core/testing';
import { StoreContextService } from './store-context.service';

describe('StoreContextService session predicates', () => {
  let service: StoreContextService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(StoreContextService);
  });

  afterEach(() => localStorage.clear());

  it('hasTenantSession requires token, user, and organization', () => {
    expect(service.hasTenantSession()).toBeFalse();
    localStorage.setItem('access_token', 'tok');
    localStorage.setItem('user_id', 'u1');
    expect(service.hasTenantSession()).toBeFalse();
    localStorage.setItem('organization_id', 'org1');
    expect(service.hasTenantSession()).toBeTrue();
  });

  it('hasTenantSession rejects whitespace-only ids', () => {
    localStorage.setItem('access_token', 'tok');
    localStorage.setItem('user_id', '  ');
    localStorage.setItem('organization_id', 'org1');
    expect(service.hasTenantSession()).toBeFalse();
  });

  it('hasStationBinding requires organization and store', () => {
    expect(service.hasStationBinding()).toBeFalse();
    localStorage.setItem('organization_id', 'org1');
    expect(service.hasStationBinding()).toBeFalse();
    localStorage.setItem('store_id', 'store1');
    expect(service.hasStationBinding()).toBeTrue();
  });

  it('clearAuth keeps station binding while clear removes it', () => {
    service.setSession({
      accessToken: 'tok',
      userId: 'u1',
      organizationId: 'org1',
      storeId: 'store1',
      roles: ['OWNER'],
    });
    localStorage.setItem('pos_staff', '[]');
    service.clearAuth();
    expect(localStorage.getItem('access_token')).toBeNull();
    expect(service.hasTenantSession()).toBeFalse();
    expect(service.hasStationBinding()).toBeTrue();
    service.clear();
    expect(service.hasStationBinding()).toBeFalse();
    expect(localStorage.getItem('pos_staff')).toBeNull();
  });
});
