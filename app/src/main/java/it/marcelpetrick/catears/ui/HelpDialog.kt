// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import it.marcelpetrick.catears.domain.FEEDBACK_URL
import it.marcelpetrick.catears.domain.HelpContent
import it.marcelpetrick.catears.domain.HelpLanguage
import it.marcelpetrick.catears.domain.REPO_URL
import it.marcelpetrick.catears.domain.helpContent

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    var selectedLanguage by remember { mutableStateOf(HelpLanguage.ENGLISH) }
    val content = helpContent(selectedLanguage)
    val uriHandler = LocalUriHandler.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("AndroidCatEars", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(12.dp))
                LanguageSelector(selectedLanguage) { selectedLanguage = it }
                Spacer(modifier = Modifier.height(16.dp))
                Text(content.appPitch, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(content.authorLine, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))
                HelpDialogLinks(content, uriHandler)
                Spacer(modifier = Modifier.height(16.dp))
                BulletSection(content.featuresTitle, content.features)
                Spacer(modifier = Modifier.height(16.dp))
                NumberedSection(content.howToUseTitle, content.howToUseSteps)
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(content.closeLabel)
                }
            }
        }
    }
}

@Composable
private fun LanguageSelector(selectedLanguage: HelpLanguage, onLanguageChange: (HelpLanguage) -> Unit) {
    val options = HelpLanguage.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, lang ->
            SegmentedButton(
                selected = lang == selectedLanguage,
                onClick = { onLanguageChange(lang) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(lang.toDisplayLabel()) },
            )
        }
    }
}

@Composable
private fun HelpDialogLinks(content: HelpContent, uriHandler: UriHandler) {
    ClickableLink(content.repoLabel, REPO_URL, uriHandler)
    Spacer(modifier = Modifier.height(6.dp))
    ClickableLink(content.feedbackLabel, FEEDBACK_URL, uriHandler)
    Spacer(modifier = Modifier.height(6.dp))
    Text(content.feedbackInvitation, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ClickableLink(label: String, url: String, uriHandler: UriHandler) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
        modifier = Modifier
            .clickable { uriHandler.openUri(url) }
            .padding(vertical = 2.dp),
    )
}

@Composable
private fun BulletSection(title: String, items: List<String>) {
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(4.dp))
    items.forEach { item ->
        Text("• $item", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NumberedSection(title: String, items: List<String>) {
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(4.dp))
    items.forEachIndexed { index, item ->
        Text("${index + 1}. $item", style = MaterialTheme.typography.bodySmall)
    }
}

private fun HelpLanguage.toDisplayLabel() = when (this) {
    HelpLanguage.ENGLISH -> "EN"
    HelpLanguage.GERMAN -> "DE"
    HelpLanguage.MANDARIN -> "中文"
}
