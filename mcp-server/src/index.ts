import { createServer } from 'http';
import { WsHandler } from './websocket-handler.js';

const PORT = parseInt(process.env.PORT || '8765', 10);
const HOST = process.env.HOST || '0.0.0.0';

console.log(`
╔═══════════════════════════════════════════╗
║        OpenCode MCP Server v1.0          ║
╚═══════════════════════════════════════════╝
`);

// Health check HTTP server
const httpServer = createServer((req, res) => {
  if (req.url === '/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      status: 'ok',
      version: '1.0.0',
      uptime: process.uptime(),
      ws_endpoint: `ws://${HOST}:${PORT}/ws`,
    }));
    return;
  }
  res.writeHead(404);
  res.end('Not Found');
});

httpServer.listen(PORT, HOST, () => {
  console.log(`[HTTP] Health check: http://${HOST}:${PORT}/health`);
});

// WebSocket handler
const wsHandler = new WsHandler(PORT + 1);

console.log(`
┌─────────────────────────────────────────────┐
│ Android приложение → MCP Server → OpenCode  │
├─────────────────────────────────────────────┤
│ WebSocket: ws://${HOST}:${PORT + 1}/ws         │
│ Health:    http://${HOST}:${PORT}/health        │
└─────────────────────────────────────────────┘
`);

// Graceful shutdown
process.on('SIGINT', () => {
  console.log('\n[Server] Остановка...');
  wsHandler.close();
  httpServer.close();
  process.exit(0);
});

process.on('SIGTERM', () => {
  console.log('\n[Server] Остановка...');
  wsHandler.close();
  httpServer.close();
  process.exit(0);
});