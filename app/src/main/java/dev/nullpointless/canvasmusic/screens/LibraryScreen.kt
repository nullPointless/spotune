package dev.nullpointless.canvasmusic.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nullpointless.canvasmusic.api.Album
import dev.nullpointless.canvasmusic.api.Artist
import dev.nullpointless.canvasmusic.api.Playlist
import dev.nullpointless.canvasmusic.api.Song
import dev.nullpointless.canvasmusic.data.Repository
import dev.nullpointless.canvasmusic.playback.PlayerConnection
import dev.nullpointless.canvasmusic.ui.ArtPlaceholder
import dev.nullpointless.canvasmusic.ui.CmTypography
import dev.nullpointless.canvasmusic.ui.EmptySection
import dev.nullpointless.canvasmusic.ui.ErrorSection
import dev.nullpointless.canvasmusic.ui.LState
import dev.nullpointless.canvasmusic.ui.LoadingSection
import dev.nullpointless.canvasmusic.ui.RemoteImage
import dev.nullpointless.canvasmusic.ui.TextSecondary
import dev.nullpointless.canvasmusic.ui.TrackRow
import dev.nullpointless.canvasmusic.ui.White
import dev.nullpointless.canvasmusic.ui.loadState
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenLiked: () -> Unit,
    onOpenGenre: (String) -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var reloadKey by remember { mutableStateOf(0) }
    var showLogout by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { runCatching { Repository.refreshLiked() } }
    val liked by Repository.liked.collectAsState()

    val playlistsSt = if (tab == 0) loadState(reloadKey) { Repository.playlists() } else null
    val artistsSt = if (tab == 1) loadState(reloadKey) { Repository.artistsAll() } else null
    val albumsSt = if (tab == 2) loadState(reloadKey) { Repository.albumsAll() } else null

    val nowItem by PlayerConnection.nowPlaying.collectAsState()
    val playing by PlayerConnection.isPlaying.collectAsState()

    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            title = { Text("Log out?", color = White) },
            text = { Text("You will need to sign in again to access your server.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showLogout = false
                    scope.launch {
                        PlayerConnection.togglePlayPause()
                        Repository.signOut()
                    }
                }) { Text("Log out", color = Color(0xFFF15E6C)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogout = false }) { Text("Cancel", color = White) }
            },
            containerColor = Color(0xFF242424)
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 20.dp, bottom = 6.dp)
            ) {
                Text(
                    "Your Library",
                    style = CmTypography.headlineLarge,
                    color = White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showLogout = true }) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out", tint = TextSecondary)
                }
            }
        }

        item {
            val tabs = listOf("Playlists", "Artists", "Albums", "Songs")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                tabs.forEachIndexed { i, label ->
                    val selected = tab == i
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) Color(0xFF535353) else Color(0xFF2A2A2A))
                            .clickable { tab = i }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            label,
                            style = CmTypography.labelLarge.copy(fontSize = 13.sp),
                            color = White
                        )
                    }
                }
            }
        }

        when (tab) {
            0 -> PlaylistsList(
                st = playlistsSt,
                liked = liked,
                onOpenPlaylist = onOpenPlaylist,
                onOpenLiked = onOpenLiked
            )
            1 -> ArtistsList(st = artistsSt, onOpenArtist = onOpenArtist)
            2 -> AlbumsList(st = albumsSt, onOpenAlbum = onOpenAlbum)
            3 -> LikedSongsList(
                songs = liked,
                nowItem = nowItem,
                playing = playing
            )
        }
    }
}

private fun <T> LazyListScope.stateSection(st: LState<T>?, failedRetry: (() -> Unit)? = null, ready: LazyListScope.(T) -> Unit) {
    when (st) {
        null, is LState.Loading -> item(key = "loading-${st.hashCode()}") { LoadingSection() }
        is LState.Failed -> item(key = "failed") {
            ErrorSection(st.message, onRetry = failedRetry)
        }
        is LState.Ready -> ready(st.data)
    }
}

private fun LazyListScope.PlaylistsList(
    st: LState<List<Playlist>>?,
    liked: List<Song>? ,
    onOpenPlaylist: (String) -> Unit,
    onOpenLiked: () -> Unit
) {
    stateSection(st) { playlists ->
        item(key = "liked-entry") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenLiked() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF4300EC), Color(0xFF93AAFD)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Liked Songs", style = CmTypography.bodyLarge, color = White)
                    Text(
                        "Playlist • ${liked?.size ?: 0} songs",
                        style = CmTypography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
        if (playlists.isEmpty()) {
            item(key = "empty-playlists") { EmptySection("No playlists on this server yet.") }
        }
        items(playlists, key = { "pl-${it.id}" }) { playlist ->
            PlaylistRow(playlist = playlist, onClick = { onOpenPlaylist(playlist.id) })
        }
    }
}

@Composable
private fun PlaylistRow(playlist: Playlist, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val artUrl = Repository.api?.coverArtUrl(playlist.coverArt)
        if (artUrl != null) {
            RemoteImage(url = artUrl, modifier = Modifier.size(56.dp), shape = RoundedCornerShape(4.dp))
        } else {
            ArtPlaceholder(modifier = Modifier.size(56.dp), icon = 26.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                playlist.name,
                style = CmTypography.bodyLarge,
                color = White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Playlist • ${playlist.songCount ?: playlist.entry.size} songs",
                style = CmTypography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun LazyListScope.ArtistsList(
    st: LState<List<Artist>>?,
    onOpenArtist: (String) -> Unit
) {
    stateSection(st) { artists ->
        val withAlbums = artists.filter { (it.albumCount ?: 0) > 0 }
        if (withAlbums.isEmpty()) {
            item(key = "empty-artists") { EmptySection("No artists found.") }
        }
        items(withAlbums, key = { "ar-${it.id}" }) { artist ->
            ArtistListRow(artist = artist, onClick = { onOpenArtist(artist.id) })
        }
    }
}

@Composable
private fun ArtistListRow(artist: Artist, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val artUrl = Repository.api?.coverArtUrl(artist.coverArt ?: artist.id, 300)
        if (artUrl != null) {
            RemoteImage(url = artUrl, modifier = Modifier.size(52.dp), shape = CircleShape)
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3A3A3A)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    artist.name.take(1).uppercase(),
                    style = CmTypography.labelLarge.copy(fontSize = 18.sp),
                    color = TextSecondary
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            artist.name,
            style = CmTypography.bodyLarge,
            color = White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun LazyListScope.AlbumsList(
    st: LState<List<Album>>?,
    onOpenAlbum: (String) -> Unit
) {
    stateSection(st) { albums ->
        if (albums.isEmpty()) {
            item(key = "empty-albums") { EmptySection("No albums found.") }
        }
        items(albums, key = { "al-${it.id}" }) { album ->
            AlbumListRow(album = album, onClick = { onOpenAlbum(album.id) })
        }
    }
}

@Composable
private fun AlbumListRow(album: Album, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        RemoteImage(
            url = Repository.api?.coverArtUrl(album.coverArt ?: album.id, 300),
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(4.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                album.name,
                style = CmTypography.bodyLarge,
                color = White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Album • ${album.artist ?: ""}",
                style = CmTypography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun LazyListScope.LikedSongsList(
    songs: List<dev.nullpointless.canvasmusic.api.Song>?,
    nowItem: androidx.media3.common.MediaItem?,
    playing: Boolean
) {
    when {
        songs == null -> item(key = "liked-loading") { LoadingSection() }
        songs.isEmpty() -> item(key = "liked-empty") {
            EmptySection("Songs you like will appear here.\nTap the heart on any song to save it.")
        }
        else -> itemsIndexed(songs, key = { _, s -> "lk-${s.id}" }) { idx, song ->
            TrackRow(
                song = song,
                isCurrent = nowItem?.mediaId == song.id,
                isPlaying = playing,
                showArt = true,
                artUrl = Repository.api?.coverArtUrl(song.coverArt ?: song.albumId, 300),
                onPlay = {
                    PlayerConnection.playSongs(songs, idx, contextName = "Liked Songs")
                },
                onMore = { }
            )
        }
    }
}
