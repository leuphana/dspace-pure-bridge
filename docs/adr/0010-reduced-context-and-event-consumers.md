# ADR-0010: Reduced DSpace context and dedicated event consumer set

- **Status:** Accepted (retrospective)
- **Date:** 2026-11-02

## Context

Every item change in DSpace triggers events that are processed by the consumers of the context's dispatcher. With the default set (including `discovery`), importing thousands of Pure objects was slow. A CLI job also has no logged-in user, but DSpace needs an `EPerson` for many operations.

## Decision

All DSpace contexts are created via `CLIScriptContextUtils.createReducedContext()`, which:

- sets the current user to the EPerson whose e-mail is in the environment variable `ADMIN_EMAIL` (fails if not found);
- uses a dedicated event dispatcher named `dspacePureBridge` that operators configure with a reduced consumer list (recommended: `authority, versioning`);
- turns off the authorization system.

Contexts are closed via `CLIScriptContextUtils.closeContext()` (dispatch events, complete; abort on failure). The CLI checks at startup that the dispatcher is configured.

## Consequences

- The import is much faster. The Discovery index must be rebuilt separately (`index-discovery`).
- Code that needs a context must use `CLIScriptContextUtils`, not `new Context()`.
- Because authorization is off, the bridge can modify any item. It must only be run by trusted operators.
- Export code that needs fresh index data reindexes items explicitly (`IndexingService.indexContent`).
