package com.quietgrid.engine.themeclear

import platform.Foundation.NSString
import platform.Foundation.decomposedStringWithCanonicalMapping

@Suppress("CAST_NEVER_SUCCEEDS")
internal actual fun decomposeCanonically(value: String): String =
    (value as NSString).decomposedStringWithCanonicalMapping
