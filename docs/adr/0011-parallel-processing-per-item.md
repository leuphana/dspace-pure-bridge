# ADR-0011: Parallel processing, one context per item

- **Status:** Accepted (retrospective)
- **Date:** 2026-11-02

## Context

Import and export are dominated by I/O: Pure HTTP calls and DSpace database access. Processing tens of thousands of objects sequentially takes too long for a nightly run. A DSpace `Context` wraps a database session and must not be shared across threads.

## Decision

- Both import (`PureToDSpace.syncObjects`) and export (`DSpaceToPure.syncItems`) submit one task per object to a fixed thread pool sized to `Runtime.availableProcessors()`.
- Each task creates its own reduced context ([ADR-0010](0010-reduced-context-and-event-consumers.md)), commits it on success and aborts it on failure. Only the item UUID is passed between threads, never a DSpace object.
- Shared state lives in thread-safe structures: `ConcurrentHashMap` caches for import, and concurrent sets in `ExportStatus` for results.
- A failing item is recorded (export) or logged (import), and processing of other items continues.

## Consequences

- Throughput scales with cores. Each item is its own transaction, so a failure rolls back only that item.
- The DB connection pool and the Pure API rate limits must allow `availableProcessors()` concurrent sessions. The pool size is not configurable at present.
- `--exportLimit` is checked when an item starts, so concurrent tasks can overshoot the limit slightly.
- Import caches are `static` and live for the JVM run. That is fine for a CLI, but tests must reset them.
- Ordering of log output and result e-mail entries is not deterministic.
