// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.marcelpetrick.catears.ui.theme.CatEarsTheme

@Composable
fun MainScreen(
    uiState: MainUiState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(24.dp),
        ) {
            when (uiState) {
                MainUiState.Initialising -> CircularProgressIndicator()

                MainUiState.PermissionRequired -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Camera access needed",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This app needs the camera to show the cat-ear overlay.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onRequestPermission) {
                        Text("Grant permission")
                    }
                }

                MainUiState.PermissionPermanentlyDenied -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Permission required",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Camera permission was denied permanently. Please enable it in app settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(onClick = onOpenSettings) {
                        Text("Open settings")
                    }
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
        MainScreen(uiState = MainUiState.Ready, onRequestPermission = {}, onOpenSettings = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPermissionRequiredPreview() {
    CatEarsTheme {
        MainScreen(uiState = MainUiState.PermissionRequired, onRequestPermission = {}, onOpenSettings = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPermissionDeniedPreview() {
    CatEarsTheme {
        MainScreen(uiState = MainUiState.PermissionPermanentlyDenied, onRequestPermission = {}, onOpenSettings = {})
    }
}
