package dev.nullpointless.canvasmusic.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.nullpointless.canvasmusic.api.Song
import dev.nullpointless.canvasmusic.data.Repository
import dev.nullpointless.canvasmusic.playback.PlayerConnection
import kotlinx.coroutines.launch
import java.util.Locale

// ---------- formatting ----------

fun fmtSec(sec: Long): String {
    val s = sec.coerceAtLeast(0)
    return if (s >= 3600) String.format(Locale.US, "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    else String.format(Locale.US, "%d:%02d", s / 60, s % 60)
}

fun fmtTotal(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    return when {
        h > 0 -> if (m > 0) "${h} hr ${m} min" else "${h} hr"
        else -> "${m} min"
    }
}

fun fmtCount(n: Long?): String {
    if (n == null) return "0"
    return when {
        n >= 1_000_000 -> String.format(Locale.US, "%.1fM", n / 1_000_000.0)
        n >= 1_000 -> String.format(Locale.US, "%.1fK", n / 1_000.0)
        else -> n.toString()
    }
}

// ---------- images ----------

@Composable
fun RemoteImage(
    url: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(0.dp),
    placeholderColor: Color = TextTertiary
) {
    if (url == null) {
        Box(modifier = modifier.clip(shape).background(placeholderColor.copy(alpha = 0.25f)))
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(180)
                .build(),
            contentDescription = null,
            modifier = modifier.clip(shape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun ArtPlaceholder(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(4.dp), icon: Dp = 28.dp) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(Color(0xFF3D3D3D), Color(0xFF282828))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.MusicNote,
            contentDescription = null,
            tint = Color(0xFF8D8D8D),
            modifier = Modifier.size(icon)
        )
    }
}

// ---------- indicators ----------

@Composable
fun EqIndicator(color: Color = GreenBright, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "eq")
    val a by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "a"
    )
    val b by transition.animateFloat(
        initialValue = 1f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(490, easing = LinearEasing), RepeatMode.Reverse),
        label = "b"
    )
    val c by transition.animateFloat(
        initialValue = 0.55f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(560, easing = LinearEasing), RepeatMode.Reverse),
        label = "c"
    )
    Canvas(modifier = modifier.size(width = 16.dp, height = 14.dp)) {
        val barW = 3.dp.toPx()
        val gap = 2.5.dp.toPx()
        fun bar(level: Float, x: Float) {
            val h = size.height * level
            drawRoundRect(
                color = color,
                topLeft = Offset(x, (size.height - h) / 2f),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f)
            )
        }
        bar(a, 0f)
        bar(b, barW + gap)
        bar(c, 2 * (barW + gap))
    }
}

// ---------- sections ----------

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = CmTypography.titleMedium,
        color = White,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
    )
}

@Composable
fun LoadingSection(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = GreenBright, strokeWidth = 2.5.dp, modifier = Modifier.size(34.dp))
    }
}

@Composable
fun ErrorSection(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Couldn't load data", style = CmTypography.titleMedium, color = White)
        Spacer(Modifier.height(6.dp))
        Text(message, style = CmTypography.bodySmall, color = TextSecondary, maxLines = 3)
        if (onRetry != null) {
            Spacer(Modifier.height(14.dp))
            Text(
                "Retry",
                style = CmTypography.labelLarge,
                color = GreenBright,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onRetry() }
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun EmptySection(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = CmTypography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}

// ---------- async loading helper ----------

sealed interface LState<out T> {
    data object Loading : LState<Nothing>
    data class Ready<T>(val data: T) : LState<T>
    data class Failed(val message: String) : LState<Nothing>
}

@Composable
fun <T> loadState(key: Any? = Unit, loader: suspend () -> T): LState<T> =
    produceState<LState<T>>(LState.Loading, key) {
        value = try {
            LState.Ready(loader())
        } catch (e: Exception) {
            LState.Failed(e.message ?: "Something went wrong")
        }
    }

// ---------- track row ----------

@Composable
fun TrackRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    showArt: Boolean = false,
    artUrl: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onOpenAlbum: ((String) -> Unit)? = null,
    onOpenArtist: ((String) -> Unit)? = null
) {
    var menuOpen by remember { mutableStateOf(false) }
    var liked by remember(song.id) { mutableStateOf(Repository.isLiked(song)) }
    val scope = rememberCoroutineScope()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (leading != null) {
            if (isCurrent && isPlaying) {
                EqIndicator(modifier = Modifier.padding(end = 16.dp))
            } else {
                Box(modifier = Modifier.padding(end = 16.dp), contentAlignment = Alignment.Center) {
                    leading()
                }
            }
        }
        if (showArt) {
            if (artUrl != null) {
                RemoteImage(
                    url = artUrl,
                    modifier = Modifier.size(48.dp).padding(end = 0.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            } else {
                ArtPlaceholder(modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                song.title,
                style = CmTypography.bodyLarge,
                color = if (isCurrent) GreenBright else White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!song.artist.isNullOrBlank()) {
                Text(
                    song.artist,
                    style = CmTypography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = TextSecondary)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Play next", style = CmTypography.bodyMedium, color = White) },
                    onClick = { menuOpen = false; PlayerConnection.playNext(song) }
                )
                DropdownMenuItem(
                    text = { Text("Add to queue", style = CmTypography.bodyMedium, color = White) },
                    onClick = { menuOpen = false; PlayerConnection.enqueue(song) }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            if (liked) "Remove from Liked Songs" else "Save to Liked Songs",
                            style = CmTypography.bodyMedium,
                            color = White
                        )
                    },
                    onClick = {
                        menuOpen = false
                        scope.launch {
                            Repository.toggleLike(song)
                            liked = Repository.isLiked(song)
                        }
                    }
                )
                if (song.albumId != null && onOpenAlbum != null) {
                    DropdownMenuItem(
                        text = { Text("Go to album", style = CmTypography.bodyMedium, color = White) },
                        onClick = { menuOpen = false; onOpenAlbum(song.albumId!!) }
                    )
                }
                if (song.artistId != null && onOpenArtist != null) {
                    DropdownMenuItem(
                        text = { Text("Go to artist", style = CmTypography.bodyMedium, color = White) },
                        onClick = { menuOpen = false; onOpenArtist(song.artistId!!) }
                    )
                }
            }
        }
    }
}

// ---------- play button ----------

@Composable
fun PlayFab(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 56.dp, playing: Boolean = false) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(GreenBright)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (playing) "Pause" else "Play",
            tint = Color(0xFF000000),
            modifier = Modifier.size(size * 0.45f)
        )
    }
}

// ---------- like button ----------

@Composable
fun LikeButton(liked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, tint: Color = White) {
    IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (liked) "Unlike" else "Like",
            tint = if (liked) GreenBright else tint
        )
    }
}
