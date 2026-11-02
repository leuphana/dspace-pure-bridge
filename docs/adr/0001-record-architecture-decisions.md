# ADR-0001: Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-11-02

## Context

The bridge has grown through several releases (1.0 – 1.5) and refactorings. The reasons behind its structure exist only in the code and in the heads of its developers. New contributors, and AI coding assistants, need that reasoning to extend the tool without breaking its assumptions.

## Decision

We record significant architecture decisions as ADRs in `docs/adr/`, using the Nygard format (Context, Decision, Consequences). Decisions that were already in place on 2026-11-02 are documented retrospectively as ADR-0002 onward.

## Consequences

- Changes that touch an existing ADR must either respect it or supersede it with a new ADR.
- `CLAUDE.md` links to the ADRs so that assistants take them into account.
- Retrospective ADRs may contain reconstructed reasoning; they say so where it applies.
