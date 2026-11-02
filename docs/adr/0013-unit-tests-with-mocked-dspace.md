# ADR-0013: Unit tests against mocked DSpace services

- **Status:** Accepted (retrospective)
- **Date:** 2026-11-02

## Context

Running a real DSpace (database, Solr, configuration) or a Pure instance in CI is heavy, and the bridge's value lies in mapping and orchestration logic. DSpace services are obtained through static factories ([ADR-0004](0004-standalone-cli-via-dsrun.md)).

## Decision

- Tests are JUnit 5 unit tests with Mockito (`mockito-inline`, `mockStatic` for DSpace factories such as `ContentServiceFactory`, `DSpaceServicesFactory`, `HandleServiceFactory`).
- Classes receive their DSpace services through `DSpaceServicesContainer` (a builder-based parameter object), and Pure clients through overridable factory methods (e.g. `createResearchOutputApiClient`, `initRestTemplateBuilder`). Tests replace them with mocks or spies.
- Members that tests need are package-private, not public.
- CI (GitHub Actions) runs `./mvnw clean verify` on Java 17 for every push.
- There are no integration tests against a real DSpace or Pure. End-to-end verification is done manually with `--checkOnly` and `--exportHandle` on a test instance.

## Consequences

- Tests are fast and need no infrastructure.
- Mocks encode assumptions about DSpace and Pure behaviour. Upgrades of DSpace or the Pure API can break production while tests stay green, so they need a manual test run.
- Static mocking is verbose and has to be scoped carefully (`try-with-resources`).
- Generated tests of the OpenAPI client exist under `src/test/.../pure/generated`. They are not part of the bridge's own test design.
