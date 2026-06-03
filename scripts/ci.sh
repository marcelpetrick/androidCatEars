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

STEP_NAMES=()
STEP_STATUSES=()
STEP_DURATIONS=()

format_duration() {
  local seconds="$1"
  printf '%02d:%02d' $((seconds / 60)) $((seconds % 60))
}

print_summary() {
  local total=0
  local overall="PASSED"

  for i in "${!STEP_NAMES[@]}"; do
    total=$((total + STEP_DURATIONS[i]))
    if [[ "${STEP_STATUSES[i]}" != "PASSED" ]]; then
      overall="FAILED"
    fi
  done

  echo ""
  echo "+----+----------------------------------+----------+----------+"
  echo "| #  | Step                             | Status   | Wall     |"
  echo "+----+----------------------------------+----------+----------+"
  for i in "${!STEP_NAMES[@]}"; do
    printf '| %-2d | %-32s | %-8s | %-8s |\n' \
      "$((i + 1))" \
      "${STEP_NAMES[i]}" \
      "${STEP_STATUSES[i]}" \
      "$(format_duration "${STEP_DURATIONS[i]}")"
  done
  echo "+----+----------------------------------+----------+----------+"
  printf '| %-37s | %-8s | %-8s |\n' "Total" "$overall" "$(format_duration "$total")"
  echo "+---------------------------------------+----------+----------+"
}

run_step() {
  local step_name="$1"
  shift
  local started
  local finished
  local duration

  echo "==> ${step_name}"
  started="$(date +%s)"
  if "$@"; then
    finished="$(date +%s)"
    duration=$((finished - started))
    STEP_NAMES+=("$step_name")
    STEP_STATUSES+=("PASSED")
    STEP_DURATIONS+=("$duration")
  else
    local status=$?
    finished="$(date +%s)"
    duration=$((finished - started))
    STEP_NAMES+=("$step_name")
    STEP_STATUSES+=("FAILED")
    STEP_DURATIONS+=("$duration")
    print_summary
    echo ""
    echo "FAILED: ${step_name}"
    echo "Command: $*"
    exit "$status"
  fi
}

run_step "Build (debug)" ./gradlew assembleDebug

run_step "Format check (Spotless)" ./gradlew spotlessCheck

run_step "Static analysis (detekt)" ./gradlew detekt

run_step "Android Lint" ./gradlew :app:lint

run_step "Unit tests" ./gradlew :app:test :domain:test

run_step "Coverage gate (Kover >= 95%)" ./gradlew :domain:koverVerify

print_summary
echo "All checks passed."
