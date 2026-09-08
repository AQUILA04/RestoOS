import { randomUUID } from 'crypto';
import { test, expect } from '../fixtures/test-fixtures';
import {
  authHeaders,
  bootstrapTenant,
  createTakeawayOrder,
  patchKitchenStatus,
} from '../helpers/api-bootstrap';

/**
 * Companion: offline queue (IndexedDB / onLine stub) + kitchen snapshot recovery
 * (practical stand-in for SockJS reconnect — GET kitchen orders after READY).
 */
test.describe('Companion — offline queue & kitchen snapshot recovery', () => {
  const runId = Date.now();

  test('IndexedDB offline queue + navigator.onLine stub; kitchen GET recovers READY', async ({
    request,
    page,
    backendApiUrl,
    posPage,
  }) => {
    const tenant = await bootstrapTenant(request, backendApiUrl, {
      runId,
      orgPrefix: 'Offline Org',
      productName: `Wrap ${runId}`,
      productPrice: 9.5,
    });

    await test.step('Offline queue: stub offline, write IndexedDB, assert pending', async () => {
      await posPage.page.goto('/pos');
      await posPage.page.evaluate(
        ([token, org, store]) => {
          localStorage.setItem('access_token', token as string);
          localStorage.setItem('organization_id', org as string);
          localStorage.setItem('store_id', store as string);
        },
        [tenant.ownerToken, tenant.orgId, tenant.storeId]
      );

      const queueSnapshot = await page.evaluate(
        async ({ productId, storeId, idempotencyKey }) => {
          Object.defineProperty(navigator, 'onLine', {
            configurable: true,
            get: () => false,
          });
          window.dispatchEvent(new Event('offline'));

          const openDb = (): Promise<IDBDatabase> =>
            new Promise((resolve, reject) => {
              const req = indexedDB.open('restoos-offline', 1);
              req.onupgradeneeded = () => {
                const db = req.result;
                if (!db.objectStoreNames.contains('orders')) {
                  db.createObjectStore('orders', { keyPath: 'tempUuid' });
                }
              };
              req.onsuccess = () => resolve(req.result);
              req.onerror = () => reject(req.error);
            });

          const db = await openDb();
          const pending = {
            tempUuid: crypto.randomUUID(),
            payload: {
              storeId,
              orderType: 'TAKEAWAY',
              sendToKitchen: true,
              idempotencyKey,
              items: [{ productId, quantity: 1 }],
            },
            createdAt: new Date().toISOString(),
            status: 'pending' as const,
          };

          await new Promise<void>((resolve, reject) => {
            const tx = db.transaction('orders', 'readwrite');
            tx.objectStore('orders').put(pending);
            tx.oncomplete = () => resolve();
            tx.onerror = () => reject(tx.error);
          });

          const all = await new Promise<unknown[]>((resolve, reject) => {
            const tx = db.transaction('orders', 'readonly');
            const req = tx.objectStore('orders').getAll();
            req.onsuccess = () => resolve(req.result as unknown[]);
            req.onerror = () => reject(req.error);
          });

          db.close();
          return {
            online: navigator.onLine,
            queueLength: all.length,
            statuses: (all as Array<{ status: string }>).map((x) => x.status),
            idempotencyKey,
          };
        },
        {
          productId: tenant.productId,
          storeId: tenant.storeId,
          idempotencyKey: randomUUID(),
        }
      );

      expect(queueSnapshot.online).toBe(false);
      expect(queueSnapshot.queueLength).toBeGreaterThanOrEqual(1);
      expect(queueSnapshot.statuses).toContain('pending');

      // Conceptual drain path: when back online, POST /orders with same Idempotency-Key
      await page.evaluate(() => {
        Object.defineProperty(navigator, 'onLine', {
          configurable: true,
          get: () => true,
        });
        window.dispatchEvent(new Event('online'));
      });

      const drainRes = await request.post(`${backendApiUrl}/api/v1/orders`, {
        headers: {
          ...authHeaders(tenant.ownerToken),
          'Idempotency-Key': queueSnapshot.idempotencyKey,
        },
        data: {
          storeId: tenant.storeId,
          orderType: 'TAKEAWAY',
          sendToKitchen: true,
          items: [{ productId: tenant.productId, quantity: 1 }],
        },
      });
      expect(drainRes.status()).toBe(200);
      const drained = (await drainRes.json()).data;
      expect(drained.status).toBe('SENT_TO_KITCHEN');
      expect(drained.orderNumber).toBeGreaterThan(100);
      expect(drained.orderNumber).not.toBe(1001);

      // Clear IndexedDB after successful sync (mirrors OfflineQueueService.drain delete)
      await page.evaluate(async () => {
        const db = await new Promise<IDBDatabase>((resolve, reject) => {
          const req = indexedDB.open('restoos-offline', 1);
          req.onsuccess = () => resolve(req.result);
          req.onerror = () => reject(req.error);
        });
        await new Promise<void>((resolve, reject) => {
          const tx = db.transaction('orders', 'readwrite');
          tx.objectStore('orders').clear();
          tx.oncomplete = () => resolve();
          tx.onerror = () => reject(tx.error);
        });
        db.close();
      });
    });

    await test.step('Kitchen snapshot recovery after READY (API stand-in for WS reconnect)', async () => {
      const order = await createTakeawayOrder(request, backendApiUrl, tenant.ownerToken, {
        storeId: tenant.storeId,
        productId: tenant.productId,
        sendToKitchen: true,
      });
      expect(order.status).toBe('SENT_TO_KITCHEN');

      await patchKitchenStatus(request, backendApiUrl, tenant.ownerToken, order.id, 'PREPARING');
      await patchKitchenStatus(request, backendApiUrl, tenant.ownerToken, order.id, 'READY');

      // Simulate client reconnect: re-fetch kitchen queue instead of SockJS
      const queueRes = await request.get(
        `${backendApiUrl}/api/v1/kitchen/orders?storeId=${tenant.storeId}`,
        { headers: authHeaders(tenant.ownerToken) }
      );
      expect(queueRes.status()).toBe(200);
      const queue = (await queueRes.json()).data as Array<{
        id: string;
        status: string;
        orderNumber: number;
      }>;
      const recovered = queue.find((o) => o.id === order.id);
      expect(recovered).toBeTruthy();
      expect(recovered!.status).toBe('READY');
      expect(recovered!.orderNumber).toBe(order.orderNumber);
      expect(recovered!.orderNumber).not.toBe(1001);
    });
  });
});
