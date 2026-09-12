import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { StoreContextService } from '../services/store-context.service';

export const RETURN_URL_KEY = 'restoos_return_url';

function rememberReturnUrl(router: Router): void {
  const url = router.getCurrentNavigation()?.extractedUrl?.toString()
    || router.url
    || '';
  const path = url.split('?')[0] || '';
  if (path && path !== '/' && !path.startsWith('/login') && !path.startsWith('/signup')) {
    sessionStorage.setItem(RETURN_URL_KEY, path.startsWith('/') ? path : `/${path}`);
  }
}

/**
 * Requires a session that identifies both the user and their tenant (organization).
 * Operational admin routes must not render for anonymous visitors.
 */
export const authGuard: CanActivateFn = () => {
  const storeContext = inject(StoreContextService);
  const router = inject(Router);

  if (storeContext.hasTenantSession()) {
    return true;
  }

  rememberReturnUrl(router);
  return router.createUrlTree(['/login']);
};

/**
 * POS/KDS may open when a full tenant session exists, or when the station is already
 * bound to an organization + store (PIN then identifies the employee).
 * Cold visitors from the landing page have neither and are sent to login.
 */
export const stationGuard: CanActivateFn = () => {
  const storeContext = inject(StoreContextService);
  const router = inject(Router);

  if (storeContext.hasTenantSession() || storeContext.hasStationBinding()) {
    return true;
  }

  rememberReturnUrl(router);
  return router.createUrlTree(['/login']);
};
