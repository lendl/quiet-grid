package com.quietgrid.app

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Modifier
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
class AppGraphTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `every graph property returns the same instance on each read`() {
        val graph = context.appGraph
        val getters = AppGraph::class.java.declaredMethods
            .filter { it.name.startsWith("get") && it.parameterCount == 0 && Modifier.isPublic(it.modifiers) }
        assertTrue(getters.size >= 20)
        getters.forEach { assertSame(it.name, it.invoke(graph), it.invoke(graph)) }
    }

    @Test
    fun `a wrapped context reaches the application graph`() {
        assertSame(context.appGraph, ContextWrapper(context).appGraph)
    }

    @Test
    fun `concurrent first reads share one instance`() {
        val graph = AppGraph(context)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(8)
        val reads = (1..8).map { pool.submit<Any> { start.await(); graph.settingsRepository } }
        start.countDown()
        assertEquals(1, reads.map { it.get() }.toSet().size)
        pool.shutdown()
    }
}
