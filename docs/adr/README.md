# Architecture Decision Records

This directory holds the Architecture Decision Records (ADRs) of the `dspace-pure-bridge`, written in the format proposed by [Michael Nygard](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions).

ADR-0002 to ADR-0013 were **documented retrospectively** on 2026-11-02. They were derived from the code base at version `1.6.0-SNAPSHOT` and describe decisions that had already been made and implemented. Their "Context" sections reconstruct the most likely reasoning; where a reason is inferred rather than known, the ADR says so.

## Index

| # | Title | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-system-of-record-per-object-type.md) | One system of record per object type | Accepted |
| [0003](0003-pure-shadow-entities.md) | Pure shadow entities linked via relationships | Accepted |
| [0004](0004-standalone-cli-via-dsrun.md) | Standalone CLI JAR executed via `dsrun` | Accepted |
| [0005](0005-configuration-via-dspace-properties.md) | Configuration via DSpace properties | Accepted |
| [0006](0006-pure-api-access.md) | Generated OpenAPI client for writes, hand-written DTOs for bulk reads | Accepted |
| [0007](0007-export-state-and-idempotency.md) | Export state in item metadata, duplicate check before creation | Accepted |
| [0008](0008-discovery-for-item-selection.md) | Discovery (Solr) for item selection | Accepted |
| [0009](0009-entity-matching-by-attribute-hash.md) | Entity matching by attribute hash | Accepted |
| [0010](0010-reduced-context-and-event-consumers.md) | Reduced DSpace context and dedicated event consumer set | Accepted |
| [0011](0011-parallel-processing-per-item.md) | Parallel processing, one context per item | Accepted |
| [0012](0012-export-type-strategy-and-classification-mapping.md) | Export types as strategies; vocabulary mapped to Pure classifications | Accepted |
| [0013](0013-unit-tests-with-mocked-dspace.md) | Unit tests against mocked DSpace services | Accepted |

## Writing a new ADR

1. Copy [`template.md`](template.md) to `NNNN-short-title.md` using the next free number.
2. Fill in Context, Decision, Consequences. Keep it short; link to code with repository-relative paths.
3. Set the status to `Proposed`. Change it to `Accepted` when the change is merged.
4. Never rewrite an accepted ADR. To revise a decision, write a new ADR and mark the old one `Superseded by ADR-NNNN`.
5. Add the new ADR to the index above.
