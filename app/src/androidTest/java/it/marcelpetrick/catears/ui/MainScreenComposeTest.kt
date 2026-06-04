// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import it.marcelpetrick.catears.domain.RecordingState
import it.marcelpetrick.catears.ui.theme.CatEarsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MainScreenComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── CameraErrorContent ────────────────────────────────────────────────────

    @Test
    fun cameraError_showsRetryButton() {
        composeTestRule.setContent {
            CatEarsTheme { CameraErrorContent(onRetryCamera = {}) }
        }
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun cameraError_retryButton_callsCallback() {
        var called = false
        composeTestRule.setContent {
            CatEarsTheme { CameraErrorContent(onRetryCamera = { called = true }) }
        }
        composeTestRule.onNodeWithText("Retry").performClick()
        assertTrue(called)
    }

    // ── RecordButton ──────────────────────────────────────────────────────────

    @Test
    fun recordButton_idle_showsRecordIcon() {
        composeTestRule.setContent {
            CatEarsTheme {
                RecordButton(
                    recordingState = RecordingState.Idle,
                    onRecordTap = {},
                    onStopRecording = {},
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Record 5s clip").assertIsDisplayed()
    }

    @Test
    fun recordButton_recording_showsStopIcon_and_callsStop() {
        var stopCalled = false
        composeTestRule.setContent {
            CatEarsTheme {
                RecordButton(
                    recordingState = RecordingState.Recording,
                    onRecordTap = {},
                    onStopRecording = { stopCalled = true },
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Recording — tap to stop").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Recording — tap to stop").performClick()
        assertTrue(stopCalled)
    }
}
