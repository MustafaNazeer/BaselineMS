# Architecture

This document is a high level overview of the application's architecture. It complements `SPEC.md` Section 5 (which contains the canonical diagrams and invariants) and is intended for a reader who wants a quick orientation without reading the full spec.

**Status:** placeholder. The Documentation Engineer fills this in during Phase 0, with the canonical diagrams from `SPEC.md` Section 5 plus any clarifying prose.

## Sections to be written in Phase 0

1. **One paragraph summary** of the layered architecture.
2. **Layer diagram** (copied from `SPEC.md` Section 5.1 and kept in sync).
3. **Layer responsibilities and invariants** (copied from `SPEC.md` Section 5.2 and kept in sync).
4. **Cross cutting concerns**: privacy, accessibility, performance, testing.
5. **Key extension points**: how to add a new test module, how to evolve the data schema, how to add a new chart type.
6. **Decisions log**: links to ADRs in `docs/adr/`.

## Notes for the Documentation Engineer

Keep this file in sync with `SPEC.md` Section 5. If the spec changes, the architecture doc changes the same day. The intended audience is a developer who is unfamiliar with the project but knows Android well; a clinician should be pointed at `SPEC.md` instead.
