# Haiagaru settings language

Haiagaru's settings dialog now begins with a bilingual language selector:
Japanese, English, or System. `settingsLanguage` stores `ja`, `en`, or `system`
in Haiagaru's own preferences. Unset/invalid preferences default to Japanese,
including on devices whose system language is English. Save with OK and reopen
settings to apply. Changing the language alone does not restart ChMate or reopen
the settings dialog automatically; a saved confirmation is shown instead.

The existing bilingual `text()` helper now consults this preference, so the
choice also applies to messages using that helper. Other extension screens with
hardcoded Japanese text remain Japanese. The device/global Locale, ChMate
resources and native preferences are not changed. No Android 13 per-app locale
API is required, so Android 12 and earlier are supported by the same logic.

This is shared by 191 dev, 226 dev, 241 and 242 dev.
`VerifySettingsLanguage.java` verifies Japanese default/override, English,
system-language selection, invalid preferences and unchanged global locale.
Actual Android 12 screen confirmation remains pending.
