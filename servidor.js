/**
 * ServidorChat — RxJS Reactivo
 * node servidor.js
 * npm install ws rxjs
 */

const http = require('http');
const fs   = require('fs');
const path = require('path');
const { WebSocketServer } = require('ws');
const { Subject, fromEvent, merge } = require('rxjs');
const { filter, map, tap, share, takeUntil } = require('rxjs/operators');

const PUERTO = 3000;

// ── Colores ───────────────────────────────────────────────────────────────
const ts  = () => new Date().toLocaleTimeString('es-CO');
const log = (msg, c = '\x1b[36m') => console.log(`${c}[${ts()}]\x1b[0m ${msg}`);

// ── HTTP ──────────────────────────────────────────────────────────────────
const httpServer = http.createServer((req, res) => {
  fs.readFile(path.join(__dirname, 'cliente.html'), (err, data) => {
    if (err) { res.writeHead(404); res.end('No se encontro cliente.html'); return; }
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(data);
  });
});

httpServer.on('error', err => {
  log(`ERROR HTTP: ${err.message}`, '\x1b[31m');
  if (err.code === 'EADDRINUSE') { log(`Puerto ${PUERTO} ocupado`, '\x1b[31m'); process.exit(1); }
});

// ── Estado ────────────────────────────────────────────────────────────────
const clientes = new Map();
let contador = 0;

const broadcast = texto => {
  const pkt = JSON.stringify({ tipo: 'mensaje', texto });
  clientes.forEach((_v, ws) => { if (ws.readyState === 1) ws.send(pkt); });
  log(`BROADCAST: ${texto}`);
};

const pushLista = () => {
  const lista = [...clientes.values()].map(c => c.nombre);
  const pkt = JSON.stringify({ tipo: 'usuarios', lista });
  clientes.forEach((_v, ws) => { if (ws.readyState === 1) ws.send(pkt); });
};

// ── WebSocket ─────────────────────────────────────────────────────────────
const wss = new WebSocketServer({ server: httpServer });

wss.on('connection', ws => {
  const id = ++contador;
  let nombre = null;
  log(`Nueva conexion id=${id}`, '\x1b[32m');

  // CLAVE: en la libreria 'ws' de Node, el evento 'message' pasa (data, isBinary)
  // fromEvent solo captura el primer argumento, hay que usar el listener directo
  const msg$ = new Subject();
  ws.on('message', (data, isBinary) => {
    const str = isBinary ? data.toString() : data.toString();
    try {
      const obj = JSON.parse(str);
      log(`MSG id=${id}: ${str}`);
      msg$.next(obj);
    } catch(e) {
      log(`JSON invalido de id=${id}: ${str}`, '\x1b[33m');
    }
  });

  const cerrar$ = new Subject();
  ws.on('close', (code, reason) => {
    log(`CLOSE id=${id} code=${code}`, '\x1b[33m');
    cerrar$.next({ code, reason });
    cerrar$.complete();
    msg$.complete();
  });
  ws.on('error', err => {
    log(`ERROR id=${id}: ${err.message}`, '\x1b[31m');
    cerrar$.next(err);
  });

  // Registro
  msg$.pipe(
    filter(d => d.tipo === 'registro'),
    takeUntil(cerrar$)
  ).subscribe(d => {
    nombre = (d.nombre || '').trim() || `Cliente-${id}`;
    clientes.set(ws, { nombre, id });
    log(`REGISTRADO: ${nombre}`, '\x1b[32m');
    broadcast(`✔ ${nombre} se unio al chat.`);
    pushLista();
    ws.send(JSON.stringify({ tipo: 'bienvenida', nombre }));
    log(`Bienvenida enviada a ${nombre}`, '\x1b[32m');
  });

  // Chat
  msg$.pipe(
    filter(d => d.tipo === 'chat'),
    takeUntil(cerrar$)
  ).subscribe(d => {
    if (!nombre) return;
    const texto = (d.texto || '').trim();
    if (!texto) return;
    if (texto === '/salir') { ws.close(); return; }
    broadcast(`[${nombre}]: ${texto}`);
  });

  // Desconexion
  cerrar$.subscribe(() => {
    if (!clientes.has(ws)) return;
    clientes.delete(ws);
    if (nombre) {
      broadcast(`⚠ ${nombre} ha salido del chat.`);
      pushLista();
    }
  });
});

// ── Arrancar ──────────────────────────────────────────────────────────────
httpServer.listen(PUERTO, () => {
  log(`Servidor HTTP  → http://localhost:${PUERTO}`, '\x1b[32m');
  log(`WebSocket      → ws://localhost:${PUERTO}`, '\x1b[32m');
  log(`Abre http://localhost:${PUERTO} en varias pestanas para chatear`, '\x1b[36m');
});

process.on('uncaughtException', err => log(`uncaughtException: ${err.message}`, '\x1b[31m'));
