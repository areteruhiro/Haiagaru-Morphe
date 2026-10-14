// Adapted from Open-SSTP-Client, revision 5f97511c3afff7dcf76b763882e0e29207ad1a36.
// Copyright (c) 2019 KOBAYASHI Ittoku. MIT; see third-party/open-sstp/LICENSE.
package app.morphe.extension.chmate.vpn.sstp.control

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


internal class LogWriter(logOutput: OutputStream) {
    private val mutex = Mutex()
    private val outputStream = BufferedOutputStream(logOutput)
    private val currentTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())

    internal fun write(message: String) {
        // use directly if it's cannot be race condition, like onCreate and onDestroy
        outputStream.write("[$currentTime] $message\n".toByteArray(Charsets.UTF_8))
    }

    internal suspend fun report(message: String) {
        mutex.withLock { write(message) }
    }

    internal fun close() {
        outputStream.flush()
        outputStream.close()
    }
}

