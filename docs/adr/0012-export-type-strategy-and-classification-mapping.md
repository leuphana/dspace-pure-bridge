# ADR-0012: Export types as strategies; vocabulary mapped to Pure classifications

- **Status:** Accepted (retrospective)
- **Date:** 2026-11-02

## Context

Pure stores theses and research outputs at different endpoints with different required fields and classification schemes. Today the bridge exports to `student-theses` and `research-outputs`. **More export types and more Pure target endpoints are expected** (e.g. other research-output subtypes, or other Pure content types), so the design must allow adding them without touching the orchestration. Pure expects classification references (URIs that must exist in the Pure instance), not free-text values. DSpace items carry institution-specific controlled vocabulary values.

## Decision

- **Strategy per export type:** `ExportType` (`STUDENT_THESIS`, `RESEARCH_OUTPUT`) selects an exporter from a registry in `DSpaceToPure`. Exporters implement `DSpaceExporter` and extend `AbstractExport`, which holds shared logic (authors and persons, titles and abstracts, contributors, duplicate check, defaults). Type-specific logic lives in `StudentThesisExport` and `ResearchOutputExport`.
- **Routing:** `export.filter` decides *whether* an item is exported. `export.exportTypeForType` decides *how* (with a `default` entry).
- **Classification mapping:** At startup, each exporter fetches the allowed classifications from Pure for each mapping type (`StudentThesisMappingType`, `ResearchOutputMappingType` together with `Pure*ClassificationRefFetcher`). DSpace values are mapped to Pure URIs through configuration ([ADR-0005](0005-configuration-via-dspace-properties.md)), and the URI is resolved to a `ClassificationRef` from the fetched list. A value that cannot be resolved makes the item fail with a clear error.
- **Mandatory fallbacks:** Missing authors or organizations fall back to the configured default author or organization.

## Consequences

- A new Pure export type or target endpoint means: a new `ExportType` constant (with its endpoint), an exporter subclass, a mapping-type enum, a config section and README docs. Orchestration stays untouched.
- Invalid mapping configuration is detected per item, not at startup. `--checkOnly` exposes such errors without side effects.
- Startup needs Pure to be reachable (classification fetch). Without Pure, the export cannot run, even with `--checkOnly`.
- Known limitation (**not** a design decision): research outputs are always created as `BookAnthology`, and their contributor roles are fetched from the book-anthology classification (`allowed-book-anthology-contributor-roles`). This only reflects the current use (dissertations). It should be lifted when further research-output types are needed, e.g. by deriving the Pure subtype and its contributor-role classification from the type mapping.
- Known limitation: the license is read only from bitstream metadata.
