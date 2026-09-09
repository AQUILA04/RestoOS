#!/usr/bin/env node
/**
 * Static SPA server with same-origin /api and /ws reverse-proxy.
 * Used by CI e2e so the browser matches Contabo Traefik routing
 * (apiUrl: '' → relative /api on the SPA origin).
 */
import http from 'node:http';
import https from 'node:https';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { URL } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(
  process.env.SPA_ROOT || path.join(__dirname, '..', 'dist', 'restoos-shell', 'browser')
);
const port = Number(process.env.PORT || 4200);
const apiTarget = process.env.API_URL || 'http://localhost:8080';
const targetUrl = new URL(apiTarget);
const proxyClient = targetUrl.protocol === 'https:' ? https : http;

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.map': 'application/json',
};

function shouldProxy(urlPath) {
  return urlPath.startsWith('/api') || urlPath.startsWith('/ws');
}

function proxyRequest(clientReq, clientRes) {
  const headers = { ...clientReq.headers, host: targetUrl.host };
  const opts = {
    protocol: targetUrl.protocol,
    hostname: targetUrl.hostname,
    port: targetUrl.port || (targetUrl.protocol === 'https:' ? 443 : 80),
    path: clientReq.url,
    method: clientReq.method,
    headers,
  };

  const upstream = proxyClient.request(opts, (upRes) => {
    clientRes.writeHead(upRes.statusCode || 502, upRes.headers);
    upRes.pipe(clientRes);
  });
  upstream.on('error', (err) => {
    if (!clientRes.headersSent) {
      clientRes.writeHead(502, { 'content-type': 'text/plain' });
    }
    clientRes.end(`proxy error: ${err.message}`);
  });
  clientReq.pipe(upstream);
}

function proxyUpgrade(req, socket, head) {
  const opts = {
    protocol: targetUrl.protocol,
    hostname: targetUrl.hostname,
    port: targetUrl.port || (targetUrl.protocol === 'https:' ? 443 : 80),
    path: req.url,
    method: 'GET',
    headers: { ...req.headers, host: targetUrl.host },
  };
  const upstream = proxyClient.request(opts);
  upstream.on('upgrade', (upRes, upSocket, upHead) => {
    socket.write(
      `HTTP/1.1 101 Switching Protocols\r\n` +
        Object.entries(upRes.headers)
          .map(([k, v]) => `${k}: ${v}`)
          .join('\r\n') +
        `\r\n\r\n`
    );
    if (upHead?.length) socket.write(upHead);
    upSocket.pipe(socket);
    socket.pipe(upSocket);
  });
  upstream.on('error', () => socket.destroy());
  upstream.end();
  if (head?.length) upstream.write(head);
}

function sendFile(res, filePath) {
  const ext = path.extname(filePath).toLowerCase();
  res.writeHead(200, { 'content-type': MIME[ext] || 'application/octet-stream' });
  fs.createReadStream(filePath).pipe(res);
}

function serveStatic(req, res) {
  const urlPath = decodeURIComponent((req.url || '/').split('?')[0]);
  const safe = path.normalize(urlPath).replace(/^(\.\.[/\\])+/, '');
  let filePath = path.join(root, safe);

  if (fs.existsSync(filePath) && fs.statSync(filePath).isDirectory()) {
    filePath = path.join(filePath, 'index.html');
  }

  if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
    sendFile(res, filePath);
    return;
  }

  const index = path.join(root, 'index.html');
  if (fs.existsSync(index)) {
    sendFile(res, index);
    return;
  }

  res.writeHead(404, { 'content-type': 'text/plain' });
  res.end('Not found');
}

if (!fs.existsSync(root)) {
  console.error(`SPA root not found: ${root}`);
  process.exit(1);
}

const server = http.createServer((req, res) => {
  const urlPath = (req.url || '/').split('?')[0];
  if (shouldProxy(urlPath)) {
    proxyRequest(req, res);
    return;
  }
  serveStatic(req, res);
});

server.on('upgrade', (req, socket, head) => {
  const urlPath = (req.url || '/').split('?')[0];
  if (shouldProxy(urlPath)) {
    proxyUpgrade(req, socket, head);
    return;
  }
  socket.destroy();
});

server.listen(port, '0.0.0.0', () => {
  console.log(`SPA+proxy listening on http://0.0.0.0:${port} → ${apiTarget} (root=${root})`);
});
