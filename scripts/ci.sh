#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
# SPDX-License-Identifier: GPL-3.0-or-later
#
# Local CI gate — mirrors .github/workflows/ci.yml exactly.
# Run before every push to catch issues before CI does.
#
# Usage: ./scripts/ci.sh

set -Eeuo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

current_step=""
trap 'status=$?; echo ""; echo "FAILED: ${current_step:-unknown step}"; echo "Command: ${BASH_COMMAND}"; exit "$status"' ERR

run_step() {
  current_step="$1"
  shift
  echo "==> ${current_step}"
  "$@"
}

run_step "[1/6] Build (debug)" ./gradlew assembleDebug

run_step "[2/6] Format check (Spotless)" ./gradlew spotlessCheck

run_step "[3/6] Static analysis (detekt)" ./gradlew detekt

run_step "[4/6] Android Lint" ./gradlew :app:lint

run_step "[5/6] Unit tests" ./gradlew :app:test

run_step "[6/6] Coverage gate (Kover >= 95%)" ./gradlew :app:koverVerify

echo ""
echo "All checks passed."
