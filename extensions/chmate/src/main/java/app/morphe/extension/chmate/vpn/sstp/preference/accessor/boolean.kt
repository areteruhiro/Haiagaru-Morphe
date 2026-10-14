// Adapted from Open-SSTP-Client, revision 5f97511c3afff7dcf76b763882e0e29207ad1a36.
// Copyright (c) 2019 KOBAYASHI Ittoku. MIT; see third-party/open-sstp/LICENSE.
package app.morphe.extension.chmate.vpn.sstp.preference.accessor

import android.content.SharedPreferences
import app.morphe.extension.chmate.vpn.sstp.preference.DEFAULT_BOOLEAN_MAP
import app.morphe.extension.chmate.vpn.sstp.preference.OscPrefKey


internal fun getBooleanPrefValue(key: OscPrefKey, prefs: SharedPreferences): Boolean {
    return prefs.getBoolean(key.name, DEFAULT_BOOLEAN_MAP[key]!!)
}

internal fun setBooleanPrefValue(value: Boolean, key: OscPrefKey, prefs: SharedPreferences) {
    prefs.edit().also {
        it.putBoolean(key.name, value)
        it.apply()
    }
}

