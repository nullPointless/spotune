package dev.nullpointless.canvasmusic.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nullpointless.canvasmusic.data.Repository
import dev.nullpointless.canvasmusic.playback.PlayerConnection
import dev.nullpointless.canvasmusic.ui.Bg
import dev.nullpointless.canvasmusic.ui.CmTypography
import dev.nullpointless.canvasmusic.ui.ErrorSection
import dev.nullpointless.canvasmusic.ui.GreenBright
import dev.nullpointless.canvasmusic.ui.LState
import dev.nullpointless.canvasmusic.ui.LikeButton
import dev.nullpointless.canvasmusic.ui.LoadingSection
import dev.nullpointless.canvasmusic.ui.PlayFab
import dev.nullpointless.canvasmusic.ui.RemoteImage
import dev.nullpointless.canvasmusic.ui.TextSecondary
import dev.nullpointless.canvasmusic.ui.TileBg
import dev.nullpointless.canvasmusic.ui.TrackRow
import dev.nullpointless.canvasmusic.ui.White
import dev.nullpointless.canvasmusic.ui.darken
import dev.nullpointless.canvasmusic.ui.fmtSec
import dev.nullpointless.canvasmusic.ui.fmtTotal
import dev.nullpointless.canvasmusic.ui.loadState
import dev.nullpointless.canvasmusic.ui.rememberDominantColor
import kotlinx.coroutines.launch

@Composable
fun AlbumScreen(
    albumId: String,
    onOpenArtist: (String) -> Unit,
    onBack: () -> Unit
) {
    when (val st = loadState(albumId) { Repository.album(albumId) }) {
        is LState.Loading -> LoadingSection(Modifier.fillMaxSize())
        is LState.Failed -> Column(Modifier.fillMaxSize()) {
            IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
            }
            ErrorSection(st.message)
        }
        is LState.Ready -> {
            val album = st.data
            val artUrl = Repository.api?.coverArtUrl(album.coverArt ?: album.id)
            val dominant = rememberDominantColor(artUrl)
            var shuffle by remember(album.id) { mutableStateOf(false) }
            var liked by remember(album.id) { mutableStateOf(album.starred != null) }
            val scope = rememberCoroutineScope()
            val nowItem by PlayerConnection.nowPlaying.collectAsState()
            val playing by PlayerConnection.isPlaying.collectAsState()

            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to dominant.darken(0.15f),
                                    0.75f to Bg,
                                    1f to Bg
                                )
                            )
                        )
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().statusBarsPadding()
                        ) {
                            IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
                            }
                        }
                    }
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.padding(horizontal = 24.dp)) {
                                RemoteImage(
                                    url = artUrl,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .shadow(14.dp, RoundedCornerShape(8.dp))
                                        .clip(RoundedCornerShape(8.dp)),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                album.name,
                                style = CmTypography.titleLarge.copy(fontSize = 24.sp),
                                color = White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                album.artist ?: "",
                                style = CmTypography.bodyLarge.copy(fontSize = 16.sp),
                                color = White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                buildString {
                                    album.year?.let { append(it) }
                                    append(" • ")
                                    append("${album.songCount ?: album.song.size} songs")
                                    album.duration?.let {
                                        if (it > 0) {
                                            append(", ")
                                            append(fmtTotal(it))
                                        }
                                    }
                                },
                                style = CmTypography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                            ) {
                                PlayFab(
                                    onClick = {
                                        PlayerConnection.playSongs(
                                            album.song, 0, shuffle = shuffle, contextName = album.name
                                        )
                                    },
                                    playing = playing && nowItem?.mediaMetadata?.albumTitle == album.name
                                )
                                IconButton(
                                    onClick = {
                                        shuffle = !shuffle
                                        PlayerConnection.setShuffle(shuffle)
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(TileBg)
                                ) {
                                    Icon(
                                        Icons.Filled.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = if (shuffle) GreenBright else White
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                LikeButton(
                                    liked = liked,
                                    onToggle = {
                                        scope.launch {
                                            // Pass the CURRENT liked state — album.starred
                                            // is a stale snapshot from the original fetch and
                                            // made un-liking impossible.
                                            liked = Repository.toggleStarItem(album.id, liked)
                                        }
                                    }
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    itemsIndexed(album.song, key = { _, s -> s.id }) { idx, song ->
                        val num = song.track ?: (idx + 1)
                        TrackRow(
                            song = song,
                            isCurrent = nowItem?.mediaId == song.id,
                            isPlaying = playing,
                            onPlay = {
                                PlayerConnection.playSongs(
                                    album.song, idx, shuffle = shuffle, contextName = album.name
                                )
                            },
                            onMore = { },
                            leading = {
                                Text(
                                    "$num",
                                    style = CmTypography.bodyMedium,
                                    color = TextSecondary
                                )
                            },
                            trailing = {
                                Text(
                                    fmtSec(song.duration ?: 0),
                                    style = CmTypography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(end = 2.dp)
                                )
                            },
                            onOpenArtist = onOpenArtist
                        )
                    }
                }
            }
        }
    }
}
