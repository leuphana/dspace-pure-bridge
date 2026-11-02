# CLAUDE.md

Guidance for Claude Code (and human contributors) working on this repository.

## What this project is

`dspace-pure-bridge` is a batch CLI that runs inside a DSpace 9.x installation (`./bin/dspace dsrun …DspacePureBridgeCLI`). It imports persons, org units and projects from Elsevier Pure (API 5.31.x) into DSpace, and exports publications from DSpace to Pure.

- Goals, scope and non-goals: [`VISION.md`](VISION.md)
- Architecture decisions: [`docs/adr/`](docs/adr/README.md). **Read the relevant ADRs before changing behaviour.**
- User/operator documentation and every configuration property: [`README.md`](README.md)

## Build & test

```bash
./mvnw clean verify                      # build + all tests (same as CI)
./mvnw test -Dtest=DSpaceToPureTest      # single test class
./mvnw test -Dtest='DSpaceToPureTest#syncItems*'   # single test method(s)
./mvnw -DskipTests package               # shaded JAR in target/
```

- Java 17, Maven Wrapper; CI is `.github/workflows/ci.yml`.
- `dspace-api` and `spring-boot-starter-web` are `provided`. Everything else is shaded into the JAR.

## Code map

```
src/main/java/de/leuphana/escience/dspacepurebridge/
├── DspacePureBridgeCLI          entry point, option handling, startup checks
├── PureSyncCLIConfiguration     CLI options
├── CLIScriptContextUtils        reduced DSpace Context (ADR-0010)
├── DSpaceServicesContainer      parameter object carrying DSpace services
├── Constants                    local.Pure.* metadata field names
├── pure/imports/                Pure → DSpace (PureToDSpace, DSpacePureEntity)
├── pure/export/                 DSpace → Pure (DSpaceToPure orchestrator,
│                                AbstractExport + StudentThesisExport / ResearchOutputExport,
│                                *MappingType, *ClassificationRefFetcher, filter/)
├── pure/apiobjects/             hand-written DTOs for bulk reads (ADR-0006)
├── pure/generated/              GENERATED OpenAPI client, do not edit (ADR-0006)
├── search/                      Discovery-based item selection (ADR-0008)
├── relations/                   relationship + entity-hash helpers (ADR-0009)
└── identifiers/                 primary identifier (DOI/URI) detection
```

The hand-written code is about 3,500 lines plus about 3,000 lines of tests. Everything under `pure/generated` (main and test) is generated.

## Ground rules for changes

### Process
- **Plan first, then implement test-driven** (Red → Green → Refactor). Every behavioural change comes with a unit test that fails before the change.
- Keep changes small and focused; one concern per branch/PR. Branch naming: `feature/<issue>-<topic>`, `fix/<topic>`, `docs/<topic>`.
- If a change contradicts an ADR, stop and propose a new ADR (`docs/adr/template.md`) instead of silently diverging.

### Architecture constraints (see ADRs)
- **Data direction is fixed:** persons, org units and projects flow Pure → DSpace; publications flow DSpace → Pure, create-only (ADR-0002, ADR-0007). Do not add bidirectional sync or Pure updates or deletes without a new ADR.
- **Never edit `pure/generated/`.** To change the Pure client, regenerate the whole package from the Pure OpenAPI spec and commit it as a separate change. Exclude it from reviews, refactorings, formatting and coverage.
- **Contexts:** always obtain them via `CLIScriptContextUtils.createReducedContext()` and release them via `closeContext()`. One context per thread or task; pass UUIDs, not DSpace objects, across threads (ADR-0010, ADR-0011).
- **Shared state** touched by worker threads must be thread-safe (`ConcurrentHashMap`, concurrent sets).
- **Idempotency:** an exported item must get `local.Pure.uuid` and be reindexed. Never create a Pure record without the export marker and (where available) the duplicate check (ADR-0007).
- **Item selection** goes through `ItemFinder` / `SearchQueryType` (Discovery), not through full scans of the metadata table (ADR-0008).
- **Per-item failures must not abort the run.** Record them via `ExportStatus.error(...)` so they reach the result e-mail. Only configuration or infrastructure errors may abort.
- **New export type:** add an `ExportType` constant, an `AbstractExport` subclass, a `*MappingType` enum, config keys and README docs (ADR-0012). Do not branch on the export type inside `DSpaceToPure`.

### Configuration
- All settings are DSpace properties with the prefix `dspace-pure-bridge.` (ADR-0005). No new config files, and no environment variables beyond `ADMIN_EMAIL`.
- Every new or changed property **must** be documented in `README.md` (syntax, parameters, example, default) in the same change.
- No institution-specific values (Leuphana handles, UUIDs, URIs) in code; use properties with documented defaults.
- Never commit real API keys, e-mail addresses or `local.cfg`.

### Testing
- JUnit 5 and Mockito. Use `mockStatic` in `try-with-resources` for DSpace factories (see `DspacePureBridgeCLITest`).
- Inject collaborators via `DSpaceServicesContainer` and overridable package-private factory methods (`createResearchOutputApiClient`, `initRestTemplateBuilder`, …) so tests can replace them.
- Prefer package-private visibility for members that tests need, not `public`.
- Import caches in `PureToDSpace` are `static`. Reset them in tests that touch them.
- No test may need network access, a database, Solr or a running DSpace/Pure.

### Code style
- Code, comments, log messages, docs and commit messages are in **English**.
- Follow the existing style: SLF4J parameterized logging (`log.info("… {}", x)`), Apache Commons `StringUtils`, Java 17 features (pattern matching `instanceof`, records are fine for new value types).
- Log at `info` for per-item progress and at `error` for per-item failures (through `ExportStatus`). Never log the API key.

### Compatibility
- Target: DSpace 9.x (currently 9.2), Java 17, Pure API 5.31.x. Keep `provided` dependency versions aligned with the target DSpace release.
- Do not add dependencies that clash with the DSpace classpath. Ask before adding any new dependency.

### Releases & git
- Version is managed by the maven-release-plugin (`release.sh`, tag format `<version>`). Do not change `<version>` in `pom.xml` by hand.
- `release.sh` pushes to `origin`. Only maintainers run it.

## Known limitations / tech debt (good places to be careful)

- Research outputs are always created as `BookAnthology`, with book-anthology contributor roles. This is a limitation, not a design decision: more export types and target endpoints are expected (ADR-0012).
- There is no duplicate check for `RESEARCH_OUTPUT` (ADR-0007).
- Entity matching by attribute hash is deliberate (ADR-0009), but brittle against spelling differences, and unmatched entities are not reported. Improve within that approach; do not switch to identifier-based matching without a new ADR.
- The license comes only from bitstream metadata.
- Exceptions rethrown inside worker tasks (`throw new RuntimeException(e)` in `PureToDSpace.syncObjects` / `DSpaceToPure.syncItemThread`) end up in the thread pool's default handler (stderr), not in the log or the result e-mail.
