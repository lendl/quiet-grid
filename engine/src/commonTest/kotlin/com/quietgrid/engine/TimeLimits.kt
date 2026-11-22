package com.quietgrid.engine

import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.measureTime

internal inline fun assertFinishesWithin(limit: Duration, block: () -> Unit) {
    val elapsed = measureTime(block)
    assertTrue(elapsed < limit, "took $elapsed, over the $limit limit")
}
