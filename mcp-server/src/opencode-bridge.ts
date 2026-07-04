import { spawn, exec } from 'child_process';
import fs from 'fs/promises';
import path from 'path';
import { EventEmitter } from 'events';

export interface BridgeConfig {
  opencodePath?: string;
  projectPath?: string;
  model?: string;
}

interface OpenCodeEvent {
  type: string;
  content?: string;
  timestamp?: number;
  sessionID?: string;
  error?: { name: string; data: { message: string } };
  tool_name?: string;
  tool_args?: any;
  part?: {
    id?: string;
    messageID?: string;
    sessionID?: string;
    type?: string;
    text?: string;
    tool_name?: string;
    tool_args?: any;
  };
}

export class OpencodeBridge extends EventEmitter {
  private config: BridgeConfig;

  constructor(config: BridgeConfig = {}) {
    super();
    this.config = {
      opencodePath: config.opencodePath || 'opencode',
      projectPath: config.projectPath || process.cwd(),
      model: config.model,
    };
  }

  get isRunning(): boolean {
    return true;
  }

  async start(): Promise<void> {
  }

  async sendPrompt(prompt: string, sessionId?: string, model?: string): Promise<void> {
    const cmd = this.config.opencodePath!;
    const effectiveModel = model || this.config.model;
    const args = ['run', '--format', 'json', '--dangerously-skip-permissions'];
    if (effectiveModel) {
      args.push('--model', effectiveModel);
    }
    if (sessionId) {
      args.push('--session', sessionId, '--continue');
    }
    args.push(prompt);

    const proc = spawn(cmd, args, {
      cwd: this.config.projectPath,
      stdio: ['ignore', 'pipe', 'pipe'],
      env: { ...process.env },
    });

    let buffer = '';
    const sid = sessionId || 'default';
    let actualSessionId: string | null = null;

    const onData = (data: Buffer) => {
      buffer += data.toString();
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      for (const line of lines) {
        if (!line.trim()) continue;
        try {
          const event: OpenCodeEvent = JSON.parse(line);
          if (!actualSessionId && event.sessionID) actualSessionId = event.sessionID;
          switch (event.type) {
            case 'text':
              if (event.part?.text) {
                this.emit('message', { type: 'text', content: event.part.text, session_id: actualSessionId || sid });
              }
              break;
            case 'tool':
              this.emit('message', {
                type: 'tool',
                content: event.part?.text || '',
                tool_name: event.part?.tool_name || 'unknown',
                tool_args: event.part?.tool_args,
                session_id: actualSessionId || sid,
              });
              break;
            case 'error':
              this.emit('message', { type: 'error', error: event.error?.data?.message || event.part?.text || 'Unknown error', session_id: actualSessionId || sid });
              break;
          }
        } catch {}
      }
    };

    proc.stdout?.on('data', onData);
    proc.stderr?.on('data', (data: Buffer) => {
      const msg = data.toString().trim();
      if (msg) {
        this.emit('message', { type: 'error', error: msg, session_id: actualSessionId || sid });
      }
    });
    proc.on('close', (code) => {
      if (code !== 0 && buffer.trim()) {
        // попытка распарсить последний буфер как ошибку
        try {
          const err = JSON.parse(buffer.trim());
          if (err?.data?.message) {
            this.emit('message', { type: 'error', error: err.data.message, session_id: actualSessionId || sid });
          }
        } catch {}
      }
      this.emit('done', { session_id: actualSessionId || sid });
    });
    proc.on('error', (err: NodeJS.ErrnoException) => {
      this.emit('message', { type: 'error', error: `OpenCode error: ${err.message}`, session_id: actualSessionId || sid });
      this.emit('done', { session_id: actualSessionId || sid });
    });
  }

  async executeCommand(command: string): Promise<string> {
    return new Promise((resolve, reject) => {
      exec(command, { cwd: this.config.projectPath, timeout: 30000 }, (err, stdout, stderr) => {
        if (err) reject(new Error(stderr || err.message));
        else resolve(stdout);
      });
    });
  }

  async getFiles(dirPath: string): Promise<any[]> {
    try {
      const fullPath = path.resolve(this.config.projectPath!, dirPath.replace(/^\//, ''));
      const entries = await fs.readdir(fullPath, { withFileTypes: true });
      return entries.map((entry) => ({
        name: entry.name,
        path: path.posix.join(dirPath, entry.name),
        isDirectory: entry.isDirectory(),
        size: entry.isFile() ? 0 : 0,
      }));
    } catch {
      return [];
    }
  }

  async readFile(filePath: string): Promise<string> {
    const fullPath = path.resolve(this.config.projectPath!, filePath.replace(/^\//, ''));
    return fs.readFile(fullPath, 'utf-8');
  }

  async writeFile(filePath: string, content: string): Promise<void> {
    const fullPath = path.resolve(this.config.projectPath!, filePath.replace(/^\//, ''));
    await fs.writeFile(fullPath, content, 'utf-8');
  }

  async listSessions(): Promise<{ id: string; title: string; updated: string }[]> {
    return new Promise((resolve) => {
      exec('opencode session list', { timeout: 10000 }, (err, stdout) => {
        if (err) { resolve([]); return; }
        const lines = stdout.split('\n').filter(Boolean);
        const sessions: { id: string; title: string; updated: string }[] = [];
        let header = true;
        for (const line of lines) {
          if (header && line.includes('Session ID')) { header = false; continue; }
          if (line.startsWith('─') || header) continue;
          const parts = line.match(/(\S+)\s+(.+?)\s{2,}(\S.+\S)/);
          if (parts) {
            sessions.push({ id: parts[1].trim(), title: parts[2].trim(), updated: parts[3].trim() });
          }
        }
        resolve(sessions);
      });
    });
  }

  async deleteSession(sessionId: string): Promise<void> {
    return new Promise((resolve, reject) => {
      exec(`opencode session delete "${sessionId}"`, { timeout: 10000 }, (err) => {
        if (err) reject(new Error(`Failed to delete session: ${err.message}`));
        else resolve();
      });
    });
  }

  stop(): void {
  }
}
