package com.quietgrid.app.core

import androidx.annotation.StringRes
import com.quietgrid.app.R

data class HelpWantedItem(@param:StringRes val titleRes: Int, @param:StringRes val blurbRes: Int, val url: String)

val HELP_WANTED_ITEMS: List<HelpWantedItem> = listOf(
    HelpWantedItem(R.string.help_wanted_icons_title, R.string.help_wanted_icons_blurb, "https://github.com/lendl/quiet-grid/issues/36"),
    HelpWantedItem(R.string.help_wanted_store_graphics_title, R.string.help_wanted_store_graphics_blurb, "https://github.com/lendl/quiet-grid/issues/33"),
)
