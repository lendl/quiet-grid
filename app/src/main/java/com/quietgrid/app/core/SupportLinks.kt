package com.quietgrid.app.core

import java.net.URLEncoder

const val SUPPORT_EMAIL = "quiet-grid@outlook.com"
const val REPO_URL = "https://github.com/lendl/quiet-grid"
const val ISSUES_URL = "$REPO_URL/issues"
const val PLAY_STORE_APP_URL = "market://details?id=com.quietgrid.app"
const val PLAY_STORE_WEB_URL = "https://play.google.com/store/apps/details?id=com.quietgrid.app"

private fun buildIssueUrl(title: String, body: String): String {
    val encodedTitle = URLEncoder.encode(title, "UTF-8")
    val encodedBody = URLEncoder.encode(body, "UTF-8")
    return "$ISSUES_URL/new?title=$encodedTitle&body=$encodedBody"
}

fun buildBugReportUrl(): String = buildIssueUrl(
    "[Bug] ",
    listOf(
        "## What happened",
        "",
        "Describe problem you ran into.",
        "",
        "## Steps to reproduce",
        "",
        "1. ",
        "2. ",
        "3. ",
        "",
        "## Expected behavior",
        "",
        "Describe what you expected instead.",
    ).joinToString("\n"),
)

fun buildFeatureRequestUrl(): String = buildIssueUrl(
    "[Feature] ",
    listOf(
        "## What would help",
        "",
        "Describe feature or improvement you would like to see.",
        "",
        "## Why it would help",
        "",
        "Share problem it would solve or what would feel better.",
    ).joinToString("\n"),
)
