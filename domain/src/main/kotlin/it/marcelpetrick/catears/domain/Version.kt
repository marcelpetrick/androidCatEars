// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

data class Version(val major: Int, val minor: Int, val patch: Int) {
    override fun toString(): String = "$major.$minor.$patch"

    fun isNewerThan(other: Version): Boolean = major > other.major ||
        (major == other.major && minor > other.minor) ||
        (major == other.major && minor == other.minor && patch > other.patch)

    companion object {
        private const val EXPECTED_PARTS = 3

        fun parse(versionString: String): Version {
            val parts = versionString.split(".")
            require(parts.size == EXPECTED_PARTS) {
                "Version string must have exactly $EXPECTED_PARTS parts: $versionString"
            }
            return Version(
                major = parts[0].toInt(),
                minor = parts[1].toInt(),
                patch = parts[2].toInt(),
            )
        }
    }
}
