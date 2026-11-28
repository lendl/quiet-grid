package com.quietgrid.app.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

private const val APPLE_LANGUAGES_KEY = "AppleLanguages"
private const val FALLBACK_LANGUAGE_TAG = "en"

class IosAppLanguage : AppLanguage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun currentTag(): String {
        val appDomain = NSBundle.mainBundle.bundleIdentifier?.let { defaults.persistentDomainForName(it) }
        val languages = appDomain?.get(APPLE_LANGUAGES_KEY) as? List<*>
        return (languages?.firstOrNull() as? String)?.let(::languageOf).orEmpty()
    }

    override fun setLanguage(tag: String) {
        if (tag.isEmpty()) {
            defaults.removeObjectForKey(APPLE_LANGUAGES_KEY)
        } else {
            defaults.setObject(listOf(tag), APPLE_LANGUAGES_KEY)
        }
    }

    override fun systemLanguageTag(): String =
        (NSLocale.preferredLanguages.firstOrNull() as? String)?.let(::languageOf)?.ifEmpty { null }
            ?: FALLBACK_LANGUAGE_TAG

    private fun languageOf(tag: String): String = tag.substringBefore('-')
}
