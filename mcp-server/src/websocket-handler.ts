import { WebSocketServer, WebSocket } from 'ws';
import { IncomingMessage } from 'http';
import { OpencodeBridge } from './opencode-bridge.js';
import { v4 as uuidv4 } from 'uuid';

interface ClientSession {
  id: string;
  ws: WebSocket;
  authToken: string | null;
  connected: boolean;
  projectPath: string;
  bridge: OpencodeBridge;
  currentSessionId: string | null;
}

export class WsHandler {
  private wss: WebSocketServer;
  private sessions = new Map<string, ClientSession>();
  private port: number;

  constructor(port: number) {
    this.port = port;
    this.wss = new WebSocketServer({ port, path: '/ws' });
    this.setup();
  }

  private setup(): void {
    this.wss.on('connection', (ws: WebSocket, req: IncomingMessage) => {
      const params = new URL(req.url || '/', 'http://localhost').searchParams;
      const token = params.get('token') || null;

      const session: ClientSession = {
        id: uuidv4(),
        ws,
        authToken: token,
        connected: true,
        projectPath: params.get('path') || process.cwd(),
        bridge: new OpencodeBridge({
          projectPath: params.get('path') || process.cwd(),
        }),
        currentSessionId: null,
      };

      this.sessions.set(session.id, session);
      console.log(`[WS] Клиент ${session.id} подключился`);

      this.send(session, { type: 'connected' });

      ws.on('message', (raw) => {
        this.handleMessage(session, raw.toString()).catch((err) => {
          console.error(`[WS] ${session.id} message error:`, err.message);
          this.send(session, { type: 'error', error: `Internal error: ${err.message}` });
        });
      });
      ws.on('close', () => this.handleClose(session));
      ws.on('error', (err) => console.error(`[WS] ${session.id} error:`, err.message));
    });

    console.log(`[WS] Сервер запущен на ws://0.0.0.0:${this.port}/ws`);
  }

  private async handleMessage(session: ClientSession, raw: string): Promise<void> {
    try {
      const msg = JSON.parse(raw);

      switch (msg.type) {
        case 'auth':
          this.handleAuth(session, msg);
          break;

        case 'prompt':
          await this.handlePrompt(session, msg);
          break;

        case 'command':
          await this.handleCommand(session, msg);
          break;

        case 'list_files':
          await this.handleListFiles(session, msg);
          break;

        case 'read_file':
          await this.handleReadFile(session, msg);
          break;

        case 'write_file':
          await this.handleWriteFile(session, msg);
          break;

        case 'terminal_command':
          await this.handleTerminalCommand(session, msg);
          break;

        case 'terminal_resize':
          this.handleTerminalResize(session, msg);
          break;

        case 'session_list':
          await this.handleSessionList(session);
          break;

        case 'session_delete':
          await this.handleSessionDelete(session, msg);
          break;

        case 'ping':
          this.send(session, { type: 'pong', timestamp: Date.now() });
          break;

        default:
          this.send(session, { type: 'error', error: 'Неизвестный тип сообщения', code: 'UNKNOWN_TYPE' });
      }
    } catch (err: any) {
      this.send(session, { type: 'error', error: `Ошибка парсинга: ${err.message}` });
    }
  }

  private handleAuth(session: ClientSession, msg: any): void {
    session.authToken = msg.token || null;
    this.send(session, { type: 'auth', success: true });
  }

  private async handlePrompt(session: ClientSession, msg: any): Promise<void> {
    const bridge = session.bridge;
    const sid = msg.session_id || session.currentSessionId || undefined;

    const onMessage = (data: any) => {
      if (data.type === 'text' || data.type === 'tool') {
        this.send(session, {
          type: data.type,
          content: data.content,
          tool_name: data.tool_name,
          session_id: data.session_id || sid,
        });
      } else if (data.type === 'error') {
        this.send(session, { type: 'error', error: data.error, session_id: data.session_id || sid });
      }
    };
    const onDone = (data: any) => {
      bridge.off('message', onMessage);
      bridge.off('done', onDone);
      if (data.session_id) session.currentSessionId = data.session_id;
      this.send(session, { type: 'done', session_id: data.session_id || sid });
    };

    bridge.on('message', onMessage);
    bridge.on('done', onDone);

    bridge.sendPrompt(msg.prompt, sid, msg.model).catch((err: Error) => {
      bridge.off('message', onMessage);
      bridge.off('done', onDone);
      this.send(session, { type: 'error', error: err.message, session_id: sid || session.id });
    });
  }

  private async handleCommand(session: ClientSession, msg: any): Promise<void> {
    try {
      const result = await session.bridge.executeCommand(msg.command);
      this.send(session, { type: 'command_result', result, session_id: session.id });
    } catch (err: any) {
      this.send(session, { type: 'error', error: err.message });
    }
  }

  private async handleListFiles(session: ClientSession, msg: any): Promise<void> {
    try {
      const files = await session.bridge.getFiles(msg.path || '/');
      this.send(session, { type: 'file_list', files, path: msg.path || '/' });
    } catch (err: any) {
      this.send(session, { type: 'error', error: err.message });
    }
  }

  private async handleReadFile(session: ClientSession, msg: any): Promise<void> {
    try {
      const content = await session.bridge.readFile(msg.path);
      this.send(session, { type: 'file_content', path: msg.path, content });
    } catch (err: any) {
      this.send(session, { type: 'error', error: err.message });
    }
  }

  private async handleWriteFile(session: ClientSession, msg: any): Promise<void> {
    try {
      await session.bridge.writeFile(msg.path, msg.content);
      this.send(session, { type: 'file_written', path: msg.path });
    } catch (err: any) {
      this.send(session, { type: 'error', error: err.message });
    }
  }

  private async handleTerminalCommand(session: ClientSession, msg: any): Promise<void> {
    const bridge = session.bridge;
    if (!bridge.isRunning) {
      await bridge.start();
    }

    const result = await bridge.executeCommand(msg.command);
    this.send(session, {
      type: 'terminal_output',
      session_id: msg.session_id || session.id,
      data: result,
      exit_code: null,
    });
  }

  private handleTerminalResize(session: ClientSession, msg: any): void {
    console.log(`[Terminal] Resize ${session.id}: ${msg.cols}x${msg.rows}`);
  }

  private handleClose(session: ClientSession): void {
    session.connected = false;
    session.bridge.stop();
    this.sessions.delete(session.id);
    console.log(`[WS] Клиент ${session.id} отключился`);
  }

  private async handleSessionList(session: ClientSession): Promise<void> {
    try {
      const sessions = await session.bridge.listSessions();
      this.send(session, { type: 'session_list', sessions, session_id: session.id });
    } catch (err: any) {
      this.send(session, { type: 'error', error: err.message, session_id: session.id });
    }
  }

  private async handleSessionDelete(session: ClientSession, msg: any): Promise<void> {
    try {
      await session.bridge.deleteSession(msg.session_id);
      this.send(session, { type: 'session_deleted', session_id: msg.session_id });
    } catch (err: any) {
      this.send(session, { type: 'error', error: err.message, session_id: msg.session_id || session.id });
    }
  }

  send(session: ClientSession, data: any): void {
    if (session.ws.readyState === WebSocket.OPEN) {
      session.ws.send(JSON.stringify(data));
    }
  }

  close(): void {
    for (const session of this.sessions.values()) {
      session.bridge.stop();
      session.ws.close();
    }
    this.wss.close();
  }
}