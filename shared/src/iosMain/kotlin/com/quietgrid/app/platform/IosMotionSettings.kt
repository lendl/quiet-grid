package com.quietgrid.app.platform

import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

class IosMotionSettings : MotionSettings {
    override fun animationsEnabled(): Boolean = !UIAccessibilityIsReduceMotionEnabled()
}
