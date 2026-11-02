# ADR-0007: Export state in item metadata, duplicate check before creation

- **Status:** Accepted (retrospective)
- **Date:** 2026-11-02

## Context

The export runs repeatedly (e.g. nightly) over the same collections. It must never create the same publication twice in Pure, including publications that were entered in Pure manually before the bridge existed. A separate state store (file or database table) would add operational burden.

## Decision

- **Export marker:** After a successful export, the Pure UUID is written to the publication's `local.Pure.uuid` metadata, and the item is reindexed immediately. Items carrying `local.Pure.uuid` are excluded from the export query (Discovery filter `!local.Pure.uuid:*`) and are additionally skipped in `DSpaceToPure.syncItemThread`.
- **Duplicate check:** Before creating a record, the exporter searches Pure by title (`POST /<endpoint>/search`) and compares DOI or title (`concreteDuplicateCheck`). On a match, the existing Pure UUID is stored as if exported, and the result is reported as "already existed in Pure".
- **Dry run:** `--checkOnly` builds and validates the export object but does not send it.

## Consequences

- The export is idempotent without external state. The DSpace item itself records whether it has been exported.
- To re-export an item, an operator removes `local.Pure.uuid`.
- Changes made in DSpace after the export are not propagated (consistent with [ADR-0002](0002-system-of-record-per-object-type.md)).
- The duplicate check is currently only active for `STUDENT_THESIS`. `ExportType.RESEARCH_OUTPUT` has no search result class, so research outputs rely on the export marker only.
- Title matching can produce false positives for generic titles. A matched item is reported but not exported.
