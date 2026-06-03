// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

/**
 * Generates a deterministic, collision-resistant filename for a captured photo.
 *
 * Format: `CatEars_<timestamp>_<random4>.jpg`
 * Example: `CatEars_20261203_143022_a3f1.jpg`
 *
 * Pure function — no Android dependencies, fully unit-testable.
 *
 * @param epochMillis Milliseconds since Unix epoch (from the capture moment).
 * @param randomSuffix A 4-character hex suffix to avoid collisions on rapid capture.
 */
fun buildImageFileName(epochMillis: Long, randomSuffix: String): String {
    val seconds = epochMillis / MILLIS_PER_SECOND
    val date = formatDate(seconds)
    val time = formatTime(seconds)
    val safe = randomSuffix.take(SUFFIX_LENGTH).padEnd(SUFFIX_LENGTH, '0')
    return "CatEars_${date}_${time}_$safe.jpg"
}

private fun formatDate(epochSeconds: Long): String {
    val days = epochSeconds / SECONDS_PER_DAY
    // Days since 1970-01-01; simplified calendar (ignores DST — good enough for filenames)
    val year = EPOCH_YEAR + (days / DAYS_PER_APPROX_YEAR).toInt()
    val dayOfYear = (days % DAYS_PER_APPROX_YEAR).toInt()
    val month = (dayOfYear / DAYS_PER_APPROX_MONTH).toInt() + 1
    val day = (dayOfYear % DAYS_PER_APPROX_MONTH).toInt() + 1
    return "%04d%02d%02d".format(year, month.coerceIn(MIN_MONTH, MAX_MONTH), day.coerceIn(MIN_DAY, MAX_DAY))
}

private fun formatTime(epochSeconds: Long): String {
    val daySeconds = epochSeconds % SECONDS_PER_DAY
    val hour = daySeconds / SECONDS_PER_HOUR
    val minute = (daySeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val second = daySeconds % SECONDS_PER_MINUTE
    return "%02d%02d%02d".format(hour, minute, second)
}

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
private const val SECONDS_PER_DAY = 86_400L
private const val DAYS_PER_APPROX_YEAR = 365L
private const val DAYS_PER_APPROX_MONTH = 30L
private const val SUFFIX_LENGTH = 4
private const val EPOCH_YEAR = 1970
private const val MIN_MONTH = 1
private const val MAX_MONTH = 12
private const val MIN_DAY = 1
private const val MAX_DAY = 31
