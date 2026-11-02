# ADR-0009: Entity matching by attribute hash

- **Status:** Accepted (retrospective, confirmed as a deliberate decision by the maintainers)
- **Date:** 2026-11-02

## Context

Shadow entities ([ADR-0003](0003-pure-shadow-entities.md)) must be linked to existing DSpace entities. The DSpace entities usually carry no Pure identifier, so matching has to use descriptive attributes.

Attribute-based matching is a **deliberate choice**, not a stopgap. DSpace entities stay independent of Pure: they need no Pure-specific data entered by curators, and the link between the two worlds is established only by the bridge.

## Decision

During import, both sides are reduced to a SHA-256 hash of a few attributes and matched through in-memory caches (`PureToDSpace.dspaceEntityCache`):

| Entity | Hashed attributes |
|---|---|
| Person | family name + given name + ORCID |
| OrgUnit | legal name |
| Project | title |

The hash functions for the DSpace side live in `relations.EntityUtils`. On a match, the relationship is created if it does not already exist.

For the export, affiliations are resolved by **exact OrgUnit legal name** (`DSpaceObjectMappings.organizationNameToPureMap`), because affiliations are not modelled as entity relations at the moment.

## Consequences

- Matching is simple, fast (one pass per side) and needs no extra identifiers in DSpace.
- Matching is brittle: spelling, whitespace, diacritics or a missing or extra ORCID prevent a match. Persons without an ORCID only match if both sides lack it.
- Unmatched entities are not reported. They can only be found by inspecting DSpace.
- Duplicate names (two persons or org units with the same name) are resolved by "first wins" (`putIfAbsent`).
- Improvements should stay within this approach, e.g. normalizing the hashed attributes or reporting unmatched entities. Switching to identifier-based matching (Pure ID stored on the DSpace entity) would reverse this decision and needs a new ADR.
