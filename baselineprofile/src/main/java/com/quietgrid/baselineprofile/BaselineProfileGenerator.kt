package com.quietgrid.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.util.regex.Pattern
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE_NAME = "com.quietgrid.app"
private const val ARROW_ESCAPE_TITLE = "Arrow Escape"
private const val EXPERT_LABEL = "Expert"
private const val START_NEW_PUZZLE = "Start New Puzzle"
private const val CLOSE = "Close"
private const val HINT = "Show a hint"
private const val BOARD_TAG = "arrowescape_board"
private const val MAX_MISSES = 2
private const val MAX_SWIPES = 6
private val ARROWS_CLEARED: Pattern = Pattern.compile("\\d+ / \\d+ arrows")
private val TAP_POINTS = listOf(0.15f, 0.5f, 0.85f).flatMap { fy -> listOf(0.1f, 0.3f, 0.5f, 0.7f, 0.9f).map { fx -> fx to fy } }

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()
        device.findObject(By.scrollable(true))?.let { list ->
            list.setGestureMargin(device.displayWidth / 5)
            list.fling(Direction.DOWN)
            device.waitForIdle()
            list.fling(Direction.UP)
            device.waitForIdle()
        }
    }

    @Test
    fun arrowEscapeExpert() = rule.collect(packageName = PACKAGE_NAME) {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()
        openArrowEscapeExpert()
        playArrows()
        device.pressBack()
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.openArrowEscapeExpert() {
        findOrSwipeTo(By.text(ARROW_ESCAPE_TITLE)).click()
        device.wait(Until.findObject(By.desc(CLOSE)), 3_000)?.click()
        device.waitForIdle()
        findOrSwipeTo(By.text(EXPERT_LABEL)).click()
        device.wait(Until.findObject(By.text(START_NEW_PUZZLE)), 2_000)?.click()
        checkNotNull(device.wait(Until.findObject(By.res(BOARD_TAG)), 15_000)) { "Arrow Escape board did not appear" }
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.playArrows() {
        val bounds = checkNotNull(device.findObject(By.res(BOARD_TAG))).visibleBounds
        var cleared = clearedCount()
        var misses = 0
        for ((fx, fy) in TAP_POINTS) {
            if (misses >= MAX_MISSES) break
            device.click(bounds.left + (bounds.width() * fx).toInt(), bounds.top + (bounds.height() * fy).toInt())
            device.waitForIdle()
            if (!device.hasObject(By.res(BOARD_TAG))) return
            val now = clearedCount()
            if (now == cleared) misses++ else cleared = now
        }
        device.findObject(By.desc(HINT))?.click()
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.clearedCount(): Int? =
        device.findObject(By.text(ARROWS_CLEARED))?.text?.substringBefore(" /")?.trim()?.toIntOrNull()

    private fun MacrobenchmarkScope.findOrSwipeTo(selector: BySelector): UiObject2 {
        repeat(MAX_SWIPES) {
            device.wait(Until.findObject(selector), 2_000)?.let { return it }
            val x = device.displayWidth / 2
            device.swipe(x, device.displayHeight * 7 / 10, x, device.displayHeight * 3 / 10, 20)
            device.waitForIdle()
        }
        return checkNotNull(device.findObject(selector)) { "Could not find $selector" }
    }
}
