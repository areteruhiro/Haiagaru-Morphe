// Adapted from Open-SSTP-Client, revision 5f97511c3afff7dcf76b763882e0e29207ad1a36.
// Copyright (c) 2019 KOBAYASHI Ittoku. MIT; see third-party/open-sstp/LICENSE.
package app.morphe.extension.chmate.vpn.sstp.preference.accessor

import android.content.SharedPreferences
import app.morphe.extension.chmate.vpn.sstp.preference.DEFAULT_SET_MAP
import app.morphe.extension.chmate.vpn.sstp.preference.OscPrefKey


internal fun getSetPrefValue(key: OscPrefKey, prefs: SharedPreferences): Set<String> {
    return prefs.getStringSet(key.name, DEFAULT_SET_MAP[key]!!)!!
}

internal fun setSetPrefValue(value: Set<String>, key: OscPrefKey, prefs: SharedPreferences) {
    prefs.edit().also {
        it.putStringSet(key.name, value)
        it.apply()
    }
}

