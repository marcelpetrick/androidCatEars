// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.marcelpetrick.catears.domain.EarAdjustment
import it.marcelpetrick.catears.ui.theme.CatEarsTheme
import kotlin.math.roundToInt

/**
 * Live ear-fit tuning panel shown over the running camera preview.
 *
 * Two sliders shift the ear base point relative to the auto-fit: height (up/down) and
 * width (inward/outward). Changes apply to the ears immediately via [onAdjustmentChanged];
 * releasing a slider persists via [onAdjustmentCommitted].
 */
@Composable
fun EarFitPanel(
    adjustment: EarAdjustment,
    onAdjustmentChanged: (EarAdjustment) -> Unit,
    onAdjustmentCommitted: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = PANEL_BG_ALPHA),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 3.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Ear fit",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = readout(adjustment),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close ear fit panel")
                }
            }
            FitSlider(
                label = "Height",
                lowLabel = "Lower",
                highLabel = "Higher",
                // Slider right = ears higher = negative vertical offset.
                value = -adjustment.verticalOffset,
                maxValue = EarAdjustment.MAX_VERTICAL_OFFSET,
                onValue = { onAdjustmentChanged(adjustment.copy(verticalOffset = -it)) },
                onCommit = onAdjustmentCommitted,
            )
            FitSlider(
                label = "Width",
                lowLabel = "Narrower",
                highLabel = "Wider",
                value = adjustment.spreadOffset,
                maxValue = EarAdjustment.MAX_SPREAD_OFFSET,
                onValue = { onAdjustmentChanged(adjustment.copy(spreadOffset = it)) },
                onCommit = onAdjustmentCommitted,
            )
            TextButton(
                onClick = onReset,
                enabled = adjustment.isCustom,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Reset to auto")
            }
        }
    }
}

@Composable
private fun FitSlider(
    label: String,
    lowLabel: String,
    highLabel: String,
    value: Float,
    maxValue: Float,
    onValue: (Float) -> Unit,
    onCommit: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = lowLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = value,
            onValueChange = onValue,
            onValueChangeFinished = onCommit,
            valueRange = -maxValue..maxValue,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .semantics { contentDescription = "$label: $lowLabel to $highLabel" },
        )
        Text(
            text = highLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** e.g. "Height +8 % · Width −12 %"; doubles as the accessible state description. */
private fun readout(adjustment: EarAdjustment): String {
    val height = (-adjustment.verticalOffset * PERCENT).roundToInt()
    val width = (adjustment.spreadOffset * PERCENT).roundToInt()
    return "Height ${signed(height)} % · Width ${signed(width)} %"
}

private fun signed(value: Int): String = if (value >= 0) "+$value" else "$value"

private const val PANEL_BG_ALPHA = 0.92f
private const val PERCENT = 100f

@Preview(showBackground = true)
@Composable
private fun EarFitPanelPreview() {
    CatEarsTheme {
        Column {
            EarFitPanel(
                adjustment = EarAdjustment(verticalOffset = -0.08f, spreadOffset = 0.12f),
                onAdjustmentChanged = {},
                onAdjustmentCommitted = {},
                onReset = {},
                onClose = {},
            )
            Spacer(modifier = Modifier.height(8.dp))
            EarFitPanel(
                adjustment = EarAdjustment.DEFAULT,
                onAdjustmentChanged = {},
                onAdjustmentCommitted = {},
                onReset = {},
                onClose = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
