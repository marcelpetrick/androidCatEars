// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

data class HelpContent(
    val appPitch: String,
    val authorLine: String,
    val repoLabel: String,
    val feedbackLabel: String,
    val feedbackInvitation: String,
    val featuresTitle: String,
    val features: List<String>,
    val howToUseTitle: String,
    val howToUseSteps: List<String>,
    val closeLabel: String,
)
