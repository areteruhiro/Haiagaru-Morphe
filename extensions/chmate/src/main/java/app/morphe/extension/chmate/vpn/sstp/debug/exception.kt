// Adapted from Open-SSTP-Client, revision 5f97511c3afff7dcf76b763882e0e29207ad1a36.
// Copyright (c) 2019 KOBAYASHI Ittoku. MIT; see third-party/open-sstp/LICENSE.
package app.morphe.extension.chmate.vpn.sstp.debug


internal class ParsingDataUnitException : Exception("Failed to parse data unit")

internal fun assertAlways(value: Boolean) {
    if (!value) {
        throw AssertionError()
    }
}

