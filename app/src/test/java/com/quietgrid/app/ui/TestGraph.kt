package com.quietgrid.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import com.quietgrid.app.LocalAppGraph
import com.quietgrid.app.appGraph

fun AndroidComposeTestRule<*, ComponentActivity>.setContentWithGraph(content: @Composable () -> Unit) {
    setContent {
        CompositionLocalProvider(LocalAppGraph provides activity.appGraph, content = content)
    }
}
