# ADR-0005: Configuration via DSpace properties

- **Status:** Accepted (retrospective)
- **Date:** 2026-11-02

## Context

Institutions differ in collections, publication types, metadata schemas, controlled vocabularies and Pure classification URIs. DSpace operators already manage `local.cfg` and environment-based configuration.

## Decision

All bridge settings are DSpace configuration properties, read through DSpace's `ConfigurationService`, under the prefix `dspace-pure-bridge.`:

| Prefix | Purpose |
|---|---|
| `dspace-pure-bridge.pure.ws.*` | Pure endpoint, API key, page size |
| `dspace-pure-bridge.entities.<entity>.collection` | Collections for shadow entities |
| `dspace-pure-bridge.export.filter` | Which collections/types are exported |
| `dspace-pure-bridge.export.exportTypeForType` | DSpace type → Pure export type |
| `dspace-pure-bridge.export.metadata.<exportType>.<name>` | DSpace metadata field per Pure attribute |
| `dspace-pure-bridge.export.mapping.<exportType>.<VOCAB>.<value>` | DSpace value → Pure classification URI |
| `dspace-pure-bridge.export.default*` | Fallbacks for mandatory Pure fields |
| `dspace-pure-bridge.export.mail.recipients` | Recipients of the result e-mail |

Exceptions: the acting EPerson comes from the environment variable `ADMIN_EMAIL`, and the event dispatcher `event.dispatcher.dspacePureBridge.*` uses DSpace's own key space.

Metadata field names have code defaults (documented in the README). The special mapping key `default` is used as a fallback value.

## Consequences

- Institution-specific behaviour changes without code changes and without rebuilding.
- The README is the reference documentation for all properties. A new property must be documented there in the same change.
- Required properties are validated at startup only partly (Pure endpoint, API key, event dispatcher). Missing mappings only show up as per-item errors at export time.
- Property values follow DSpace conventions (comma-separated arrays, variable interpolation).
