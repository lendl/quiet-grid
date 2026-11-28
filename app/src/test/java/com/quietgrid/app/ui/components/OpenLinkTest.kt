package com.quietgrid.app.ui.components

import androidx.compose.ui.platform.UriHandler
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenLinkTest {

    private fun handlerThatThrows(failure: Exception?) = object : UriHandler {
        override fun openUri(uri: String) {
            if (failure != null) throw failure
        }
    }

    @Test
    fun openedLinkReportsSuccess() {
        assertTrue(handlerThatThrows(null).tryOpen("https://example.org"))
    }

    @Test
    fun linkWithNoAppToOpenItReportsFailure() {
        assertFalse(handlerThatThrows(IllegalArgumentException("no activity")).tryOpen("market://details?id=x"))
    }

    @Test
    fun linkToProtectedHandlerReportsFailureInsteadOfCrashing() {
        assertFalse(handlerThatThrows(SecurityException("not exported")).tryOpen("https://example.org"))
    }
}
