# Development Notes and Links

## Frontend / client

* Node 18
* pnpm
* sveltekit

## Backend / server

* JDK 25 (upgraded from 17 on 2026-01-10)
* [Javalin](https://javalin.io/)
* ~~Redis~~

## Games

* [UNO rules](https://service.mattel.com/instruction_sheets/42001pr.pdf)


## Deployment

Development environment deployment will use Vite dev server to serve client assets. It will proxy the Javalin server.

Production environment deployment will use Javalin server to serve client assets as static files.

## Non-Goals

It would be nice to separate Lobby and game engine implementations, but for the initial MVP I'll just build what works.

Similarly, I'm trying to bear in mind an eventual scalable architecture but getting a working game in a single server is more important.

## Java 25 Upgrade (2026-01-10)

Upgraded from Java 17 to Java 25 (LTS release, Sept 2025).

**Changes:**
- Gradle 8.5 → 9.2.1 (required for Java 25 support)
- Java 17 → 25 (all modules)
- OpenRewrite Gradle plugin 5.40.6 → 6.25.0
- Docker images: eclipse-temurin:25.0.1_8-jdk-alpine
- CI/CD updated to use Java 25
- Updated .tool-versions for local development (temurin-25)

**Known Limitations:**
- Groovy tests temporarily disabled: Java 25 requires Groovy 5.x, but Spock framework doesn't yet have a stable Groovy 5 release
- Groovy 5.0.0-alpha-10 is in dependencies but Spock 2.4 milestone versions are incompatible
- TODO: Re-enable Groovy/Spock tests when stable versions are released

**Testing:** Core Java code builds and runs successfully on Java 25. Groovy tests will need to be re-enabled when compatible Groovy 5 + Spock combination is available.