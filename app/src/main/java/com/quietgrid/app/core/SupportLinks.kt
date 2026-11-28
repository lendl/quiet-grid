package com.quietgrid.app.core

const val SUPPORT_EMAIL = "quiet-grid@outlook.com"
const val REPO_URL = "https://github.com/lendl/quiet-grid"
const val ISSUES_URL = "$REPO_URL/issues"
const val PLAY_STORE_APP_URL = "market://details?id=com.quietgrid.app"
const val PLAY_STORE_WEB_URL = "https://play.google.com/store/apps/details?id=com.quietgrid.app"

private const val HEX_DIGITS = "0123456789ABCDEF"

private fun isUnreservedByte(value: Int): Boolean =
    value in 'A'.code..'Z'.code || value in 'a'.code..'z'.code || value in '0'.code..'9'.code ||
        value == '-'.code || value == '.'.code || value == '_'.code || value == '~'.code

fun encodeUrlComponent(value: String): String = buildString {
    value.encodeToByteArray().forEach { byte ->
        val unsigned = byte.toInt() and 0xFF
        if (isUnreservedByte(unsigned)) {
            append(unsigned.toChar())
        } else {
            append('%')
            append(HEX_DIGITS[unsigned shr 4])
            append(HEX_DIGITS[unsigned and 0xF])
        }
    }
}

private fun buildIssueUrl(title: String, body: String): String {
    val encodedTitle = encodeUrlComponent(title)
    val encodedBody = encodeUrlComponent(body)
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

fun buildThemeWordsIssueUrl(
    gameKey: String,
    themeId: String,
    locale: String,
    added: List<String>,
    removed: List<String>,
    appVersion: String,
): String {
    fun section(heading: String, words: List<String>): List<String> =
        if (words.isEmpty()) emptyList() else listOf("## $heading", "") + words.map { "- $it" } + ""
    return buildIssueUrl(
        "[Theme words] $themeId ($locale)",
        (
            listOf(
                "## Theme",
                "",
                "Theme: $themeId",
                "Language: $locale",
                "Game: $gameKey",
                "",
            ) +
                section("Add", added) +
                section("Remove", removed) +
                listOf(
                    "## Notes",
                    "",
                    "Anything else about these words (optional).",
                    "",
                    "## App",
                    "",
                    "Version: $appVersion",
                )
            ).joinToString("\n"),
    )
}
