package dev.nullpointless.canvasmusic.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.core.net.toUri
import dev.nullpointless.canvasmusic.data.Repository
import dev.nullpointless.canvasmusic.playback.PlayerConnection
import dev.nullpointless.canvasmusic.ui.Bg
import dev.nullpointless.canvasmusic.ui.CmTypography
import dev.nullpointless.canvasmusic.ui.GreenBright
import dev.nullpointless.canvasmusic.ui.RemoteImage
import dev.nullpointless.canvasmusic.ui.TextSecondary
import dev.nullpointless.canvasmusic.ui.TextTertiary
import dev.nullpointless.canvasmusic.ui.WaveProgress
import dev.nullpointless.canvasmusic.ui.White
import dev.nullpointless.canvasmusic.ui.darken
import dev.nullpointless.canvasmusic.ui.fmtSec
import dev.nullpointless.canvasmusic.ui.rememberDominantColor
import kotlinx.coroutines.launch

@Composable
fun NowPlayingScreen(
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: ((String) -> Unit)? = null,
    onBack: () -> Unit,
    onOpenQueue: () -> Unit
) {
    val item by PlayerConnection.nowPlaying.collectAsState()
    val playing by PlayerConnection.isPlaying.collectAsState()
    val pos by PlayerConnection.positionMs.collectAsState()
    val dur by PlayerConnection.durationMs.collectAsState()
    val shuffle by PlayerConnection.shuffleOn.collectAsState()
    val repeat by PlayerConnection.repeatMode.collectAsState()
    val contextTitle by PlayerConnection.queueTitle.collectAsState()

    val current = item
    if (current == null) {
        Box(Modifier.fillMaxSize().background(Bg))
        return
    }

    val artUrl = current.mediaMetadata.artworkUri?.toString()
    val dominant = rememberDominantColor(artUrl, fallback = Color(0xFF2E2E2E))
    var liked by remember(current.mediaId) { mutableStateOf(Repository.isLikedId(current.mediaId)) }
    var menuOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val artScale by animateFloatAsState(
        targetValue = if (playing) 1f else 0.86f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 170f),
        label = "artScale"
    )

    val progress = if (dur > 0) (pos.toFloat() / dur) else 0f
    val artistId = current.mediaMetadata.extras?.getString("artistId")
    val albumId = current.mediaMetadata.extras?.getString("albumId")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to dominant.darken(0.05f),
                        0.55f to dominant.darken(0.55f),
                        1f to Bg
                    )
                )
            )
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        if (totalDrag < 0 || totalDrag > 120f) return@detectVerticalDragGestures
                        totalDrag += amount
                    },
                    onDragEnd = { if (totalDrag > 120f) onBack() }
                )
            }
            .statusBarsPadding()
    ) {
        // top bar: dismiss, context, menu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close", tint = White, modifier = Modifier.size(28.dp))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "PLAYING FROM",
                    style = CmTypography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.8.sp),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    contextTitle ?: "Canvas Music",
                    style = CmTypography.labelMedium.copy(fontSize = 12.sp),
                    color = White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = White)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (albumId != null && onOpenAlbum != null) {
                        DropdownMenuItem(
                            text = { Text("Go to album", color = White) },
                            onClick = { menuOpen = false; onOpenAlbum(albumId) }
                        )
                    }
                    if (artistId != null) {
                        DropdownMenuItem(
                            text = { Text("Go to artist", color = White) },
                            onClick = { menuOpen = false; onOpenArtist(artistId) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // album art
        Box(Modifier.padding(horizontal = 28.dp)) {
            RemoteImage(
                url = artUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .scale(artScale)
                    .shadow(24.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp)
            )
        }

        Spacer(Modifier.weight(1.2f))

        // title + artist + like
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    current.mediaMetadata.title?.toString() ?: "",
                    style = CmTypography.bodyLarge.copy(fontSize = 21.sp),
                    color = White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    current.mediaMetadata.artist?.toString() ?: "",
                    style = CmTypography.bodyMedium.copy(fontSize = 16.sp),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .let { m ->
                            if (artistId != null) m.clickable { onOpenArtist(artistId) } else m
                        }
                )
            }
            IconButton(onClick = {
                scope.launch {
                    Repository.toggleLikeId(current.mediaId)
                    liked = Repository.isLikedId(current.mediaId)
                }
            }) {
                Icon(
                    if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (liked) "Unlike" else "Like",
                    tint = if (liked) GreenBright else TextSecondary
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // wave progress bar
        WaveProgress(
            progress = progress,
            isPlaying = playing,
            onSeek = { fraction ->
                if (dur > 0) PlayerConnection.seekTo((dur * fraction).toLong())
            },
            modifier = Modifier.padding(horizontal = 28.dp)
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 6.dp)
        ) {
            Text(
                fmtSec((pos / 1000L)),
                style = CmTypography.labelSmall,
                color = TextSecondary
            )
            Text(
                fmtSec((dur / 1000L)),
                style = CmTypography.labelSmall,
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(10.dp))

        // transport controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        ) {
            IconButton(onClick = { PlayerConnection.setShuffle(!shuffle) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffle) GreenBright else TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(
                        Modifier
                            .size(width = 4.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(if (shuffle) GreenBright else Color.Transparent)
                    )
                }
            }
            IconButton(onClick = { PlayerConnection.previous() }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = White, modifier = Modifier.size(38.dp))
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(White)
                    .clickable { PlayerConnection.togglePlayPause() }
            ) {
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = Color(0xFF000000),
                    modifier = Modifier.size(38.dp)
                )
            }
            IconButton(onClick = { PlayerConnection.next() }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = White, modifier = Modifier.size(38.dp))
            }
            IconButton(onClick = { PlayerConnection.cycleRepeat() }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (repeat == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeat != Player.REPEAT_MODE_OFF) GreenBright else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(
                        Modifier
                            .size(width = 4.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(if (repeat != Player.REPEAT_MODE_OFF) GreenBright else Color.Transparent)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // bottom row: queue
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp)
        ) {
            Text(
                "",
                style = CmTypography.labelSmall,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpenQueue) {
                Icon(Icons.Filled.QueueMusic, contentDescription = "Queue", tint = TextSecondary, modifier = Modifier.size(24.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        Box(Modifier.navigationBarsPadding())
    }
}
