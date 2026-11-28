package com.quietgrid.engine.themeclear

import java.text.Normalizer

actual fun decomposeCanonically(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
