package dev.nullpointless.canvasmusic.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import dev.nullpointless.canvasmusic.api.Playlist
import dev.nullpointless.canvasmusic.api.Song
import dev.nullpointless.canvasmusic.data.HomeData
import dev.nullpointless.canvasmusic.data.Repository
import dev.nullpointless.canvasmusic.ui.ArtPlaceholder
import dev.nullpointless.canvasmusic.ui.Bg
import dev.nullpointless.canvasmusic.ui.CmTypography
import dev.nullpointless.canvasmusic.ui.EmptySection
import dev.nullpointless.canvasmusic.ui.ErrorSection
import dev.nullpointless.canvasmusic.ui.Green
import dev.nullpointless.canvasmusic.ui.LoadingSection
import dev.nullpointless.canvasmusic.ui.RemoteImage
import dev.nullpointless.canvasmusic.ui.SectionTitle
import dev.nullpointless.canvasmusic.ui.TextSecondary
import dev.nullpointless.canvasmusic.ui.TileBg
import dev.nullpointless.canvasmusic.ui.White
import java.util.Calendar

@Composable
fun HomeScreen(
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenLiked: () -> Unit
) {
    var data by remember { mutableStateOf<HomeData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        loading = data == null
        error = null
        try {
            data = Repository.home()
        } catch (e: Exception) {
            error = e.message ?: "Failed to load"
        }
        loading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            val greeting = remember {
                val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                when {
                    h < 5 -> "Good night"
                    h < 12 -> "Good morning"
                    h < 18 -> "Good afternoon"
                    else -> "Good evening"
                }
            }
            Text(
                greeting,
                style = CmTypography.headlineLarge,
                color = White,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 16.dp)
            )
        }

        val d = data
        when {
            loading && d == null -> item { LoadingSection() }
            error != null && d == null -> item {
                ErrorSection(message = error ?: "") { reloadKey++ }
            }
            d != null -> {
                if (d.recent.isEmpty() && d.playlists.isEmpty() && d.liked.isEmpty() && d.random.isEmpty() && d.frequent.isEmpty()) {
                    item {
                        EmptySection("Nothing here yet. Play some music on your Navidrome server and it will show up here.")
                    }
                }

                // Spotify-style shortcut tiles
                if (d.recent.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            d.recent.take(6).chunked(2).forEach { pair ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp)
                                ) {
                                    HomeTile(album = pair[0], onClick = onOpenAlbum, modifier = Modifier.weight(1f))
                                    if (pair.size > 1) {
                                        HomeTile(album = pair[1], onClick = onOpenAlbum, modifier = Modifier.weight(1f))
                                    } else {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                if (d.recent.isNotEmpty()) {
                    item { SectionTitle("Recently played", Modifier.padding(top = 26.dp)) }
                    item {
                        AlbumRow(
                            albums = d.recent,
                            onClick = onOpenAlbum,
                            subtitle = { it.artist ?: "" }
                        )
                    }
                }

                if (d.liked.isNotEmpty()) {
                    item { SectionTitle("Liked songs", Modifier.padding(top = 26.dp)) }
                    item {
                        SongRow(songs = d.liked, onOpenAlbum = onOpenAlbum, onOpenArtist = onOpenArtist)
                    }
                }

                if (d.random.isNotEmpty()) {
                    item { SectionTitle("Made for you", Modifier.padding(top = 26.dp)) }
                    item {
                        SongRow(songs = d.random, onOpenAlbum = onOpenAlbum, onOpenArtist = onOpenArtist)
                    }
                }

                if (d.playlists.isNotEmpty()) {
                    item { SectionTitle("Your playlists", Modifier.padding(top = 26.dp)) }
                    item {
                        PlaylistRow(playlists = d.playlists, onClick = onOpenPlaylist)
                    }
                }

                if (d.frequent.isNotEmpty()) {
                    item { SectionTitle("Most played", Modifier.padding(top = 26.dp)) }
                    item {
                        AlbumRow(
                            albums = d.frequent,
                            onClick = onOpenAlbum,
                            subtitle = { it.artist ?: "" }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTile(album: Album, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(TileBg)
            .clickable { onClick(album.id) }
    ) {
        RemoteImage(
            url = Repository.api?.coverArtUrl(album.coverArt ?: album.id, 300),
            modifier = Modifier.size(58.dp)
        )
        Text(
            album.name,
            style = CmTypography.labelLarge,
            color = White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}

@Composable
fun MediaCard(
    title: String,
    subtitle: String,
    artUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 152.dp
) {
    Column(modifier = modifier.width(width).clickable { onClick() }) {
        Box {
            if (artUrl != null) {
                RemoteImage(
                    url = artUrl,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    shape = RoundedCornerShape(6.dp)
                )
            } else {
                ArtPlaceholder(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    shape = RoundedCornerShape(6.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            style = CmTypography.bodyMedium.copy(fontSize = 15.sp),
            color = White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                style = CmTypography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AlbumRow(albums: List<Album>, onClick: (String) -> Unit, subtitle: (Album) -> String) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            MediaCard(
                title = album.name,
                subtitle = subtitle(album),
                artUrl = Repository.api?.coverArtUrl(album.coverArt ?: album.id),
                onClick = { onClick(album.id) }
            )
        }
    }
}

@Composable
private fun SongRow(songs: List<Song>, onOpenAlbum: (String) -> Unit, onOpenArtist: (String) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(songs, key = { it.id }) { song ->
            MediaCard(
                title = song.title,
                subtitle = song.artist ?: "",
                artUrl = Repository.api?.coverArtUrl(song.coverArt ?: song.albumId),
                onClick = {
                    dev.nullpointless.canvasmusic.playback.PlayerConnection.playSongs(
                        songs, songs.indexOf(song), contextName = "Canvas Music"
                    )
                }
            )
        }
    }
}

@Composable
private fun PlaylistRow(playlists: List<Playlist>, onClick: (String) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(playlists, key = { it.id }) { playlist ->
            MediaCard(
                title = playlist.name,
                subtitle = "Playlist • ${playlist.songCount ?: 0} songs",
                artUrl = Repository.api?.coverArtUrl(playlist.coverArt),
                onClick = { onClick(playlist.id) }
            )
        }
    }
}
