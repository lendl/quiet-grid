package com.quietgrid.app.platform

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

class IosHaptics : Haptics {
    private val impact = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val notification = UINotificationFeedbackGenerator()

    override fun tapFeedback() = impact.impactOccurred()

    override fun correctFeedback() =
        notification.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)

    override fun incorrectFeedback() =
        notification.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError)
}
