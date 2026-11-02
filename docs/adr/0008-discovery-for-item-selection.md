# ADR-0008: Discovery (Solr) for item selection

- **Status:** Accepted (retrospective)
- **Date:** 2026-11-02

## Context

Earlier versions selected exportable publications and matching candidates by iterating over items and reading their metadata from the database. With large repositories this was slow (see commit `c35ebfd`, "Enhance Retrieval of exportable Items using SOLR query filters instead of querying metadata table").

## Decision

Item selection uses DSpace Discovery (`SearchService`) through `search.ItemFinder`:

- Each use case is a `SearchQueryType` with a base query (entity type) and an optional filter query (e.g. `PUBLICATION_EXPORT` = publications without `local.Pure.uuid`).
- Export filters add collection filter queries (`PublicationExportFilter.getFilterQueries`).
- Results are paged (100 per page, sorted by `search.resourceid`) and handed to an `ItemProcessor` callback.

The shadow-entity cache of the import is an exception: it reads the configured collection directly with `ItemService.findByCollection`.

## Consequences

- Selection is fast and pushes filtering into Solr.
- The Discovery index must be current. The import does not update it (the `discovery` consumer is disabled, see [ADR-0010](0010-reduced-context-and-event-consumers.md)), so `index-discovery` must run after the import and before the export.
- The export reindexes each exported item at once, so a later run with a stale index does not export it again.
- Solr field names (`local.Pure.uuid`, `organization.legalName`, `search.entitytype`) are coupled to the DSpace Discovery configuration.
