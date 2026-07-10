// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

/**
 * User fine-tuning of the ear base point, applied on top of the auto-fitted placement.
 *
 * Both axes are expressed as face-box-relative fractions so the adjustment scales with
 * face size and distance and keeps tracking the face — it shifts the auto-fit, it never
 * replaces it.
 *
 * @param verticalOffset Vertical shift of the ear base line as a fraction of face-box
 *   height. Negative = up, positive = down. Clamped to [±[MAX_VERTICAL_OFFSET]].
 * @param spreadOffset Horizontal spread delta as a fraction of the auto-fit ear
 *   half-spacing. Negative = inward, positive = outward. Clamped to [±[MAX_SPREAD_OFFSET]].
 */
data class EarAdjustment(val verticalOffset: Float = 0f, val spreadOffset: Float = 0f) {
    /** True when this adjustment differs from the auto-fit default. */
    val isCustom: Boolean get() = this != DEFAULT

    /** Returns a copy with both axes clamped into their documented ranges. */
    fun coerced(): EarAdjustment = EarAdjustment(
        verticalOffset = verticalOffset.coerceIn(-MAX_VERTICAL_OFFSET, MAX_VERTICAL_OFFSET),
        spreadOffset = spreadOffset.coerceIn(-MAX_SPREAD_OFFSET, MAX_SPREAD_OFFSET),
    )

    companion object {
        /** Auto-fit only — no user adjustment. */
        val DEFAULT = EarAdjustment()

        /** Maximum vertical shift, fraction of face-box height. */
        const val MAX_VERTICAL_OFFSET = 0.25f

        /** Maximum spread delta, fraction of the auto-fit half-spacing. */
        const val MAX_SPREAD_OFFSET = 0.5f
    }
}
