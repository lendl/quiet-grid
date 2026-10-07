package com.quietgrid.engine.whodidit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhoDidItThemesTest {
    private val traitCategories = listOf(WhoDidItCategory.SUSPECT, WhoDidItCategory.PLACE, WhoDidItCategory.ITEM)

    @Test fun `catalog has at least 6 themes and at least 2 murder themes with unique ids`() {
        assertTrue(WHODIDIT_THEMES.size >= 6)
        assertTrue(WHODIDIT_THEMES.count { it.tone == WhoDidItTone.MURDER } >= 2)
        assertEquals(WHODIDIT_THEMES.size, WHODIDIT_THEMES.map { it.id }.toSet().size)
        assertEquals("castle", whoDidItTheme("castle").id)
    }

    @Test fun `every pool has exactly WHODIDIT_POOL_SIZE entries for all four categories`() {
        for (theme in WHODIDIT_THEMES) {
            assertEquals(WhoDidItCategory.entries.toSet(), theme.pools.keys)
            for (pool in theme.pools.values) assertEquals(WHODIDIT_POOL_SIZE, pool.size)
        }
    }

    @Test fun `every suspect, place and item entry has exactly one trait per dimension of its category and motives have none`() {
        for (theme in WHODIDIT_THEMES) {
            for (category in traitCategories) {
                val dimensions = WhoDidItTrait.entries.filter { it.category == category }.map { it.dimension }.toSet()
                for (traits in theme.pools.getValue(category)) {
                    assertTrue(traits.all { it.category == category })
                    assertEquals(dimensions, traits.map { it.dimension }.toSet())
                    assertEquals(dimensions.size, traits.size)
                }
            }
            assertTrue(theme.pools.getValue(WhoDidItCategory.MOTIVE).all { it.isEmpty() })
        }
    }

    @Test fun `every trait dimension is non-constant within each pool`() {
        for (theme in WHODIDIT_THEMES) for (category in traitCategories) {
            val pool = theme.pools.getValue(category)
            for (dimension in WhoDidItTrait.entries.filter { it.category == category }.map { it.dimension }.toSet()) {
                val values = pool.flatten().filter { it.dimension == dimension }.toSet()
                assertTrue("${theme.id} $category $dimension", values.size >= 2)
            }
        }
    }

    @Test fun `caseCount is at least 1`() {
        assertTrue(WHODIDIT_THEMES.all { it.caseCount >= 1 })
    }
}
