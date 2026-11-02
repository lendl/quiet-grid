package com.quietgrid.app.ui.journey.map

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.quietgrid.app.ui.theme.ResolvedTheme
import com.quietgrid.app.ui.theme.colorSchemeFor
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w390dp-h844dp-port")
class JourneySceneCanvasTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `every theme's scene draws`() {
        var theme by mutableStateOf(ResolvedTheme.LIGHT)
        val layout = journeyRoadLayout(stopCount = 12, width = 390f)
        composeRule.setContent {
            MaterialTheme(colorScheme = colorSchemeFor(theme)) {
                JourneySceneCanvas(
                    kind = journeySceneFor(theme),
                    layout = layout,
                    doneCount = 4,
                    seed = journeySceneSeed("arrowescape", 1),
                    modifier = Modifier
                        .size(layout.width.dp, layout.height.dp)
                        .testTag("scene"),
                )
            }
        }
        ResolvedTheme.entries.forEach { next ->
            theme = next
            composeRule.waitForIdle()
            val image = composeRule.onNodeWithTag("scene").captureToImage()
            assertTrue(next.name, image.width > 0 && image.height > 0)
        }
    }
}
