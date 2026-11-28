package com.quietgrid.app.ui.components

import androidx.compose.ui.platform.UriHandler

fun UriHandler.tryOpen(url: String): Boolean = runCatching { openUri(url) }.isSuccess
