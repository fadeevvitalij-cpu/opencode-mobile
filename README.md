<div align="center">

# OpenCode Mobile

**Android client for remote access to OpenCode AI coding assistant**

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-2024.02-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material3](https://img.shields.io/badge/Material_3-3.2-0061a4?logo=materialdesign&logoColor=white)](https://m3.material.io)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

---

[English](#english) • [Русский](#russian)

</div>

---

<h2 id="english">🇬🇧 English</h2>

**OpenCode Mobile** is an Android application that connects to an MCP (Model Context Protocol) server to interact with OpenCode — a terminal-based AI coding assistant. It provides a native mobile interface for chat, file browsing, and terminal access.

## Architecture

```
┌─────────────────┐     WebSocket      ┌──────────────────┐     spawn     ┌─────────────┐     API     ┌──────────┐
│  Android Client │ ◄──────────────►   │   MCP Server     │ ◄──────────►  │  OpenCode   │ ◄────────► │ LLM API  │
│  (Kotlin/Compose)│                   │  (TypeScript)     │               │    CLI      │            │(OpenRouter)│
└─────────────────┘                    └──────────────────┘               └─────────────┘            └──────────┘
```

The app communicates with the MCP server over WebSocket. The server acts as a bridge — it translates Android requests into OpenCode CLI commands and streams responses back.

## Features

- **Chat with AI** — multi-turn conversations with session support, model selection
- **File Browser** — browse remote filesystem, view file contents
- **Terminal** — execute shell commands on the host
- **Multiple Connections** — save and manage multiple server profiles
- **Theme & Skins** — 5 color skins (Default, Ocean, Forest, Sunset, Lavender) with light/dark modes
- **Font Size** — adjustable from 12 to 24sp
- **Auto-Reconnect** — automatic reconnection on connection loss (exponential backoff)
- **Root Path** — configurable starting directory for file browser and terminal

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 1.9.22 |
| UI | Jetpack Compose (BOM 2024.02) |
| Design | Material 3 |
| Navigation | Navigation Compose |
| WebSocket | OkHttp |
| JSON | Gson |
| Local DB | Room |
| Preferences | DataStore |
| Server | TypeScript + ws + uuid |

## Build

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/opencode-mobile.git
cd opencode-mobile

# Build APK
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Quick Start

### 1. Start the MCP server on host

```bash
cd mcp-server
npm install
npm run dev
```

Server listens on `ws://0.0.0.0:8765/ws` (health check on port 8765, WebSocket on 8766).

### 2. Verify OpenCode CLI

```bash
opencode models
```

The CLI must be installed on the host machine.

### 3. Connect from the app

- Open the app, tap **+** to add a connection
- Enter host IP, port (default 8765), optional auth token
- Select a model (or leave default)
- Set root path if needed (e.g. `D:/Opencode`)
- Tap the connection to start chatting

## Project Structure

```
opencode-mobile/
├── app/
│   ├── src/main/java/com/opencode/mobile/
│   │   ├── data/
│   │   │   ├── database/          # Room (DAO, AppDatabase, migrations)
│   │   │   ├── model/             # ConnectionEntity (Room entity)
│   │   │   ├── repository/        # ConnectionRepository
│   │   │   └── websocket/         # WebSocket client, protocol (WSMessage, WSResponse)
│   │   ├── ui/
│   │   │   ├── screens/
│   │   │   │   ├── chat/          # Chat screen, ChatViewModel
│   │   │   │   ├── connections/   # Connection list, add/edit dialog
│   │   │   │   ├── files/         # File browser, FileBrowserViewModel
│   │   │   │   ├── settings/      # Theme, skin, font size
│   │   │   │   └── terminal/      # Terminal screen, TerminalViewModel
│   │   │   ├── components/        # Shared components (EmptyState, etc.)
│   │   │   └── theme/             # Theme, colors, Skins (5 color schemes)
│   │   └── MainActivity.kt       # Entry point, bottom navigation
│   └── build.gradle.kts
├── mcp-server/                    # TypeScript bridge server
│   ├── src/
│   │   ├── index.ts               # Server entry point (health check, WS server)
│   │   ├── websocket-handler.ts   # WebSocket message routing
│   │   └── opencode-bridge.ts     # OpenCode CLI bridge (spawn, parse output)
│   └── package.json
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Configuration

| Setting | Description |
|---------|-------------|
| Theme | System / Light / Dark |
| Skin | Default, Ocean, Forest, Sunset, Lavender |
| Font Size | 12–24 sp |
| Auto-Reconnect | On/Off |
| Root Path | Starting directory for file browser and terminal |

## Screenshots

*(Add screenshots here)*

## License

MIT

---

<h2 id="russian">🇷🇺 Русский</h2>

**OpenCode Mobile** — Android-приложение для удалённой работы с OpenCode — AI-ассистентом для программирования, работающим в терминале. Предоставляет нативный мобильный интерфейс для чата, просмотра файлов и терминала.

## Архитектура

```
┌─────────────────┐     WebSocket      ┌──────────────────┐     spawn     ┌─────────────┐     API     ┌──────────┐
│  Android Client │ ◄──────────────►   │   MCP Server     │ ◄──────────►  │  OpenCode   │ ◄────────► │ LLM API  │
│  (Kotlin/Compose)│                   │  (TypeScript)     │               │    CLI      │            │(OpenRouter)│
└─────────────────┘                    └──────────────────┘               └─────────────┘            └──────────┘
```

Приложение общается с MCP-сервером через WebSocket. Сервер выступает мостом — преобразует запросы Android в команды OpenCode CLI и передаёт ответы обратно.

## Возможности

- **Чат с AI** — многошаговые диалоги с поддержкой сессий, выбор модели
- **Файловый браузер** — просмотр удалённой файловой системы, чтение файлов
- **Терминал** — выполнение shell-команд на хосте
- **Несколько подключений** — сохранение и управление профилями серверов
- **Тема и скины** — 5 цветовых схем (Стандартная, Океан, Лес, Закат, Лаванда) со светлой/тёмной темой
- **Размер шрифта** — от 12 до 24sp
- **Автопереподключение** — автоматическое восстановление соединения (экспоненциальная задержка)
- **Корневая директория** — настраиваемый начальный путь для браузера и терминала

## Технологии

| Компонент | Технология |
|-----------|-----------|
| Язык | Kotlin 1.9.22 |
| UI | Jetpack Compose (BOM 2024.02) |
| Дизайн | Material 3 |
| Навигация | Navigation Compose |
| WebSocket | OkHttp |
| JSON | Gson |
| Локальная БД | Room |
| Настройки | DataStore |
| Сервер | TypeScript + ws + uuid |

## Сборка

```bash
git clone https://github.com/YOUR_USERNAME/opencode-mobile.git
cd opencode-mobile
./gradlew assembleDebug
```

APK будет в `app/build/outputs/apk/debug/app-debug.apk`.

## Быстрый старт

### 1. Запустите MCP-сервер на хосте

```bash
cd mcp-server
npm install
npm run dev
```

Сервер слушает на `ws://0.0.0.0:8765/ws` (health check на 8765, WebSocket на 8766).

### 2. Проверьте OpenCode CLI

```bash
opencode models
```

CLI должен быть установлен на хосте.

### 3. Подключитесь из приложения

- Откройте приложение, нажмите **+** чтобы добавить подключение
- Укажите IP хоста, порт (по умолчанию 8765), опционально токен
- Выберите модель (или оставьте по умолчанию)
- Укажите корневую директорию (например `D:/Opencode`)
- Нажмите на подключение чтобы начать чат

## Структура проекта

```
opencode-mobile/
├── app/
│   ├── src/main/java/com/opencode/mobile/
│   │   ├── data/
│   │   │   ├── database/          # Room (DAO, AppDatabase, миграции)
│   │   │   ├── model/             # ConnectionEntity (Room entity)
│   │   │   ├── repository/        # ConnectionRepository
│   │   │   └── websocket/         # WebSocket клиент, протокол (WSMessage, WSResponse)
│   │   ├── ui/
│   │   │   ├── screens/
│   │   │   │   ├── chat/          # Чат, ChatViewModel
│   │   │   │   ├── connections/   # Список подключений, диалог добавления/редактирования
│   │   │   │   ├── files/         # Файловый браузер, FileBrowserViewModel
│   │   │   │   ├── settings/      # Тема, скин, размер шрифта
│   │   │   │   └── terminal/      # Терминал, TerminalViewModel
│   │   │   ├── components/        # Общие компоненты (EmptyState и др.)
│   │   │   └── theme/             # Тема, цвета, скины (5 цветовых схем)
│   │   └── MainActivity.kt       # Точка входа, нижняя навигация
│   └── build.gradle.kts
├── mcp-server/                    # TypeScript сервер-мост
│   ├── src/
│   │   ├── index.ts               # Точка входа (health check, WS сервер)
│   │   ├── websocket-handler.ts   # Маршрутизация WebSocket-сообщений
│   │   └── opencode-bridge.ts     # Мост к OpenCode CLI (spawn, парсинг вывода)
│   └── package.json
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Настройки

| Параметр | Описание |
|----------|---------|
| Тема | Системная / Светлая / Тёмная |
| Скин | Стандартный, Океан, Лес, Закат, Лаванда |
| Размер шрифта | 12–24 sp |
| Автопереподключение | Вкл/Выкл |
| Корневая директория | Начальный путь для файлового браузера и терминала |

## Скриншоты

*(Добавьте скриншоты)*

## Лицензия

MIT
