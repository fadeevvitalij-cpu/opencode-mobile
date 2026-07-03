# opencode-mobile

Android-приложение на Kotlin + Jetpack Compose для удалённого подключения к OpenCode.

## Tech Stack

- Kotlin 1.9.22
- Jetpack Compose (BOM 2024.02)
- Material 3
- Navigation Compose
- OkHttp (WebSocket клиент)
- Gson

## Build

```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Project Structure

- `app/src/main/java/com/opencode/mobile/` — исходный код
  - `MainActivity.kt` — точка входа
  - `ui/theme/` — тема, цвета, типографика
  - `ui/screens/` — экраны приложения

## Conventions

- package: `com.opencode.mobile`
- Compose-компоненты в PascalCase
- Экран — файл с суффиксом `Screen.kt`
