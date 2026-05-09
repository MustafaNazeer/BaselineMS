# Getting started, BaselineMS

This is the first session walkthrough. If this is the very first session opened in this directory, follow these steps in order. After Phase 0 is complete, future sessions just read `STATUS.md` and run the "work on the next phase" flow.

## Before you start

Check that:

- `~/src/BaselineMS/SPEC.md` exists.
- `~/src/BaselineMS/STATUS.md` exists and "Next phase" reads `Phase 0: Bootstrap setup`.
- The user has confirmed the GitHub remote at https://github.com/MustafaNazeer/BaselineMS is reachable.
- Android Studio Iguana or later is installed on the machine where the build will actually happen. The session itself can run on Linux without Android Studio; the build steps in Phase 0 onward require Android Studio.

If any of those is missing, stop and tell the user.

## Step 1, orient

Read these files in this order:

1. `CLAUDE.md` (you may already have it auto loaded; re read the "Mandatory check-in protocol" section anyway).
2. `SPEC.md`.
3. `STATUS.md`.
4. `docs/plan.md`.
5. `agents/00-project-manager.md`.

You are the Project Manager unless the user tells you otherwise.

## Step 2, confirm with the user

Briefly state:

> "I'm the PM for BaselineMS. The next phase is Phase 0, Bootstrap setup. Before I start it, I need to run the check-in protocol."

Then ask the three check-in questions verbatim:

1. What is your current Max plan usage percentage?
2. How long until your usage window resets?
3. Should I continue, pause until reset, or stop here?

Wait for answers. Do not proceed without them.

## Step 3, plan the phase

If the user says continue, briefly review `docs/plan.md` Phase 0 section with the user. Confirm the agents you intend to dispatch:

- DevOps Engineer: scaffold the Android Studio project at `~/src/BaselineMS/app/`, wire git, push initial commit to the remote.
- Security Engineer: write `docs/security/threat-model.md` and `docs/security/hardening-checklist.md`.
- UI/UX Designer: write `docs/design/tokens.md` with Material 3 baseline plus accessibility tokens.
- Documentation Engineer: write `docs/architecture.md` and finalize `README.md`.
- Clinical Validator: review the test designs in `SPEC.md` Section 6 against current clinical literature; flag any concerns in `docs/source/clinical-references.md`.
- Citation Auditor: audit every entry in `docs/source/clinical-references.md` and every cited number in `SPEC.md` against the source papers; results in `docs/source/citation-audit-log.md`.
- Compliance Reviewer: review `SPEC.md` Section 4 (non goals), Section 10 (privacy), and `README.md` positioning against FDA wellness guidance, HIPAA scope, and Google Play health app policy; results in `docs/security/compliance-review.md`.
- Patient Advocate: write initial framing notes on the patient population in `docs/qa/patient-advocate-reviews.md`; coordinate with Clinical Validator on relevant patient reported outcome studies.

Ask the user if any agent should be deferred to a later phase.

## Step 4, dispatch Phase 0 agents

Use the Task tool dispatch template from `CLAUDE.md` verbatim. A concrete example for DevOps:

```
Task tool prompt:
You are the DevOps Engineer for the BaselineMS project.

Read these files in order before doing anything else:
1. /home/mustafa/src/BaselineMS/SPEC.md
2. /home/mustafa/src/BaselineMS/agents/08-devops-engineer.md
3. /home/mustafa/src/BaselineMS/STATUS.md
4. /home/mustafa/src/BaselineMS/CLAUDE.md (specifically the "Inherited rules" and the dispatch template sections)

You are working on Phase 0. Stay strictly in your role as defined in your agent brief.
Do not recursively dispatch subagents; if you need work done outside your role, stop and report
the need back so the PM can dispatch it.

Your Phase 0 deliverables:
1. Initialize the Android Studio project at /home/mustafa/src/BaselineMS/app/ following Task 1
   of /home/mustafa/src/BaselineMS/docs/plans/phase-1-foundation.md.
2. Wire git: `git init` at the project root, `git remote add origin
   https://github.com/MustafaNazeer/BaselineMS.git`, push the initial commit.
3. Configure GitHub Actions on Linux runners to run `./gradlew :app:testDebugUnitTest` on PRs.

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

Other Phase 0 agents follow the same shape.

## Step 5, review handoffs

As each agent reports back, review their outputs:

- Did they produce the deliverables listed in their brief?
- Did they flag anything for PM review?
- Did they flag uncertainty rather than guessing?

If anything is missing or uncertain, dispatch a follow up or ask the user. Do not paper over gaps.

## Step 6, gate plus check-in plus update STATUS.md

When all Phase 0 agents have reported back and their work is reviewed:

1. Run the QA Engineer over the Phase 0 deliverables (the threat model exists, the architecture stub exists, the project builds, the GitHub remote works).
2. Run the Code Reviewer if any code was produced.
3. Update `STATUS.md`: flip Phase 0 to `completed`, fill in the date and window cost, update "Next phase" to `Phase 1: Foundation`.
4. Run the mandatory check-in protocol again before starting Phase 1.

Phase 0 is closed only after STATUS.md is updated.

## Step 7, propose Phase 1

Once Phase 0 is closed and the user has answered the check-in for Phase 1, brief the agents Phase 1 needs (Android Engineer, Data Engineer, Database Administrator, QA Engineer, Code Reviewer, Documentation Engineer). Phase 1 follows the detailed plan at `docs/plans/phase-1-foundation.md` step by step, with the Android Studio project already created in Phase 0.

## What to do on the second session and beyond

Open `STATUS.md`. The "Next phase" line tells you what to work on. Read the relevant agent briefs, run the check-in protocol, and proceed.

If you ever feel uncertain about state, default to reading these files in order: `STATUS.md`, `SPEC.md`, `docs/plan.md`, `CLAUDE.md`, the agent brief for the role you are about to play. If still uncertain, ask the user.
