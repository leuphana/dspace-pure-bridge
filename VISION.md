# Vision: dspace-pure-bridge

## Purpose

Research information at Leuphana University Lüneburg lives in two systems with different strengths:

- **Elsevier Pure** (CRIS) is the authoritative source for *who* and *where*: persons, organizational units, projects.
- **DSpace** (repository) is the authoritative source for *what*: full-text publications and student theses.

The `dspace-pure-bridge` keeps both systems consistent **without manual double entry**. Every system stays the owner of its own data, and the bridge carries it to the other side.

## Problem we solve

Without the bridge:
- Librarians and research staff re-type publication metadata into Pure.
- Authors, affiliations and projects in DSpace drift away from the official Pure records.
- Theses and open-access full texts in DSpace are not visible in the research portal.

With the bridge:
- Publications deposited in DSpace appear in Pure automatically, linked to the correct Pure persons and organizations.
- DSpace entities carry the Pure identifiers needed for that linking.
- Curators get a report per run listing what was exported and what needs attention.

## Target users

| User | Need |
|---|---|
| Repository managers / curators | Reliable, unattended export; a clear report of failures they can fix in DSpace |
| DSpace operators | A tool that drops into an existing DSpace 9.x installation (`dsrun`), configured like the rest of DSpace |
| Other institutions running DSpace + Pure | A configurable, documented bridge they can adopt without forking |

## Guiding principles

1. **Single source of truth per object type.** Data flows in one direction per type (persons/orgs/projects: Pure → DSpace; publications: DSpace → Pure). The bridge never resolves conflicts by merging (see [ADR-0002](docs/adr/0002-system-of-record-per-object-type.md)).
2. **Safe by default.** Exports are idempotent: an item is never created twice in Pure (export marker plus duplicate check). A dry run (`--checkOnly`) is always available.
3. **Configuration over code.** Institution-specific choices (collections, types, metadata fields, vocabularies, defaults) are DSpace properties, not code changes.
4. **Native to DSpace.** Runs inside the DSpace runtime, uses DSpace services, configuration, e-mail templates and the Discovery index. No additional server and no separate database.
5. **Transparent results.** Each run produces logs and a result e-mail per item. A failure of one item never stops the others.
6. **Small and testable.** Business logic stays independent of a running DSpace or Pure, so it can be unit-tested.

## Scope

**In scope**
- Import of Pure persons, organizational units and projects as DSpace entities, including relations to existing DSpace entities.
- Export of DSpace publications to Pure as `student-theses` or `research-outputs`.
- Configurable filtering, type mapping, metadata mapping and vocabulary mapping.
- Result reporting via e-mail.

**Out of scope (non-goals)**
- Bidirectional synchronization or conflict resolution for the same object.
- Updating or deleting publications in Pure after the initial export.
- Transferring files (bitstreams) to Pure. Only access and license information is transferred.
- A user interface or a long-running service. The bridge is a batch CLI triggered by cron or an operator.
- Supporting repository or CRIS products other than DSpace and Pure.

## Direction of further development

The following directions follow from the current limitations. They are candidates, not commitments:

- **More export types and target endpoints:** research-output subtypes other than dissertations (currently hard-wired to `BookAnthology`), and further Pure endpoints beyond `student-theses` and `research-outputs`.
- **Duplicate check for research outputs:** today the check only exists for student theses.
- **Configurable license source:** item metadata in addition to bitstream metadata.
- **More robust entity matching:** keep attribute-based matching (ADR-0009), but normalize the attributes and report entities that could not be matched.
- **Better operability:** a structured run summary (counts, durations), a clear exit code and an import report.
- **Adoption by others:** remove Leuphana-specific defaults from code, provide example configurations, and keep up with DSpace and Pure API releases.

## Success criteria

- Exportable publications reach Pure without manual intervention; the remaining failures are actionable from the result e-mail.
- No duplicate records are created in Pure by the bridge.
- A DSpace or Pure upgrade needs, at most, a dependency bump or regenerated API client, not a redesign.
