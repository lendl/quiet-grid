package com.quietgrid.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.quietgrid.app.platform.IosBundledFiles
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val files = IosBundledFiles()
    return ComposeUIViewController { SkeletonScreen(files) }
}
