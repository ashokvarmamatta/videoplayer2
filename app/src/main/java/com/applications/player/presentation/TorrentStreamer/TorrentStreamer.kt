package com.applications.player.presentation.TorrentStreamer

import android.net.Uri
import kotlinx.coroutines.*
import org.libtorrent4j.FileStorage
import org.libtorrent4j.SessionManager
import org.libtorrent4j.Sha1Hash
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentInfo
import org.libtorrent4j.AddTorrentParams
import org.libtorrent4j.swig.torrent_flags_t
import java.io.File

interface TorrentStreamListener {
    fun onStreamReady(localUrl: String)
    fun onError(message: String)
}

class TorrentStreamer(
    private val listener: TorrentStreamListener,
    private val downloadDir: File
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionManager = SessionManager()
    private var currentHandle: TorrentHandle? = null

    private val localHttpPort = 8080
    private val localHost = "127.0.0.1"

    init {
        sessionManager.start()
    }

    fun streamTorrent(torrentLink: String) {
        scope.launch {
            try {
                // 1️⃣ Parse magnet URI
                val params = AddTorrentParams.parseMagnetUri(torrentLink)
                params.savePath = downloadDir.absolutePath

                // 2️⃣ Add torrent using SessionManager.download()
                sessionManager.download(torrentLink, downloadDir, torrent_flags_t())

                // 3️⃣ Wait for torrent handle to appear
                val infoHash: Sha1Hash = params.infoHashes.best
                var waited = 0L
                val maxWaitTime = 30_000L
                while (isActive && (currentHandle == null || currentHandle?.torrentFile() == null) && waited < maxWaitTime) {
                    currentHandle = sessionManager.find(infoHash)
                    delay(500)
                    waited += 500
                }

                if (currentHandle?.torrentFile() == null) {
                    onMain { listener.onError("Failed to retrieve torrent metadata (timeout).") }
                    return@launch
                }

                // 4️⃣ Sequential download setup: start from first piece
                currentHandle?.setSequentialRange(0)

                // 5️⃣ Extract torrent info
                val torrentInfo: TorrentInfo = currentHandle!!.torrentFile()
                    ?: run {
                        onMain { listener.onError("Torrent metadata missing.") }
                        return@launch
                    }

                val files: FileStorage = torrentInfo.files()
                if (files.numFiles() == 0) {
                    onMain { listener.onError("Torrent contains no files.") }
                    return@launch
                }

                // 6️⃣ Find largest file (likely video)
                var largestIndex = -1
                var largestSize = 0L
                for (i in 0 until files.numFiles()) {
                    val size = files.fileSize(i)
                    if (size > largestSize) {
                        largestSize = size
                        largestIndex = i
                    }
                }

                if (largestIndex < 0) {
                    onMain { listener.onError("No suitable file found in torrent.") }
                    return@launch
                }

                val innerPath = files.filePath(largestIndex)
                val streamFileName = File(innerPath).name

                // 7️⃣ Build local stream URL
                val encodedName = Uri.encode(streamFileName)
                val streamUrl = "http://$localHost:$localHttpPort/$encodedName"

                onMain { listener.onStreamReady(streamUrl) }

            } catch (e: Exception) {
                onMain { listener.onError("Torrent session error: ${e.message}") }
            }
        }
    }

    fun stop() {
        scope.launch {
            try { currentHandle?.pause() } catch (_: Throwable) {}
            try { sessionManager.stop() } catch (_: Throwable) {}
            scope.cancel()
        }
    }

    private suspend fun onMain(block: () -> Unit) {
        withContext(Dispatchers.Main) { block() }
    }
}
