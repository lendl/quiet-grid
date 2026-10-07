package com.quietgrid.app.core

import androidx.annotation.StringRes

data class HelpWantedItem(@param:StringRes val titleRes: Int, @param:StringRes val blurbRes: Int, val url: String)

val HELP_WANTED_ITEMS: List<HelpWantedItem> = emptyList()
