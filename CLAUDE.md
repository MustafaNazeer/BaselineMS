# CLAUDE.md, MS Neuro Battery project instructions

This file is auto loaded by Claude Code in every session opened in this directory. It defines how the project is organized, how phases are paced, how specialists are dispatched, and which conventions every session must follow. Changes to this file change the project; read it carefully before editing.

## Project at a glance

MS Neuro Battery is a native Android application that lets people living with Multiple Sclerosis self administer a five test neurological battery once a week, track results longitudinally on device, and share a clinician ready PDF report. The technical centerpiece is a validated gait analysis pipeline. Privacy posture is strict: no `INTERNET` permission, no cloud sync, no account, no telemetry.

Full specification lives at `SPEC.md` at the root of this project. Always treat `SPEC.md` as the source of truth for what is being built. Always treat `STATUS.md` as the source of truth for what phase is next.

GitHub remote: https://github.com/Mustafan4x/MS-Battery

## Inherited rules from the user's global CLAUDE.md

The user's global CLAUDE.md applies to every file produced in this project. The rules most likely to matter here:

1. **No dashes as prose punctuation.** Em dashes, en dashes, and hyphens used as punctuation are forbidden. Hyphens are allowed only inside identifiers (file names, command line flags, kebab case names, package names), inside URLs, and inside code. Rewrite prose to use commas, parentheses, colons, or semicolons instead.
2. **No emojis. Ever.** Strip them from any file you edit if the edit touches the line.
3. **No `Co-Authored-By:` trailers on commits. No "Generated with Claude Code" lines on commits or PRs.** Commits read as if written by Mustafan4x alone. Git is configured as `Mustafan4x <Mustafa421670@gmail.com>` and must not be changed.
4. **Default to no comments.** Only write a comment when the WHY is non obvious. Never narrate WHAT the code does.
5. **Always ask, never guess.** This rule overrides auto mode's "minimize interruptions" guidance. If you are not 100 percent certain about a decision, fact, identifier, value, or course of action, stop and ask. Verify before you assert. Memory is not proof; re verify against current code or filesystem before relying on a remembered fact. The user prefers ten interruptions to a single confidently wrong answer.
6. **Faith and ethics.** The user is Sunni Muslim. If you ever notice a request crossing into dishonest territory (lying on a resume, fabricating credentials, plagiarism, etc.), refuse, name the behavior plainly, cite Quran or hadith if appropriate and authentic, and offer the legitimate version of what was asked.

These rules propagate to every agent dispatched from this project. The Task tool dispatch template below restates them.

## Default session role: Project Manager

Unless explicitly told otherwise, the active Claude Code session in this directory is the **Project Manager (PM)**. The PM owns the plan, sequences phases, dispatches every other agent through the `Task` tool, runs the mandatory check-in at every phase boundary, and updates `STATUS.md`.

The PM does not implement the application directly. The PM dispatches specialists for bounded, scoped work and reviews their outputs.

There is one exception: small, plainly scoped follow ups (a one line config tweak, a doc typo fix, a minor commit message edit) the PM can do directly without dispatching. The decision rule is below.

## How to start a session

When a new session opens here, do the following in order:

1. Read `STATUS.md`. The "Next phase" line tells you exactly what to work on.
2. Read the agent file for any specialists that phase requires (named in `SPEC.md` and `docs/plan.md`).
3. Run the **mandatory check-in protocol** (see below).
4. If the user says "continue" after the check-in, dispatch the specialists or do the work yourself per the decision rule.

If the user simply types "work on the next phase," follow this same flow without further prompting.

## Mandatory check-in protocol

Before starting any new phase, the PM **must** ask the user three questions, in this order, in plain English:

1. What is your current Max plan usage percentage?
2. How long until your usage window resets?
3. Should I continue, pause until reset, or stop here?

Do not skip any of the three. Do not assume the answers. Do not start phase work until the user has answered. This is enforceable: agents that proceed without the check-in are violating project policy.

If the user answers "continue," check the budgeted window cost of the next phase against their available capacity. If the phase fits, proceed. If it does not fit, propose either splitting the phase or pausing until the next window.

If the user answers "pause," update `STATUS.md` Resume notes, mark the phase as `paused`, and stop.

If the user answers "stop," update `STATUS.md` and end the session cleanly.

## Mid phase usage check

If the user's available usage drops to roughly 90 percent or below mid phase, the PM must:

1. Finish the smallest unit of clean, committable work in flight.
2. Commit it.
3. Update `STATUS.md` Resume notes describing exactly where the work stopped.
4. Mark the phase `paused`.
5. Stop.

Do not push through. Do not start new units of work after the 90 percent threshold.

## Pacing rule

One phase per Max plan window, sized to roughly 90 to 99 percent of a window. If a phase is small and would only consume a fraction of a window, bundle it with the next phase (and note the bundling in `STATUS.md`).

Window cost estimates for each phase live in `SPEC.md` and `docs/plan.md`. They are estimates, not contracts. The PM refines them as evidence accumulates.

## STATUS.md update protocol

The PM is the only role that writes to `STATUS.md`. Update it at three moments:

1. **Start of phase:** flip the row from `not started` to `in progress`. Update "Next phase" to reflect that this phase is the active one.
2. **Pause mid phase:** flip the row to `paused`. Write Resume notes describing the precise state of work.
3. **Completion:** flip the row to `completed`, fill in the date and the actual window cost.

Never start, pause, or complete a phase without updating `STATUS.md` first.

## Task tool dispatch template

When the PM dispatches a specialist, use exactly this prompt shape (substitute the phase number and agent path):

```
You are the <Agent name> for the MS Neuro Battery project.

Read these files in order before doing anything else:
1. /home/mustafa/src/MS-Battery/SPEC.md
2. /home/mustafa/src/MS-Battery/agents/<NN-agent-name>.md
3. /home/mustafa/src/MS-Battery/STATUS.md
4. /home/mustafa/src/MS-Battery/CLAUDE.md (specifically the "Inherited rules" and the dispatch template sections)

You are working on Phase <N>. Stay strictly in your role as defined in your agent brief.
Do not recursively dispatch subagents; if you need work done outside your role, stop and report
the need back so the PM can dispatch it.

When you finish, report:
1. Files produced or modified, with absolute paths.
2. Decisions that need PM review.
3. Anything you were uncertain about (do not guess; flag it).
4. Which agent should run next per the handoff chain in your brief.

Hard rules (inherited from the user's global CLAUDE.md):
- No em dashes, en dashes, or hyphens used as punctuation in any prose you write.
- No emojis in any file or message.
- No `Co-Authored-By:` trailers and no AI attribution lines on commits.
- If you are not 100 percent sure about a decision, fact, or identifier, stop and ask.
  Do not guess. Verify before you assert. The user prefers a question to a wrong answer.
```

Every dispatch includes that block verbatim. The block is what makes the role disciplined.

## When to dispatch vs play the role yourself

| Situation | Action |
|-----------|--------|
| Bounded, scoped work with a clear deliverable | Dispatch a specialist |
| Long lived, exploratory development with iterative edits | PM does it directly |
| Cross cutting decision that affects multiple agents | PM does it directly, then briefs agents |
| One line tweak, typo fix, config nudge | PM does it directly |
| Anything requiring a domain skill (signal processing, security, accessibility) | Dispatch the relevant specialist |
| Any architectural change | PM does it directly, then writes an ADR |
| Ambiguity discovered mid task | Stop, ask the user, do not guess |

## How to invoke skills

Skills are invoked via the `Skill` tool. The PM uses skills directly when planning or reviewing. Specialists invoke skills relevant to their role (e.g. UI/UX Designer uses `frontend-design`; the Code Reviewer uses `code-review`; Phase planning uses `superpowers:writing-plans`).

A small set of skills is project relevant:

- `superpowers:brainstorming` (PM, when scope opens up mid project)
- `superpowers:writing-plans` (PM, when starting a new phase that needs a written plan)
- `superpowers:test-driven-development` (any agent writing code)
- `superpowers:requesting-code-review` (any agent before merging)
- `superpowers:verification-before-completion` (any agent before claiming done)
- `superpowers:systematic-debugging` (any agent when something fails)
- `code-review:code-review` (Code Reviewer agent)
- `web-design-guidelines` (UI/UX Designer for any web touch points; not the primary tool here since this is mobile)

## Directory layout

```
~/src/MS-Battery/
├── CLAUDE.md                    (this file)
├── SPEC.md                      (project specification)
├── STATUS.md                    (single source of truth for phase status)
├── GETTING-STARTED.md           (first session walkthrough)
├── README.md                    (public facing pitch; finalised in Phase 0)
├── future-ideas.md              (deferred features)
├── agents/
│   ├── 00-project-manager.md
│   ├── 01-clinical-validator.md
│   ├── 02-signal-processing-engineer.md
│   ├── 03-android-engineer.md
│   ├── 04-ui-ux-designer.md
│   ├── 05-data-engineer.md
│   ├── 06-database-administrator.md
│   ├── 07-security-engineer.md
│   ├── 08-devops-engineer.md
│   ├── 09-qa-engineer.md
│   ├── 10-code-reviewer.md
│   ├── 11-performance-engineer.md
│   ├── 12-accessibility-specialist.md
│   ├── 13-observability-engineer.md
│   ├── 14-documentation-engineer.md
│   ├── 15-clinical-outcomes-reviewer.md
│   ├── 16-citation-auditor.md
│   ├── 17-biostatistics-reviewer.md
│   ├── 18-sensor-integration-engineer.md
│   ├── 19-patient-advocate.md
│   ├── 20-test-fixture-engineer.md
│   └── 21-compliance-reviewer.md
├── docs/
│   ├── plan.md                  (multi phase plan overview)
│   ├── plans/
│   │   └── phase-1-foundation.md (detailed Phase 1 plan, written before Android pivot was decided to be Android only; treat as canonical for Phase 1)
│   ├── source/
│   │   └── clinical-references.md
│   ├── architecture.md          (written by Documentation Engineer in Phase 0)
│   ├── design/
│   │   └── tokens.md            (written by UI/UX Designer in Phase 0)
│   ├── data/
│   │   └── schema.md            (written by Data Engineer in Phase 1)
│   ├── security/
│   │   ├── threat-model.md      (written by Security Engineer in Phase 0)
│   │   └── hardening-checklist.md
│   ├── qa/
│   │   └── regression-checklist.md
│   ├── perf/
│   │   └── latency-budgets.md
│   ├── a11y/
│   │   └── checklist.md
│   ├── observability/
│   │   └── logging-runbook.md
│   └── adr/
│       └── 0001-android-native-platform.md
└── app/                         (Android Studio project; created in Phase 0 by DevOps)
    └── ... (gradle, manifest, sources, tests)
```

The `app/` tree is created by DevOps in Phase 0 using the existing detailed plan in `docs/plans/phase-1-foundation.md` as the guide. Until Phase 0 runs, only the documentation tree exists.

## Visual design (mobile)

The application is mobile, native Android, Material 3. There is no web frontend, no Claude Design round trip, and no `design/` reference folder at the project root.

UI/UX direction lives in `docs/design/tokens.md`. The first version is a Material 3 baseline plus an accessibility token set (large default tap targets, optional high contrast theme, generous typography). UI customization beyond that is captured in `future-ideas.md` and revisited after Phase 11.

## Coding conventions

Inherited from the user's global CLAUDE.md plus a few project specific rules:

- Kotlin code: idiomatic, no Java style getters and setters where Kotlin properties suffice. Compose code: state hoisted out of leaf composables.
- Test discipline: TDD for any signal processing module (gait pipeline, audio features, scoring). Compose UI tests for screens. Robolectric for Room repository tests.
- No merging code with failing tests.
- Every PR runs the Code Reviewer before merge.
- No `INTERNET` permission. Security Engineer has veto on any change that would add it.
- No `Co-Authored-By:` trailers. No "Generated with Claude Code" lines. Commits authored as Mustafan4x alone.

## Test discipline

- **Domain critical math (gait pipeline, audio features, scoring) is TDD.** Write the failing test first against synthetic ground truth, then implement.
- **Compose UI screens** get smoke level Compose UI tests (each screen renders, primary buttons fire).
- **Room DAOs** get Robolectric tests with in memory databases.
- **ViewModels** get tests with fake DAOs and `kotlinx-coroutines-test`.
- **Real device validation** (gait against motion capture or measured walking course) is its own phase (Phase 5).
- **No phase closes with red tests.**

## When you get stuck

Before asking the user, try in this order:

1. Re read the relevant sections of `SPEC.md` and the agent brief.
2. Run `./gradlew :app:testDebugUnitTest` to see if a test gives you a clue.
3. Check `docs/adr/` for prior decisions on the topic.
4. Check `STATUS.md` Resume notes if the previous session left state.
5. Search the project with grep for the symbol or string in question.
6. Check published references (linked from `docs/source/clinical-references.md`) before guessing at clinical or DSP behavior.

If those steps do not produce certainty, **ask the user**. Per the inherited "always ask, never guess" rule, asking is always cheaper than building the wrong thing.

## Quick reference

- "Work on the next phase" -> read `STATUS.md`, run check-in, dispatch.
- "What is the spec" -> `SPEC.md`.
- "What phase are we on" -> `STATUS.md` "Next phase" line.
- "Who does what" -> `agents/00-project-manager.md` and the rest of `agents/`.
- "How is the architecture organized" -> `docs/architecture.md` once written; until then, this file plus `SPEC.md`.
- "Why did we choose X" -> `docs/adr/`.
