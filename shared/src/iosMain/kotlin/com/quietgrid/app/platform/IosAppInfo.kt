package com.quietgrid.app.platform

import platform.Foundation.NSBundle

private const val SHORT_VERSION_KEY = "CFBundleShortVersionString"

class IosAppInfo : AppInfo {
    override val versionName: String =
        NSBundle.mainBundle.objectForInfoDictionaryKey(SHORT_VERSION_KEY) as? String ?: "unknown"
}
