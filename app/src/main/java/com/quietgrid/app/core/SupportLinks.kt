package com.quietgrid.app.core

import android.content.Context
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

fun buildBugReportUrl(appVersion: String): String = buildIssueUrl(
    "[Bug] ",
    listOf(
        "## What happened",
        "",
        "Describe the problem you ran into.",
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
        "",
        "## App version",
        "",
        appVersion,
    ).joinToString("\n"),
)

fun buildFeatureRequestUrl(): String = buildIssueUrl(
    "[Feature] ",
    listOf(
        "## What would help",
        "",
        "Describe the feature or improvement you would like to see.",
        "",
        "## Why it would help",
        "",
        "Share the problem it would solve or what would feel better.",
    ).joinToString("\n"),
)

fun buildPuzzleReportUrl(
    gameKey: String,
    difficultyKey: String,
    puzzleId: String?,
    dailyDate: String?,
    result: String,
    appVersion: String,
    appLanguage: String,
): String = buildIssueUrl(
    listOfNotNull("[Puzzle]", gameKey, difficultyKey, puzzleId).joinToString(" "),
    listOfNotNull(
        "## What went wrong",
        "",
        "Describe the problem (for example: more than one solution, a correct move was rejected, a clue looks wrong).",
        "",
        "## Puzzle",
        "",
        "Game: $gameKey",
        "Difficulty: $difficultyKey",
        "Puzzle: ${puzzleId ?: "generated"}",
        dailyDate?.let { "Daily: $it" },
        "Result: $result",
        "",
        "## App",
        "",
        "Version: $appVersion",
        "Language: $appLanguage",
    ).joinToString("\n"),
)

fun appVersionName(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        .getOrNull() ?: "unknown"
