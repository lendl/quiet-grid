package com.quietgrid.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R

private data class InfoSection(val heading: Int, val body: Int)
private data class InfoContent(val title: Int, val intro: Int, val sections: List<InfoSection>)

fun supportInfoTitleRes(key: String): Int? = INFO_CONTENT[key]?.title

private val INFO_CONTENT = mapOf(
    "privacy" to InfoContent(
        title = R.string.support_info_privacy_title,
        intro = R.string.support_info_privacy_intro,
        sections = listOf(
            InfoSection(R.string.support_info_privacy_heading_1, R.string.support_info_privacy_body_1),
            InfoSection(R.string.support_info_privacy_heading_2, R.string.support_info_privacy_body_2),
            InfoSection(R.string.support_info_privacy_heading_3, R.string.support_info_privacy_body_3),
        ),
    ),
    "about" to InfoContent(
        title = R.string.support_info_about_title,
        intro = R.string.support_info_about_intro,
        sections = listOf(
            InfoSection(R.string.support_info_about_heading_1, R.string.support_info_about_body_1),
            InfoSection(R.string.support_info_about_heading_2, R.string.support_info_about_body_2),
        ),
    ),
    "licenses" to InfoContent(
        title = R.string.support_info_licenses_title,
        intro = R.string.support_info_licenses_intro,
        sections = listOf(
            InfoSection(R.string.support_info_licenses_heading_1, R.string.support_info_licenses_body_1),
            InfoSection(R.string.support_info_licenses_heading_2, R.string.support_info_licenses_body_2),
        ),
    ),
)

@Composable
fun SupportInfoScreen(key: String) {
    val content = INFO_CONTENT[key] ?: return
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(stringResource(content.intro), style = MaterialTheme.typography.bodyMedium)
        content.sections.forEach { section ->
            Text(
                stringResource(section.heading),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            )
            Text(stringResource(section.body), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
