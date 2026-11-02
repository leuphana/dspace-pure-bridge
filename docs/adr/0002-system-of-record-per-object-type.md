# ADR-0002: One system of record per object type

- **Status:** Accepted (retrospective)
- **Date:** 2026-11-02

## Context

Leuphana operates both Pure (CRIS) and DSpace (repository). Both systems can hold persons, organizations, projects and publications. Synchronizing the same object in both directions would require change detection on both sides and conflict resolution, which neither system supports well.

## Decision

Each object type has exactly one system of record, and data flows in one direction only:

| Object type | System of record | Direction |
|---|---|---|
| Person, organizational unit, project | Pure | Pure → DSpace (`--import`) |
| Publication (student thesis, research output) | DSpace | DSpace → Pure (`--export`) |

The import must run before the export, because exported publications reference Pure persons and organizations by their Pure UUIDs.

## Consequences

- No conflict resolution is needed; the target side is overwritten (import) or only created (export).
- Corrections must be made in the system of record. Edits to imported data in DSpace are overwritten by the next import if the Pure record changed.
- Publications are only *created* in Pure. Later changes in DSpace are not propagated (see [ADR-0007](0007-export-state-and-idempotency.md)).
- Import and export are separate CLI modes and can be scheduled independently.
