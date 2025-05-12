import {
  AngularNodeAppEngine,
  createNodeRequestHandler,
  isMainModule,
  writeResponseToNodeResponse,
} from '@angular/ssr/node';
import express from 'express';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const serverDistFolder = dirname(fileURLToPath(import.meta.url));
const browserDistFolder = resolve(serverDistFolder, '../browser');

const app = express();
const angularApp = new AngularNodeAppEngine();

/**
 * Example Express Rest API endpoints can be defined here.
 * Uncomment and define endpoints as necessary.
 *
 * Example:
 * ```ts
 * app.get('/api/**', (req, res) => {
 *   // Handle API request
 * });
 * ```
 */

/**
 * Serve static files from /browser
 */
app.use(
  express.static(browserDistFolder, {
    maxAge: '1y',
    index: false,
    redirect: false,
  }),
);

// Content Security Policy Middleware
app.use((req, res, next) => {
  res.setHeader(
    'Content-Security-Policy',
    "default-src 'self'; " +
    "script-src 'self' https://js.stripe.com https://m.stripe.network https://b.stripecdn.com 'unsafe-inline' 'unsafe-eval'; " + // b.stripecdn.com eklendi
    "style-src 'self' https://fonts.googleapis.com 'unsafe-inline'; " +
    "img-src 'self' http://localhost:8080 data: https: *.stripe.com; " + // http://localhost:8080 ve https: eklendi, *.stripe.com zaten vardı
    "font-src 'self' https://fonts.gstatic.com data:; " + // data: eklendi
    "connect-src 'self' http://localhost:8080 https://api.stripe.com https://q.stripe.com https://b.stripecdn.com *.stripe.com; " + // b.stripecdn.com ve *.stripe.com eklendi
    "frame-src 'self' https://js.stripe.com https://b.stripecdn.com https://hooks.stripe.com https://m.stripe.network; " + // b.stripecdn.com ve m.stripe.network eklendi
    "worker-src 'self' blob: https://js.stripe.com https://b.stripecdn.com https://m.stripe.network;" // b.stripecdn.com ve m.stripe.network eklendi
  );
  next();
});

/**
 * Handle all other requests by rendering the Angular application.
 */
app.use('/**', (req, res, next) => {
  angularApp
    .handle(req)
    .then((response) =>
      response ? writeResponseToNodeResponse(response, res) : next(),
    )
    .catch(next);
});

/**
 * Start the server if this module is the main entry point.
 * The server listens on the port defined by the `PORT` environment variable, or defaults to 4000.
 */
if (isMainModule(import.meta.url)) {
  const port = process.env['PORT'] || 4000;
  app.listen(port, () => {
    console.log(`Node Express server listening on http://localhost:${port}`);
  });
}

/**
 * Request handler used by the Angular CLI (for dev-server and during build) or Firebase Cloud Functions.
 */
export const reqHandler = createNodeRequestHandler(app);
