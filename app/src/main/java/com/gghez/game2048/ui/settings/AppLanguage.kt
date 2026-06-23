package com.gghez.game2048.ui.settings

/**
 * A language the app ships UI translations for, shown in the in-app language
 * selector. [tag] is the locale tag used both for persistence and for applying
 * the locale (see MainActivity.attachBaseContext); empty tag = follow the system.
 *
 * Hebrew intentionally uses the legacy "iw" tag so it resolves to res/values-iw.
 * [flag] is a Unicode flag emoji; [nativeName] is the language's own endonym, so
 * it reads correctly regardless of the current UI locale.
 *
 * Keep in sync with res/xml/locales_config.xml and the res/values-* folders.
 */
data class AppLanguage(val tag: String, val flag: String, val nativeName: String)

/** Selectable languages, in the order shown in the selector (after "System default"). */
val APP_LANGUAGES: List<AppLanguage> = listOf(
    AppLanguage("en", "🇬🇧", "English"),
    AppLanguage("fr", "🇫🇷", "Français"),
    AppLanguage("es", "🇪🇸", "Español"),
    AppLanguage("de", "🇩🇪", "Deutsch"),
    AppLanguage("it", "🇮🇹", "Italiano"),
    AppLanguage("ja", "🇯🇵", "日本語"),
    AppLanguage("ko", "🇰🇷", "한국어"),
    AppLanguage("zh", "🇨🇳", "中文"),
    AppLanguage("iw", "🇮🇱", "עברית"),
)
