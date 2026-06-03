# Agent Guidelines

This document defines the rules and conventions that all AI agents must follow when contributing to this project.

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
