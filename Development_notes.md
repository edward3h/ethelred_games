# Development Notes and Links

## Frontend / client

* Node 22.20.0 (upgraded from 20.11.0 on 2026-01-11)
* pnpm 10.16.1 (pinned via packageManager field)
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

**Update (2026-01-11):**
- Upgraded Groovy from 5.0.0-alpha-10 to 5.0.3 (latest stable)
- Re-enabled Groovy tests with Spock 2.4-groovy-5.0
- Groovy 5.0.3 and Spock 2.4 stable now fully compatible with Java 25
- All 43 Groovy tests across 8 test classes now compile and run successfully

## Node.js 22 Upgrade (2026-01-11)

Upgraded from Node.js 20.11.0 to 22.20.0 (LTS "Jod").

**Changes:**
- Node.js 20.11.0 → 22.20.0 (Active LTS until October 2025, Maintenance LTS until April 2027)
- Pinned pnpm to 10.16.1 via packageManager field in package.json
- Updated .tool-versions for local development
- Updated GitHub Actions workflow to Node 22.20.0
- Updated Dockerfile to use node:22.20.0-alpine
- Updated package.json engines constraint to >=22
- Regenerated pnpm-lock.yaml with lockfile v9.0 format
- All frontend dependencies (SvelteKit 1.30.3, Vite 4.5.1) verified compatible with Node 22

**Rationale:** Node 20 enters Maintenance LTS in October 2025, while Node 22 provides Active LTS support through October 2025 and Maintenance support until April 2027. Conservative approach skipping Node 24 to minimize compatibility risk.