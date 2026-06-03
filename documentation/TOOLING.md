# Tooling Log

A traceable record of every tool, SDK, or dependency installed (or system change made) while working
on this project. See [`agents.md`](agents.md) "Local Environment & Tooling" for the rules.

Append one row per install. Prefer **project-local** scope; flag anything system-wide.

| Date | Tool | Version | Purpose | How installed | Scope (project / system) |
|------|------|---------|---------|---------------|--------------------------|
| 2026-06-03 | OpenJDK | 26.0.1 | JVM for running Gradle and building the project | Pre-installed on host (Manjaro) | system |
| 2026-06-03 | Gradle | 9.5.1 | Build system; bootstrapped via `gradle wrapper`; self-managed by wrapper at `~/.gradle/wrapper/dists` | Bootstrapped via temporary download of `gradle-9.5.1-bin.zip` to `/tmp` (deleted after wrapper generation); subsequent runs use `./gradlew` self-download | project (wrapper in repo, distribution cached in `~/.gradle`) |
