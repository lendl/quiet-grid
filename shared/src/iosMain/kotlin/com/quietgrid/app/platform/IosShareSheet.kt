package com.quietgrid.app.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController

class IosShareSheet : ShareSheet {
    @OptIn(ExperimentalForeignApi::class)
    override fun shareText(text: String, chooserTitle: String) {
        val presenter = topViewController() ?: return
        val controller = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
        controller.popoverPresentationController?.let { popover ->
            popover.sourceView = presenter.view
            presenter.view.bounds.useContents {
                popover.sourceRect = CGRectMake(size.width / 2, size.height / 2, 0.0, 0.0)
            }
            popover.permittedArrowDirections = 0u
        }
        presenter.presentViewController(controller, animated = true, completion = null)
    }

    private fun topViewController(): UIViewController? {
        val root = keyWindow()?.rootViewController ?: return null
        return generateSequence(root) { it.presentedViewController }.last()
    }

    private fun keyWindow(): UIWindow? =
        UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { scene -> scene.windows.filterIsInstance<UIWindow>() }
            .firstOrNull { it.isKeyWindow() }
}
