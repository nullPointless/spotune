package dev.nullpointless.canvasmusic.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dev.nullpointless.canvasmusic.api.Song
import dev.nullpointless.canvasmusic.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object PlayerConnection {

    private lateinit var appContext: Context
    private var controller: MediaController? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val isReady = MutableStateFlow(false)
    val nowPlaying = MutableStateFlow<MediaItem?>(null)
    val isPlaying = MutableStateFlow(false)
    val positionMs = MutableStateFlow(0L)
    val durationMs = MutableStateFlow(0L)
    val shuffleOn = MutableStateFlow(false)
    val repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val queue = MutableStateFlow<List<MediaItem>>(emptyList())
    val currentIndex = MutableStateFlow(0)
    val queueTitle = MutableStateFlow<String?>(null)

    fun init(context: Context) {
        appContext = context.applicationContext
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener({
            val c = try {
                future.get()
            } catch (e: Exception) {
                return@addListener
            }
            controller = c
            attach(c)
            isReady.value = true
        }, Runnable::run)

        scope.launch {
            isPlaying.collect { playing ->
                while (playing) {
                    controller?.let { positionMs.value = it.currentPosition }
                    delay(250)
                }
            }
        }
    }

    private fun attach(c: MediaController) {
        c.addListener(object : Player.Listener {
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                nowPlaying.value = item
                positionMs.value = 0
                durationMs.value = c.duration.coerceAtLeast(0)
                currentIndex.value = c.currentMediaItemIndex
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying.value = playing
                if (!playing) positionMs.value = c.currentPosition
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                durationMs.value = c.duration.coerceAtLeast(0)
            }

            override fun onShuffleModeEnabledChanged(enabled: Boolean) {
                shuffleOn.value = enabled
            }

            override fun onRepeatModeChanged(mode: Int) {
                repeatMode.value = mode
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                refreshQueue(c)
            }
        })
        nowPlaying.value = c.currentMediaItem
        isPlaying.value = c.isPlaying
        durationMs.value = c.duration.coerceAtLeast(0)
        positionMs.value = c.currentPosition
        currentIndex.value = c.currentMediaItemIndex
        shuffleOn.value = c.shuffleModeEnabled
        repeatMode.value = c.repeatMode
        refreshQueue(c)
    }

    private fun refreshQueue(c: MediaController) {
        queue.value = (0 until c.mediaItemCount).map { c.getMediaItemAt(it) }
    }

    // ---------- queue building ----------

    private fun mediaItemFor(song: Song): MediaItem? {
        val api = Repository.api ?: return null
        val meta = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist ?: "")
            .setAlbumTitle(song.album)
            .setArtworkUri(api.coverArtUrl(song.coverArt ?: song.albumId)?.toUri())
            .setDurationMs((song.duration ?: 0L) * 1000L)
            .setExtras(
                bundleOf(
                    "albumId" to song.albumId,
                    "artistId" to song.artistId
                )
            )
            .build()
        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(api.streamUrl(song.id).toUri())
            .setMediaMetadata(meta)
            .build()
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false, contextName: String? = null) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        val items = songs.mapNotNull { mediaItemFor(it) }
        if (items.isEmpty()) return
        queueTitle.value = contextName
        if (shuffle) {
            val first = items.getOrNull(startIndex) ?: items.first()
            val rest = items.filterIndexed { i, _ -> i != startIndex }.shuffled()
            c.setMediaItems(listOf(first) + rest, 0, 0L)
            c.shuffleModeEnabled = false
        } else {
            c.setMediaItems(items, startIndex, 0L)
        }
        c.prepare()
        c.play()
    }

    fun enqueue(song: Song) {
        val c = controller ?: return
        mediaItemFor(song)?.let { c.addMediaItem(it) }
    }

    fun playNext(song: Song) {
        val c = controller ?: return
        mediaItemFor(song)?.let { c.addMediaItem(c.currentMediaItemIndex + 1, it) }
    }

    fun playSongAt(index: Int) {
        val c = controller ?: return
        c.seekTo(index, 0L)
        c.play()
    }

    fun removeAt(index: Int) {
        controller?.removeMediaItem(index)
    }

    // ---------- transport ----------

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        positionMs.value = ms
    }

    fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun release() {
        controller?.release()
        controller = null
        scope.cancel()
    }
}
