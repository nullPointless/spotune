package dev.nullpointless.canvasmusic.playback

import android.content.Intent
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dev.nullpointless.canvasmusic.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var lastItemId: String? = null
    private var itemStartElapsed = 0L
    private var lastKnownDurationMs = 0L

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(newItem: MediaItem?, reason: Int) {
                val now = SystemClock.elapsedRealtime()
                val old = lastItemId
                val playedMs = now - itemStartElapsed
                if (old != null && shouldScrobble(reason, playedMs)) {
                    val api = Repository.api
                    if (api != null) {
                        serviceScope.launch { runCatching { api.scrobble(old, true) } }
                    }
                }
                lastItemId = newItem?.mediaId
                itemStartElapsed = now
                if (newItem != null) {
                    val api = Repository.api
                    if (api != null) {
                        serviceScope.launch { runCatching { api.scrobble(newItem.mediaId, false) } }
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                mediaSession?.player?.let { p ->
                    if (p.duration > 0) lastKnownDurationMs = p.duration
                }
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
    }

    private fun shouldScrobble(reason: Int, playedMs: Long): Boolean {
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) return true
        if (playedMs >= 240_000L) return true
        if (lastKnownDurationMs > 0 && playedMs >= lastKnownDurationMs / 2) return true
        return false
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.player?.let { player ->
            if (!player.isPlaying) stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        serviceScope.cancel()
        super.onDestroy()
    }
}
