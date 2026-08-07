package com.quietgrid.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.quietgrid.app.ui.theme.QuietGridTheme
import com.quietgrid.app.ui.theme.ResolvedTheme

@Composable
private fun BottomNavBarPreview(resolvedTheme: ResolvedTheme) {
    QuietGridTheme(resolvedTheme = resolvedTheme) {
        BottomNavBar(selectedTab = AppTab.GAMES, onSelectTab = {})
    }
}

@PreviewTest
@Preview(name = "Light", showBackground = true)
@Composable
fun BottomNavBarLightPreview() {
    BottomNavBarPreview(ResolvedTheme.LIGHT)
}

@PreviewTest
@Preview(name = "Dark", showBackground = true)
@Composable
fun BottomNavBarDarkPreview() {
    BottomNavBarPreview(ResolvedTheme.DARK)
}

@PreviewTest
@Preview(name = "Pencil", showBackground = true)
@Composable
fun BottomNavBarPencilPreview() {
    BottomNavBarPreview(ResolvedTheme.PENCIL)
}

@PreviewTest
@Preview(name = "Light, large font", showBackground = true, fontScale = 1.5f)
@Composable
fun BottomNavBarLargeFontPreview() {
    BottomNavBarPreview(ResolvedTheme.LIGHT)
}
