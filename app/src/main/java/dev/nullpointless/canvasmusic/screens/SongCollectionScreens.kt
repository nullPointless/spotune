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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nullpointless.canvasmusic.api.Song
import dev.nullpointless.canvasmusic.data.Repository
import dev.nullpointless.canvasmusic.playback.PlayerConnection
import dev.nullpointless.canvasmusic.ui.ArtPlaceholder
import dev.nullpointless.canvasmusic.ui.Bg
import dev.nullpointless.canvasmusic.ui.CmTypography
import dev.nullpointless.canvasmusic.ui.EmptySection
import dev.nullpointless.canvasmusic.ui.ErrorSection
import dev.nullpointless.canvasmusic.ui.GreenBright
import dev.nullpointless.canvasmusic.ui.LState
import dev.nullpointless.canvasmusic.ui.LoadingSection
import dev.nullpointless.canvasmusic.ui.PlayFab
import dev.nullpointless.canvasmusic.ui.RemoteImage
import dev.nullpointless.canvasmusic.ui.TextSecondary
import dev.nullpointless.canvasmusic.ui.TileBg
import dev.nullpointless.canvasmusic.ui.TrackRow
import dev.nullpointless.canvasmusic.ui.White
import dev.nullpointless.canvasmusic.ui.fmtTotal
import dev.nullpointless.canvasmusic.ui.loadState

@Composable
fun PlaylistScreen(
    playlistId: String,
    onOpenArtist: (String) -> Unit,
    onBack: () -> Unit
) {
    when (val st = loadState(playlistId) { Repository.playlist(playlistId) }) {
        is LState.Loading -> LoadingSection(Modifier.fillMaxSize())
        is LState.Failed -> Column(Modifier.fillMaxSize()) {
            IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
            }
            ErrorSection(st.message)
        }
        is LState.Ready -> {
            val playlist = st.data
            SongCollectionScreen(
                title = playlist.name,
                typeLabel = "Playlist",
                subtitle = buildString {
                    playlist.owner?.let {
                        append(it)
                        append(" • ")
                    }
                    append("${playlist.songCount ?: playlist.entry.size} songs")
                    val total = playlist.duration ?: playlist.entry.sumOf { it.duration ?: 0L }
                    if (total > 0) {
                        append(", ")
                        append(fmtTotal(total))
                    }
                },
                artUrl = Repository.api?.coverArtUrl(playlist.coverArt),
                fallbackIcon = Icons.Filled.MusicNote,
                load = { playlist.entry },
                loadKey = playlist.id,
                onBack = onBack
            )
        }
    }
}

@Composable
fun LikedScreen(onBack: () -> Unit) {
    LaunchedEffect(Unit) { runCatching { Repository.refreshLiked() } }
    val liked by Repository.liked.collectAsState()

    val songs = liked
    when {
        songs == null -> LoadingSection(Modifier.fillMaxSize())
        else -> SongCollectionScreen(
            title = "Liked Songs",
            typeLabel = "Playlist",
            subtitle = "${songs.size} songs",
            artUrl = null,
            gradient = listOf(Color(0xFF4300EC), Color(0xFF93AAFD)),
            fallbackIcon = Icons.Filled.Favorite,
            load = { songs },
            loadKey = songs.size,
            onBack = onBack
        )
    }
}

@Composable
fun GenreScreen(
    genreName: String,
    onOpenArtist: (String) -> Unit,
    onBack: () -> Unit
) {
    SongCollectionScreen(
        title = genreName,
        typeLabel = "Genre",
        subtitle = "",
        artUrl = null,
        gradient = listOf(Color(0xFF509BF5), Color(0xFF1E3264)),
        fallbackIcon = Icons.Filled.MusicNote,
        load = { Repository.songsByGenre(genreName) },
        loadKey = genreName,
        onBack = onBack
    )
}

@Composable
fun SongCollectionScreen(
    title: String,
    typeLabel: String,
    subtitle: String,
    artUrl: String?,
    fallbackIcon: ImageVector,
    load: suspend () -> List<Song>,
    loadKey: Any?,
    onBack: () -> Unit,
    gradient: List<Color>? = null
) {
    when (val st = loadState(loadKey) { load() }) {
        is LState.Loading -> LoadingSection(Modifier.fillMaxSize())
        is LState.Failed -> Column(Modifier.fillMaxSize()) {
            IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
            }
            ErrorSection(st.message)
        }
        is LState.Ready -> {
            val songs = st.data
            var shuffle by remember { mutableStateOf(false) }
            val nowItem by PlayerConnection.nowPlaying.collectAsState()
            val playing by PlayerConnection.isPlaying.collectAsState()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().statusBarsPadding()
                        ) {
                            IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
                            }
                        }
                        Box(Modifier.padding(horizontal = 24.dp)) {
                            if (artUrl != null) {
                                RemoteImage(
                                    url = artUrl,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .shadow(14.dp, RoundedCornerShape(8.dp))
                                        .clip(RoundedCornerShape(8.dp)),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .shadow(14.dp, RoundedCornerShape(8.dp))
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Brush.linearGradient(gradient ?: listOf(TileBg, Bg))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        fallbackIcon,
                                        contentDescription = null,
                                        tint = White,
                                        modifier = Modifier.size(72.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            typeLabel,
                            style = CmTypography.labelSmall.copy(fontSize = 11.sp),
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            title,
                            style = CmTypography.titleLarge.copy(fontSize = 26.sp),
                            color = White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        if (subtitle.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                subtitle,
                                style = CmTypography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                        ) {
                            PlayFab(
                                onClick = {
                                    if (songs.isNotEmpty()) {
                                        PlayerConnection.playSongs(
                                            songs, 0, shuffle = shuffle, contextName = title
                                        )
                                    }
                                },
                                playing = playing
                            )
                            IconButton(
                                onClick = {
                                    shuffle = !shuffle
                                    PlayerConnection.setShuffle(shuffle)
                                    if (songs.isNotEmpty() && nowItem == null) {
                                        PlayerConnection.playSongs(
                                            songs, 0, shuffle = true, contextName = title
                                        )
                                    }
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
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                if (songs.isEmpty()) {
                    item { EmptySection("No songs in this collection.") }
                }
                itemsIndexed(songs, key = { _, s -> s.id }) { idx, song ->
                    TrackRow(
                        song = song,
                        isCurrent = nowItem?.mediaId == song.id,
                        isPlaying = playing,
                        showArt = true,
                        artUrl = Repository.api?.coverArtUrl(song.coverArt ?: song.albumId, 300),
                        onPlay = {
                            PlayerConnection.playSongs(songs, idx, shuffle = shuffle, contextName = title)
                        },
                        onMore = { }
                    )
                }
            }
        }
    }
}
