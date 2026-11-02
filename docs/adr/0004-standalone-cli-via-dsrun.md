# ADR-0004: Standalone CLI JAR executed via `dsrun`

- **Status:** Accepted (retrospective)
- **Date:** 2026-11-02

## Context

The bridge needs full access to DSpace's domain services (items, relationships, authorization, Discovery, e-mail). Possible deployment options were a DSpace addon module built into DSpace, a separate service talking to the DSpace REST API, or a plain JAR on the DSpace classpath.

## Decision

The bridge is a single shaded JAR, placed in the DSpace `lib` directory and started with `./bin/dspace dsrun de.leuphana.escience.dspacepurebridge.DspacePureBridgeCLI`. Command-line options (`-i`, `-e`, `-c`, `-l`, `-x`, `-h`) are parsed with Apache Commons CLI.

`dspace-api` and `spring-boot-starter-web` are `provided` dependencies, supplied by the DSpace runtime. Pure-client dependencies (OkHttp, Gson, …) are shaded into the JAR.

## Consequences

- No rebuild of DSpace is needed. Deployment means copying one JAR (and the e-mail template).
- The bridge uses DSpace's Java API directly. This is fast and complete, but ties the bridge to a DSpace major version (currently 9.x; see the DSpace 9.2 compatibility commits).
- Library versions must not conflict with those on the DSpace classpath. Upgrading `provided` dependencies must follow the target DSpace version.
- The bridge is a batch job. Scheduling (cron, container entrypoint) is the operator's responsibility.
- DSpace services are obtained from DSpace's static service factories. Code that uses them needs static mocking in tests (see [ADR-0013](0013-unit-tests-with-mocked-dspace.md)).
