package com.quietgrid.engine.themeclear

import java.text.Normalizer

internal actual fun decomposeCanonically(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
