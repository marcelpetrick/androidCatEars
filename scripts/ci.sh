#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
# SPDX-License-Identifier: GPL-3.0-or-later
#
# Local CI gate — mirrors .github/workflows/ci.yml exactly.
# Run before every push to catch issues before CI does.
#
# Usage: ./scripts/ci.sh

set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

echo "==> [1/6] Build (debug)"
./gradlew assembleDebug

echo "==> [2/6] Format check (Spotless)"
./gradlew spotlessCheck

echo "==> [3/6] Static analysis (detekt)"
./gradlew detekt

echo "==> [4/6] Android Lint"
./gradlew :app:lint

echo "==> [5/6] Unit tests"
./gradlew :app:test

echo "==> [6/6] Coverage gate (Kover >= 95%)"
./gradlew :app:koverVerify

echo ""
echo "All checks passed."
