package com.quietgrid.app.platform

import android.content.Context

class AndroidAppInfo(context: Context) : AppInfo {
    override val versionName: String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "unknown"
}
