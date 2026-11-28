package com.quietgrid.app.platform

interface AppLanguage {
    fun currentTag(): String
    fun setLanguage(tag: String)
    fun systemLanguageTag(): String
}
