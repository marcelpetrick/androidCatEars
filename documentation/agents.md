# Agent Guidelines

This document defines the rules and conventions that all AI agents must follow when contributing to this project.

---

## Work Format

Work is organised as **atomic, focused tasks**, not large work packages.

- The actionable backlog lives in [`BACKLOG.md`](BACKLOG.md), derived from the strategic
  [`DEVELOPMENT_PLAN.md`](DEVELOPMENT_PLAN.md). The plan says *what and why*; the backlog says
  *the next concrete thing to do*.
- **One task = one focused aspect = one atomic commit** (or a small, tightly related set). A task
  never mixes unrelated concerns.
- Work tasks **in order**. Pick the next `TODO`, mark it `IN PROGRESS`, and mark it `DONE` only when
  the global Definition of Done is met. Do not start a task before its predecessors are done.
- A task states its **goal and acceptance criteria, not a step-by-step recipe.** Agents have freedom
  to choose the implementation, libraries-of-detail, and structure within the rules of this document
  and the project's binding decisions.
- If a task turns out to be **too large to stay atomic**, split it into sub-tasks in `BACKLOG.md`
  *before* starting, rather than producing a sprawling commit.
- When in doubt about scope, behaviour, or an architectural choice not already decided, **stop and
  ask the user** with 2–3 concrete proposals and a recommendation.

> This work format is provisional and will be refined as we learn what works in practice.

---

## Execution Loop

This is the autonomous workflow. When the loop is started, repeat the following until told to stop,
a task needs a user decision, or the backlog has no `TODO` tasks left:

1. **Select** the next `TODO` task in [`BACKLOG.md`](BACKLOG.md), respecting order (top to bottom).
   Mark it `IN PROGRESS`.
2. **Implement** it until the global **Definition of Done** is fully met:
   build passes, tests pass, coverage stays ≥ 95% on its scope, linters are clean, docs are updated,
   and the patch version is bumped.
3. **On success:** mark the task `DONE` and make an **atomic, conventional, signed-off** commit
   locally (no AI attribution, **never push**). Then go to step 1 for the next task.
4. **If the task cannot be fully completed in one go:**
   - Implement as much as can be done **while keeping the repository green** (buildable, tests/lint
     passing). Never leave the repo broken between commits.
   - **Add one or more follow-up tasks** to `BACKLOG.md` capturing the remaining work, each with a
     clear goal and acceptance criteria, placed where it belongs in the order.
   - Include that backlog update **in the same commit**, so another agent can pick the follow-up up
     later without any lost context.
   - Mark progress honestly (the original task stays `IN PROGRESS` or becomes `DONE` only if its own
     acceptance criteria are met; otherwise the remainder lives in the new follow-up task).
5. **Repeat.**

**Stop and ask the user** when: a task is marked `ASK`, an architectural/product decision not already
made is required, or something contradicts the documented plan. Present 2–3 concrete proposals with a
recommendation rather than guessing.

**Loop invariants (never violate):**
- Every commit leaves the repository **green** — it builds and passes the quality gate.
- Commits are **local only**; the user decides when to push.
- All work stays **inside the project directory** (see "Local Environment & Tooling").
- The backlog is always an accurate reflection of remaining work.

### Periodic review (every 10 commits)

After roughly every **10 commits**, pause and do a short review of the current state vs. the plan:

1. Compare the **IS** (what the code currently does) against [`DEVELOPMENT_PLAN.md`](DEVELOPMENT_PLAN.md)
   and [`VISION.md`](VISION.md).
2. Note anything missing, risky, deferred, or worth doing that is not already a backlog task.
3. Record findings in [`TODO.md`](documentation/TODO.md) as a **simple enumerated list of one-liners** (no prose).
   Keep it current: remove items once they become backlog tasks or are done.
4. Commit the review as its own atomic `docs(todo): …` commit.

This keeps a lightweight, always-visible radar of follow-ups separate from the detailed backlog.

---

## Git Identity

All commits must use the following identity:

- **Author:** Marcel Petrick
- **Email:** mail@marcelpetrick.it
- All commits must carry a `Signed-off-by` trailer (`git commit -s` / `--signoff`).

---

## Commit Messages

- **Never** reference AI tools, models, assistants, harnesses, or code generators (e.g. Claude, Codex, Copilot, ChatGPT) in commit messages or PR descriptions.
- All commit messages must follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

  ```
  <type>(<scope>): <short summary>

  [optional body with details]

  [optional footer(s)]
  ```

  Common types: `feat`, `fix`, `build`, `chore`, `ci`, `docs`, `refactor`, `test`, `perf`, `style`.

- The summary line must be concise and imperative (e.g. `fix(auth): handle null token on logout`).
- The body should explain *what* changed and *why*, not *how* — add meaningful detail for non-trivial changes.
- Commits must be **atomic**: each commit represents one logical, self-contained change. For multi-step work, commit at each meaningful checkpoint rather than bundling everything into one commit at the end.

---

## Versioning

This project uses [Semantic Versioning](https://semver.org/) (`MAJOR.MINOR.PATCH`).

- **Patch** — increment by 1 for every new commit (bug fixes, small changes).
- **Minor** — increment when new functionality is added in a backwards-compatible manner; reset patch to 0.
- **Major** — increment for breaking changes; reset minor and patch to 0.

There is a **single source of truth** for the version (e.g. `version.properties` or equivalent — to be defined when the project is initialised). All tooling must read from that single location; never hardcode version strings elsewhere.

Every commit that goes to the main branch must include a corresponding version bump.

---

## CI / CD Pipeline

When a local or remote CI pipeline is in place, every commit pushed to a tracked branch must pass the full pipeline before being considered done. This includes:

- **Tests** — all unit and integration tests must pass with zero failures.
- **Coverage** — code coverage must be at or above **95%** (measured by whatever tool the project uses, e.g. JaCoCo for Android).
- **Linting** — all configured linters (e.g. ktlint, detekt, Android Lint) must report zero errors and zero warnings that are not explicitly suppressed with justification.
- **Build** — a clean debug (and, where applicable, release) build must succeed without errors.

A pipeline failure blocks the commit from being treated as complete. Fix the failure before moving on.

---

## Documentation

- The project root must contain a `README.md` that covers:
  - What the project is and what problem it solves.
  - Prerequisites and environment setup.
  - Build instructions (debug and release).
  - How to run the app (emulator, device, any required flags).
  - How to deploy / release (signing, distribution channel, versioning step).
- Keep `README.md` up to date with every change that affects build, run, or deploy steps.
- Additional design documents, ADRs, and reference material live under `documentation/`.

---

## Local Environment & Tooling

Agents may install tools, SDKs, and dependencies needed to build, test, and lint the project — but
under strict rules so the environment stays reproducible and traceable.

- **Stay inside the project directory.** All work and side effects must happen within this repo's
  directory tree. Prefer project-local installs (Gradle-managed dependencies, the Gradle wrapper,
  project-local SDK/tool paths) over global or system-wide changes. Do not modify the user's system
  outside this directory unless it is genuinely unavoidable — and if so, flag it explicitly first.
- **Prefer reproducible, pinned versions.** Use the Gradle wrapper and the version catalog
  (`libs.versions.toml`) so versions are declared in-repo, rather than relying on ad-hoc global tools.
- **Log every installation.** Whenever a tool or SDK is installed (or a system-level change is made),
  record it in `documentation/TOOLING.md`: the tool name, version, purpose, how it was installed,
  whether it is project-local or system-wide, and the date. This keeps the setup traceable and
  reproducible by the next agent or a human.
- If a required tool is already present, note its version in `documentation/TOOLING.md` rather than
  reinstalling.

---

## Repository Hygiene (`.gitignore`)

Commit only what is **necessary and not regenerable**. Anything a tool can regenerate must not be
stored in git.

- **Do commit:** source code, configuration, the Gradle wrapper, the version catalog, documentation,
  and committed assets (e.g. the placeholder overlay artwork).
- **Never commit (ignore instead):** build outputs (`build/`, `.gradle/`), IDE files (`.idea/`,
  `*.iml`), local environment config (`local.properties`), keystores/secrets, caches, and
  test/coverage report outputs.
- **Groom `.gitignore` continuously.** As the project grows, if a generated or regenerable artifact
  shows up in `git status`, add it to `.gitignore` instead of committing it. The goal is a clean repo
  that contains only meaningful, hand-authored or source-of-truth files.
- The baseline `.gitignore` is established in backlog task 0.0 and maintained from then on.
