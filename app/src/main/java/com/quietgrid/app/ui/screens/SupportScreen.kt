package com.quietgrid.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.ISSUES_URL
import com.quietgrid.app.core.PLAY_STORE_APP_URL
import com.quietgrid.app.core.PLAY_STORE_WEB_URL
import com.quietgrid.app.core.REPO_URL
import com.quietgrid.app.core.SUPPORT_EMAIL
import com.quietgrid.app.core.buildBugReportUrl
import com.quietgrid.app.core.buildFeatureRequestUrl

@Composable
fun SupportScreen(onOpenInfo: (String) -> Unit) {
    val context = LocalContext.current
    fun openUrl(url: String): Boolean =
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.isSuccess
    fun openRateApp() {
        if (!openUrl(PLAY_STORE_APP_URL)) openUrl(PLAY_STORE_WEB_URL)
    }

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SupportSection(title = stringResource(R.string.support_support_section)) {
            SupportRow(stringResource(R.string.support_report_bug), stringResource(R.string.support_opens_github_issues), external = true) { openUrl(buildBugReportUrl()) }
            SupportRow(stringResource(R.string.support_request_feature), stringResource(R.string.support_opens_github_issues), external = true) { openUrl(buildFeatureRequestUrl()) }
            SupportRow(stringResource(R.string.support_contact), SUPPORT_EMAIL, external = true) { openUrl("mailto:$SUPPORT_EMAIL") }
        }

        SupportSection(title = stringResource(R.string.support_trust_section)) {
            SupportRow(stringResource(R.string.support_privacy), "") { onOpenInfo("privacy") }
            SupportRow(stringResource(R.string.support_source_code), stringResource(R.string.support_opens_github), external = true) { openUrl(REPO_URL) }
            SupportRow(stringResource(R.string.support_licenses), "") { onOpenInfo("licenses") }
        }

        SupportSection(title = stringResource(R.string.support_about_section)) {
            SupportRow(stringResource(R.string.support_about_quiet_grid), "") { onOpenInfo("about") }
            SupportRow(stringResource(R.string.support_rate_quiet_grid), stringResource(R.string.support_opens_play_store), external = true) { openRateApp() }
        }
    }
}

@Composable
private fun SupportSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
        HorizontalDivider()
        Column(content = content)
    }
}

@Composable
private fun SupportRow(label: String, detail: String, external: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 2.dp))
            if (detail.isNotEmpty()) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            imageVector = if (external) Icons.AutoMirrored.Filled.OpenInNew else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
