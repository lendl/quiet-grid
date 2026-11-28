package com.quietgrid.app.platform

import android.content.Context
import java.util.Locale

private const val FALLBACK_LANGUAGE_TAG = "en"

class AndroidAppLanguage(private val context: Context) : AppLanguage {
    override fun currentTag(): String = AppLocale.currentTag(context)

    override fun setLanguage(tag: String) = AppLocale.setLanguage(context, tag)

    override fun systemLanguageTag(): String = Locale.getDefault().language.ifEmpty { FALLBACK_LANGUAGE_TAG }
}
