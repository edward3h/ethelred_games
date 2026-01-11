# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A multiplayer card game platform implementing NUO (a UNO variant) with a Java backend and SvelteKit frontend. The architecture supports multiple game types through a plugin-based game engine.

## Development Commands

### Running the Application

```bash
# Run both server (port 7000) and frontend (port 3000) in dev mode
./gradlew runDev

# Run server only
./gradlew :server:run

# Run frontend only (in frontend2/ directory)
pnpm dev
```

### Building

```bash
# Build entire project (includes frontend build via Gradle task)
./gradlew build

# Build specific modules
./gradlew :server:build
./gradlew :engine:build
./gradlew :games:nuo:build

# Frontend build only (in frontend2/ directory)
pnpm build
```

### Testing

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :server:test
./gradlew :engine:test

# Frontend tests
cd frontend2
pnpm check
pnpm lint
```

### Code Quality

```bash
# Format code (Java, Groovy)
./gradlew spotlessApply

# Check formatting without applying
./gradlew spotlessCheck

# Frontend formatting
cd frontend2
pnpm format
```

## Architecture

### Multi-Module Structure

- **engine/**: Core game framework providing action system, game lifecycle, and serialization
- **games/nuo/**: NUO game implementation (UNO variant)
- **games/duo/**: Placeholder for future game
- **server/**: Javalin HTTP server (port 7000)
- **frontend2/**: SvelteKit UI with TailwindCSS + DaisyUI
- **auto-player/**: AI bot player implementation
- **build-logic/**: Gradle convention plugins for shared build configuration

### Communication Architecture

**REST API + Polling** (no WebSocket in current frontend):

- Frontend polls game state every 1000ms via SvelteKit's `invalidateAll()`
- Actions sent as HTTP POST to `/api/{game}/{gameId}`
- Server responds with `ServerPlayerView` record containing:
  - `path`: API endpoint for this game
  - `playerView`: JSON-serialized game state visible to player
  - `message`: Optional error/status message

**Key API Endpoints**:
- `GET /api/games` - List available game types
- `POST /api/{game}` - Create new game (returns short code)
- `PUT /api/join/{shortCode}` - Join game via short code
- `GET /api/{game}/{gameId}` - Fetch current game state
- `POST /api/{game}/{gameId}` - Submit player action
- `POST /api/player/name` - Set player name (max 20 chars)

### Game Engine Plugin System

The engine uses **type-safe action performers** for extensibility:

1. **Action**: Typed command objects (`StringAction`, `BooleanAction`)
2. **ActionPerformer<G extends Game>**: Validates and executes actions on game state
3. **GameDefinition**: Factory + registry for game-specific action performers
4. **Game**: Holds game state, synchronized during action processing
5. **PlayerView**: Interface defining JSON-serialized state visible to players

**Data Flow**:
```
Client POST → Main.attach() → Engine.message(Channel, Action)
  → Lookup ActionPerformer by action.name()
  → Synchronized perform(game, player, action)
  → game.playerViews() → JSON response
```

**Game Lifecycle States**:
- `PRESTART`: Players joining, can add bots
- `IN_PROGRESS`: Active gameplay
- `ENDED`: Game complete, "play again" voting

### Dependency Injection

Uses **Dagger 2** with component-based architecture:

- `GameEngineComponent`: Singleton DI container
- Modules: `IdModule` (Snowflake IDs), `EngineModule` (core engine), `NuoModule` (NUO game)
- New games register via `@Provides @IntoSet GameDefinition<?>`

### Serialization

**Jackson JSON** with custom module (`MyCustomSerializationModule`):

- `ActionDeserializer`: Maps action names to Action subclasses
- `@JsonProperty`: Controls field visibility in PlayerView
- `@JsonInclude(NON_EMPTY)`: Omits empty collections
- `@JsonAnyGetter`: Merges player properties into JSON object

## Adding a New Game

1. Create game module under `games/yourgame/`
2. Implement `GameDefinition<YourGame>`:
   - Define game type identifier
   - Register action performers in `actionPerformers()` method
   - Implement `create()` factory method
3. Create `YourGame extends BaseGame<YourPlayer>`
4. Create `YourPlayerView extends BasePlayerView<YourPlayer>`
5. Implement action performers by implementing `ActionPerformer<YourGame>`
6. Create Dagger module with `@Provides @IntoSet GameDefinition<?>`
7. Add module to `GameEngineComponent`
8. Include in `settings.gradle`

## Key Patterns

### Thread Safety

- Games stored in `ConcurrentSkipListMap` with Caffeine cache (30min TTL)
- Each game synchronized during action processing
- Bot players execute in `ExecutorService` work-stealing pool

### Immutability

- Cards and game objects are immutable value objects
- Game state modified only through ActionPerformers
- Use `shortCode` for serialization keys

### Testing

- Uses Groovy + Spock framework for backend tests
- TestNG annotations in engine tests
- Gru framework for HTTP endpoint testing
- `nv-websocket-client` for WebSocket tests

### Frontend

- **SvelteKit** routing: `/nuo/[gameId]/+page.svelte`
- **Loaders**: `+page.ts` fetches game state via `fetch()`
- **Hooks**: `hooks.server.ts` proxies API calls in dev mode
- **Styling**: TailwindCSS + DaisyUI components
- **Icons**: svelte-fa (Font Awesome)

## Build System

**Gradle multi-module** with convention plugins in `build-logic/`:

- `ethelred-games.java-common-conventions`: Java 17, Spotless, OpenRewrite
- `ethelred-games.java-library-conventions`: Library defaults
- `ethelred-games.java-application-conventions`: Application defaults

**Frontend integration**:
- Gradle task `:frontend2:npmBuild` runs during main build
- Dev mode: Vite dev server with proxy to Javalin
- Prod mode: Javalin serves static frontend assets

## Requirements

- **Java**: JDK 25 (via Gradle toolchain)
- **Node.js**: 20+ (managed via `.tool-versions`)
- **Package manager**: pnpm
- **Gradle**: 9.2.1+ (wrapper included)

## Deployment

- **Development**: Vite dev server (port 3000) proxies to Javalin (port 7000)
- **Production**: Javalin serves static frontend assets from `frontend2/build/`
- **Docker**: `Dockerfile` provided for containerized deployment

## Important Files

| Purpose | Location |
|---------|----------|
| Server entry point | `server/src/main/java/org/ethelred/games/server/Main.java:146-217` |
| Engine implementation | `engine/src/main/java/org/ethelred/games/core/InMemoryEngineImpl.java:117-139` |
| DI component | `server/src/main/java/org/ethelred/games/server/GameEngineComponent.java` |
| NUO game logic | `games/nuo/src/main/java/org/ethelred/games/nuo/NuoGame.java` |
| NUO definition | `games/nuo/src/main/java/org/ethelred/games/nuo/NuoGameDefinition.java` |
| Frontend game page | `frontend2/src/routes/nuo/[gameId]/+page.svelte` |
| Jackson serialization | `engine/src/main/java/org/ethelred/games/core/MyCustomSerializationModule.java` |

## Pre-commit Hooks

Git hooks configured via Gradle plugin to run `./gradlew check` before commit. This enforces:
- Spotless code formatting
- Test suite passing
- Frontend linting (if frontend files changed)

## Known Limitations

**Groovy Tests (as of 2026-01-10):**
- Groovy/Spock tests are temporarily disabled in `ethelred-games.java-common-conventions.gradle`
- Java 25 requires Groovy 5.x, but Spock framework doesn't have a stable Groovy 5 release yet
- Using Groovy 5.0.0-alpha-10, but Spock 2.4 milestones are incompatible
- Tests will be re-enabled when stable Groovy 5 + Spock versions are available
- Core Java code compiles and runs successfully on Java 25
