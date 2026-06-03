#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
# SPDX-License-Identifier: GPL-3.0-or-later
#
# Generates the aggregate CycloneDX SBOM and copies versioned JSON/XML files
# plus SHA-256 checksums into the requested output directory.
#
# Usage:
#   ./scripts/generate-sbom.sh [output-dir]

set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

OUT_DIR="${1:-build/reports/cyclonedx-release}"

version_part() {
  local key="$1"
  grep "^${key}=" version.properties | cut -d= -f2
}

VERSION="$(version_part major).$(version_part minor).$(version_part patch)"
BASE_NAME="androidCatEars-${VERSION}"

./gradlew cyclonedxBom

test -s build/reports/cyclonedx/bom.json
test -s build/reports/cyclonedx/bom.xml

mkdir -p "$OUT_DIR"
cp build/reports/cyclonedx/bom.json "${OUT_DIR}/${BASE_NAME}.cdx.json"
cp build/reports/cyclonedx/bom.xml "${OUT_DIR}/${BASE_NAME}.cdx.xml"

(
  cd "$OUT_DIR"
  sha256sum "${BASE_NAME}.cdx.json" > "${BASE_NAME}.cdx.json.sha256"
  sha256sum "${BASE_NAME}.cdx.xml" > "${BASE_NAME}.cdx.xml.sha256"
)

echo "Generated SBOM artifacts in ${OUT_DIR}:"
echo "  ${BASE_NAME}.cdx.json"
echo "  ${BASE_NAME}.cdx.xml"
echo "  ${BASE_NAME}.cdx.json.sha256"
echo "  ${BASE_NAME}.cdx.xml.sha256"
