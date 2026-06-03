// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import it.marcelpetrick.catears.ui.theme.CatEarsTheme

@Composable
fun MainScreen(uiState: MainUiState, onPermissionResult: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (uiState) {
                MainUiState.Initialising -> CircularProgressIndicator()

                MainUiState.PermissionRequired -> androidx.compose.material3.Button(
                    onClick = { onPermissionResult(true) },
                ) {
                    Text(text = "Grant camera permission")
                }

                MainUiState.Ready -> Text(
                    text = "Cat Ears — coming soon",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenReadyPreview() {
    CatEarsTheme {
        MainScreen(uiState = MainUiState.Ready, onPermissionResult = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPermissionPreview() {
    CatEarsTheme {
        MainScreen(uiState = MainUiState.PermissionRequired, onPermissionResult = {})
    }
}
