package com.quietgrid.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.CONTRIBUTORS
import com.quietgrid.app.core.REPO_URL

@Composable
fun ContributorsScreen() {
    val context = LocalContext.current
    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.contributors_intro), style = MaterialTheme.typography.bodyMedium)

        Column {
            HorizontalDivider()
            CONTRIBUTORS.forEach { contributor ->
                SupportRow("@${contributor.githubHandle}", "", external = true) {
                    openUrl("https://github.com/${contributor.githubHandle}")
                }
            }
        }

        ContributorsCta(onContribute = { openUrl(REPO_URL) })
    }
}

@Composable
private fun ContributorsCta(onContribute: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color = MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onContribute)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.contributors_cta_title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.contributors_cta_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
