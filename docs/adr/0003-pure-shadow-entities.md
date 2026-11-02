# ADR-0003: Pure shadow entities linked via relationships

- **Status:** Accepted (retrospective)
- **Date:** 2026-11-02

## Context

DSpace already holds `Person`, `OrgUnit` and `Project` entities that are curated in DSpace (e.g. name variants used in the repository). Pure holds its own version of the same real-world objects, sometimes with different names. Writing Pure data directly into the DSpace entities would overwrite curated DSpace data.

## Decision

Imported Pure objects are stored as separate DSpace entities of dedicated types `PurePerson`, `PureOrgUnit` and `PureProject`, each in its own configured collection. Each one is linked to its DSpace counterpart with a relationship (`isPurePersonOfPerson`, `isPureOrgUnitOfOrgUnit`, `isPureProjectOfProject`), configured with `copyToLeft` so that the DSpace entity can show the Pure data.

The Pure identity is stored in `local.Pure.uuid`, `local.Pure.id`, `local.Pure.lastModificationDate` and `local.Pure.visibility`.

The mapping between entity type, Pure endpoint, collection property and relationship type is defined in the enum `DSpacePureEntity`.

## Consequences

- DSpace and Pure names can differ without conflict.
- DSpace must be configured in advance: entity types, relationship types, metadata fields and collections (see README).
- An unlinked shadow entity is harmless. It simply exists until a matching DSpace entity appears (see [ADR-0009](0009-entity-matching-by-attribute-hash.md)).
- Adding a new Pure object type means extending `DSpacePureEntity`, the DTOs in `pure/apiobjects`, the matching logic and the DSpace configuration.
