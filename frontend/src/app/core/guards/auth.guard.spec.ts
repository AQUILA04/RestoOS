import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { authGuard, stationGuard } from './auth.guard';
import { StoreContextService } from '../services/store-context.service';

describe('route guards', () => {
  let storeContext: jasmine.SpyObj<StoreContextService>;
  let router: Router;

  beforeEach(() => {
    storeContext = jasmine.createSpyObj('StoreContextService', [
      'hasTenantSession',
      'hasStationBinding',
    ]);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'login', children: [] }]),
        { provide: StoreContextService, useValue: storeContext },
      ],
    });
    router = TestBed.inject(Router);
  });

  describe('authGuard', () => {
    it('allows activation when user and tenant session exist', () => {
      storeContext.hasTenantSession.and.returnValue(true);
      const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
      expect(result).toBe(true);
    });

    it('redirects to /login when session is missing', () => {
      storeContext.hasTenantSession.and.returnValue(false);
      const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
      expect(result).toEqual(router.createUrlTree(['/login']));
    });
  });

  describe('stationGuard', () => {
    it('allows activation with a full tenant session', () => {
      storeContext.hasTenantSession.and.returnValue(true);
      storeContext.hasStationBinding.and.returnValue(false);
      const result = TestBed.runInInjectionContext(() => stationGuard({} as never, {} as never));
      expect(result).toBe(true);
    });

    it('allows activation when the station is org/store bound', () => {
      storeContext.hasTenantSession.and.returnValue(false);
      storeContext.hasStationBinding.and.returnValue(true);
      const result = TestBed.runInInjectionContext(() => stationGuard({} as never, {} as never));
      expect(result).toBe(true);
    });

    it('redirects cold visitors to /login', () => {
      storeContext.hasTenantSession.and.returnValue(false);
      storeContext.hasStationBinding.and.returnValue(false);
      const result = TestBed.runInInjectionContext(() => stationGuard({} as never, {} as never));
      expect(result).toEqual(router.createUrlTree(['/login']));
    });
  });
});
