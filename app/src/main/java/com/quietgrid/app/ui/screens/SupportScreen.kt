package com.quietgrid.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.PLAY_STORE_APP_URL
import com.quietgrid.app.core.PLAY_STORE_WEB_URL
import com.quietgrid.app.core.REPO_URL
import com.quietgrid.app.core.SUPPORT_EMAIL
import com.quietgrid.app.core.buildBugReportUrl
import com.quietgrid.app.core.buildFeatureRequestUrl

private val SUPPORT_ICON_COLOR = Color(0xFF60A5FA)
private val TRUST_ICON_COLOR = Color(0xFF34D399)
private val ABOUT_ICON_COLOR = Color(0xFFF472B6)

@Composable
fun SupportPageScreen() {
    val context = LocalContext.current
    val appVersion = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "unknown"
    }
    fun openUrl(url: String): Boolean =
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.isSuccess

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SupportRow(Icons.Filled.BugReport, SUPPORT_ICON_COLOR, stringResource(R.string.support_report_bug), stringResource(R.string.support_opens_github_issues), external = true) { openUrl(buildBugReportUrl(appVersion)) }
        SupportRow(Icons.Filled.Lightbulb, SUPPORT_ICON_COLOR, stringResource(R.string.support_request_feature), stringResource(R.string.support_opens_github_issues), external = true) { openUrl(buildFeatureRequestUrl()) }
        SupportRow(Icons.Filled.Mail, SUPPORT_ICON_COLOR, stringResource(R.string.support_contact), SUPPORT_EMAIL, external = true) { openUrl("mailto:$SUPPORT_EMAIL") }
        StaticSupportRow(Icons.Filled.Tag, SUPPORT_ICON_COLOR, stringResource(R.string.support_version), appVersion)
    }
}

@Composable
fun TrustPageScreen(onOpenInfo: (String) -> Unit) {
    val context = LocalContext.current
    fun openUrl(url: String): Boolean =
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.isSuccess

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SupportRow(Icons.Filled.PrivacyTip, TRUST_ICON_COLOR, stringResource(R.string.support_privacy), "") { onOpenInfo("privacy") }
        SupportRow(painterRes = R.drawable.ic_github, iconTint = TRUST_ICON_COLOR, label = stringResource(R.string.support_source_code), detail = stringResource(R.string.support_opens_github), external = true) { openUrl(REPO_URL) }
        SupportRow(Icons.Filled.Gavel, TRUST_ICON_COLOR, stringResource(R.string.support_licenses), "") { onOpenInfo("licenses") }
    }
}

@Composable
fun AboutPageScreen(onOpenInfo: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SupportRow(Icons.Filled.Info, ABOUT_ICON_COLOR, stringResource(R.string.support_about_quiet_grid), "") { onOpenInfo("about") }
        SupportRow(Icons.Filled.People, ABOUT_ICON_COLOR, stringResource(R.string.support_contributors), "") { onOpenInfo("contributors") }
    }
}

@Composable
internal fun ShowYourSupportSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    fun openUrl(url: String): Boolean =
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.isSuccess
    fun openRateApp() {
        if (!openUrl(PLAY_STORE_APP_URL)) openUrl(PLAY_STORE_WEB_URL)
    }
    val onRate = { openRateApp() }
    val onStar = { openUrl(REPO_URL); Unit }

    Column(modifier) {
        Text(stringResource(R.string.support_show_your_support), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.support_show_your_support_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp, top = 2.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SupportCallToActionButton(
                label = stringResource(R.string.support_rate_on_play_store),
                icon = Icons.Filled.PlayArrow,
                modifier = Modifier.weight(1f),
                onClick = onRate,
            )
            SupportCallToActionButton(
                label = stringResource(R.string.support_star_on_github),
                iconRes = R.drawable.ic_github,
                modifier = Modifier.weight(1f),
                onClick = onStar,
            )
        }
    }
}

@Composable
private fun SupportCallToActionButton(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconRes: Int? = null,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color = MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null)
        } else if (iconRes != null) {
            Icon(painter = painterResource(iconRes), contentDescription = null)
        }
        Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SupportRowIcon(icon: ImageVector?, painterRes: Int?, tint: Color) {
    if (icon == null && painterRes == null) return
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        when {
            icon != null -> Icon(icon, contentDescription = null, tint = tint)
            painterRes != null -> Icon(painter = painterResource(painterRes), contentDescription = null, tint = tint)
        }
    }
}

@Composable
private fun StaticSupportRow(icon: ImageVector, iconTint: Color, label: String, detail: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SupportRowIcon(icon, null, iconTint)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 2.dp))
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun SupportRow(
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    label: String,
    detail: String,
    painterRes: Int? = null,
    external: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SupportRowIcon(icon, painterRes, iconTint)
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
