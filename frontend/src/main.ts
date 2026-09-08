// Ensure sockjs/stomp Node shims exist before any app imports evaluate.
(window as any).global = window;

async function bootstrap(): Promise<void> {
  const { platformBrowserDynamic } = await import('@angular/platform-browser-dynamic');
  const { AppModule } = await import('./app/app.module');
  await platformBrowserDynamic().bootstrapModule(AppModule);
}

bootstrap().catch((err) => console.error(err));
