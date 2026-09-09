export {};

declare global {
  interface Window {
    global: typeof globalThis;
  }
}

// sockjs-client expects Node-style global
(window as any).global = window;
