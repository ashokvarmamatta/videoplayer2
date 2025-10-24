package com.applications.player.presentation.TorrentStreamer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.applications.player.R
import java.io.File

class VideoStreamActivity : AppCompatActivity(), TorrentStreamListener {

    private lateinit var torrentStreamer: TorrentStreamer
    private var exoPlayer: ExoPlayer? = null
    private lateinit var playerView: PlayerView // Assumes you have one in your layout

    // --- Lifecycle and Initialization ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Assume you are setting your layout here, which contains a PlayerView
        setContentView(R.layout.activity_video_stream)
        playerView = findViewById(R.id.player_view)

        // Setup the streamer
        val downloadDir = File(filesDir, "torrents")
        if (!downloadDir.exists()) downloadDir.mkdirs()

        torrentStreamer = TorrentStreamer(this, downloadDir)

        // Start the torrent process
        val magnetLink = "magnet:?xt=urn:btih:39646c637a49aa5740c81085a115738bbcbdaf62&dn=www.1TamilMV.plus%20-%20Into%20the%20Deep%20%282025%29%20BR-Rip%20-%20x264%20-%20%5BTam%20%2B%20Tel%20%2B%20Hin%5D%20-%20AAC%20-%20450MB%20-%20ESub.mkv&xl=462211407&tr=http%3A%2F%2Ftracker.vraphim.com%3A6969%2Fannounce&tr=http%3A%2F%2Ftracker.bt4g.com%3A2095%2Fannounce&tr=http%3A%2F%2Ftracker.renfei.net%3A8080%2Fannounce&tr=https%3A%2F%2Ftr.nyacat.pw%3A443%2Fannounce&tr=udp%3A%2F%2Ftracker.fnix.net%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce&tr=udp%3A%2F%2Fopen.stealth.si%3A80%2Fannounce&tr=udp%3A%2F%2Ftracker.torrent.eu.org%3A451%2Fannounce&tr=udp%3A%2F%2Fbandito.byterunner.io%3A6969%2Fannounce&tr=udp%3A%2F%2Fretracker.lanta.me%3A2710%2Fannounce&tr=udp%3A%2F%2Fwepzone.net%3A6969%2Fannounce&tr=udp%3A%2F%2Fevan.im%3A6969%2Fannounce&tr=udp%3A%2F%2Fttk2.nbaonlineservice.com%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.tryhackx.org%3A6969%2Fannounce&tr=https%3A%2F%2Ftr.zukizuki.org%3A443%2Fannounce&tr=udp%3A%2F%2Fexplodie.org%3A6969%2Fannounce&tr=https%3A%2F%2Ftracker.yemekyedim.com%3A443%2Fannounce&tr=udp%3A%2F%2Ftracker.breizh.pm%3A6969%2Fannounce&tr=udp%3A%2F%2Fd40969.acod.regrucolo.ru%3A6969%2Fannounce&tr=udp%3A%2F%2Ftr4ck3r.duckdns.org%3A6969%2Fannounce&tr=udp%3A%2F%2Fudp.tracker.projectk.org%3A23333%2Fannounce&tr=udp%3A%2F%2Ftracker.srv00.com%3A6969%2Fannounce&tr=https%3A%2F%2Ftracker.gcrenwp.top%3A443%2Fannounce&tr=udp%3A%2F%2Fopen.demonii.com%3A1337%2Fannounce&tr=http%3A%2F%2Fbt.okmp3.ru%3A2710%2Fannounce&tr=http%3A%2F%2Ftracker.mywaifu.best%3A6969%2Fannounce&tr=udp%3A%2F%2Fexodus.desync.com%3A6969%2Fannounce&tr=udp%3A%2F%2Ft.overflow.biz%3A6969%2Fannounce&tr=udp%3A%2F%2Fopentracker.io%3A6969%2Fannounce&tr=udp%3A%2F%2Fopen.dstud.io%3A6969%2Fannounce&tr=http%3A%2F%2Ftracker1.itzmx.com%3A8080%2Fannounce&tr=udp%3A%2F%2Ftracker.dler.com%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.gmi.gd%3A6969%2Fannounce&tr=https%3A%2F%2Ftr2.trkb.ru%3A443%2Fannounce&tr=udp%3A%2F%2Ftr3.ysagin.top%3A2715%2Fannounce&tr=https%3A%2F%2Ftracker.aburaya.live%3A443%2Fannounce&tr=https%3A%2F%2Fretracker.x2k.ru%3A443%2Fannounce&tr=http%3A%2F%2Fipv4.rer.lol%3A2710%2Fannounce&tr=https%3A%2F%2Ftorrent.tracker.durukanbal.com%3A443%2Fannounce&tr=udp%3A%2F%2Fwww.torrent.eu.org%3A451%2Fannounce&tr=https%3A%2F%2Ftracker.leechshield.link%3A443%2Fannounce&tr=udp%3A%2F%2Fbittorrent-tracker.e-n-c-r-y-p-t.net%3A1337%2Fannounce" // Replace with your link
        torrentStreamer.streamTorrent(magnetLink)

        // Initialize ExoPlayer
        initializePlayer()
    }

    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        playerView.player = exoPlayer
    }

    override fun onStop() {
        super.onStop()
        torrentStreamer.stop()
        releasePlayer()
    }

    private fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    // --- TorrentStreamListener Implementation ---

    /**
     * Called when the TorrentStreamer successfully sets up the stream and local server.
     */
    override fun onStreamReady(localUrl: String) {
        // This is the key integration point: Use the local URL to create a MediaItem
        val mediaItem = MediaItem.fromUri(localUrl)

        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            play() // Start playback
        }
    }

    /**
     * Handles any errors from the torrent process.
     */
    override fun onError(message: String) {
        // Display an error message to the user
        // Log.e("STREAMING", message)
    }
}