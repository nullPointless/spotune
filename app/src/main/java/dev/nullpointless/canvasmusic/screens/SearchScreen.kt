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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nullpointless.canvasmusic.api.Artist
import dev.nullpointless.canvasmusic.api.GenreValue
import dev.nullpointless.canvasmusic.api.SearchResult
import dev.nullpointless.canvasmusic.data.Repository
import dev.nullpointless.canvasmusic.playback.PlayerConnection
import dev.nullpointless.canvasmusic.ui.CmTypography
import dev.nullpointless.canvasmusic.ui.EmptySection
import dev.nullpointless.canvasmusic.ui.LoadingSection
import dev.nullpointless.canvasmusic.ui.RemoteImage
import dev.nullpointless.canvasmusic.ui.SectionTitle
import dev.nullpointless.canvasmusic.ui.TextSecondary
import dev.nullpointless.canvasmusic.ui.White
import kotlinx.coroutines.delay

private val genreColors = listOf(
    0xFFDC148C, 0xFF7358FF, 0xFFE13300, 0xFF158A08, 0xFF0D73EC,
    0xFF8400E7, 0xFFE91429, 0xFF509BF5, 0xFF1E3264, 0xFFA56752,
    0xFF537AA1, 0xFFB02897
)

private val fallbackGenres = listOf(
    "Rock", "Pop", "Jazz", "Electronic", "Hip Hop", "Classical",
    "Metal", "Folk", "R&B", "Ambient", "Indie", "Soundtrack"
)

@Composable
fun SearchScreen(
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenGenre: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<SearchResult?>(null) }
    var searching by remember { mutableStateOf(false) }
    var genres by remember { mutableStateOf<List<GenreValue>?>(null) }
    val nowItem by PlayerConnection.nowPlaying.collectAsState()
    val playing by PlayerConnection.isPlaying.collectAsState()

    LaunchedEffect(Unit) {
        genres = runCatching { Repository.genres() }
            .getOrNull()
            ?.filter { it.songCount != null && it.songCount > 0 }
            ?.take(16)
            ?: fallbackGenres.map { GenreValue(it, 1, 1) }
    }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            result = null
            searching = false
        } else {
            delay(300)
            searching = true
            runCatching { result = Repository.search(query) }
            searching = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                "Search",
                style = CmTypography.headlineLarge,
                color = White,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 16.dp)
            )
        }

        item {
            SearchField(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (query.isBlank()) {
            val genreList = genres
            if (genreList == null) {
                item { LoadingSection() }
            } else {
                item { SectionTitle("Browse all", Modifier.padding(top = 22.dp)) }
                items(genreList.chunked(2)) { pair ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        GenreCard(
                            name = pair[0].value,
                            color = Color(genreColors[(pair[0].value.hashCode().let { if (it < 0) -it else it }) % genreColors.size]),
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenGenre(pair[0].value) }
                        )
                        if (pair.size > 1) {
                            GenreCard(
                                name = pair[1].value,
                                color = Color(genreColors[(pair[1].value.hashCode().let { if (it < 0) -it else it } * 7 + 3) % genreColors.size]),
                                modifier = Modifier.weight(1f),
                                onClick = { onOpenGenre(pair[1].value) }
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        } else if (searching && result == null) {
            item { LoadingSection() }
        } else {
            val r = result
            if (r == null) {
                item { EmptySection("Search your music") }
            } else if (r.artist.isEmpty() && r.album.isEmpty() && r.song.isEmpty()) {
                item { EmptySection("No results found for \"$query\"") }
            } else {
                // Top result
                val topArtist = r.artist.firstOrNull()
                val topAlbum = r.album.firstOrNull()
                val topSong = r.song.firstOrNull()
                val hasTop = topArtist != null || topAlbum != null
                if (hasTop) {
                    item { SectionTitle("Top result", Modifier.padding(top = 22.dp)) }
                    item {
                        if (topArtist != null) {
                            TopResultCard(
                                title = topArtist.name,
                                typeLabel = "Artist",
                                artUrl = Repository.api?.coverArtUrl(topArtist.coverArt ?: topArtist.id),
                                circular = true,
                                onClick = { onOpenArtist(topArtist.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        } else if (topAlbum != null) {
                            TopResultCard(
                                title = topAlbum.name,
                                typeLabel = "Album",
                                artUrl = Repository.api?.coverArtUrl(topAlbum.coverArt ?: topAlbum.id),
                                circular = false,
                                subtitle = "Album • ${topAlbum.artist ?: ""}",
                                onClick = { onOpenAlbum(topAlbum.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                if (r.song.isNotEmpty()) {
                    item { SectionTitle("Songs", Modifier.padding(top = 22.dp)) }
                    itemsIndexed(r.song.take(6)) { idx, song ->
                        dev.nullpointless.canvasmusic.ui.TrackRow(
                            song = song,
                            isCurrent = nowItem?.mediaId == song.id,
                            isPlaying = playing,
                            showArt = true,
                            artUrl = Repository.api?.coverArtUrl(song.coverArt ?: song.albumId),
                            onPlay = {
                                PlayerConnection.playSongs(r.song, idx, contextName = "Search • $query")
                            },
                            onMore = { },
                            onOpenAlbum = onOpenAlbum,
                            onOpenArtist = onOpenArtist
                        )
                    }
                }

                if (r.artist.isNotEmpty()) {
                    item { SectionTitle("Artists", Modifier.padding(top = 22.dp)) }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            items(r.artist, key = { it.id }) { artist ->
                                ArtistCircle(artist = artist, onClick = { onOpenArtist(artist.id) })
                            }
                        }
                    }
                }

                if (r.album.isNotEmpty()) {
                    item { SectionTitle("Albums", Modifier.padding(top = 22.dp)) }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            items(r.album, key = { it.id }) { album ->
                                MediaCard(
                                    title = album.name,
                                    subtitle = album.artist ?: "",
                                    artUrl = Repository.api?.coverArtUrl(album.coverArt ?: album.id),
                                    onClick = { onOpenAlbum(album.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (query.isBlank()) Color(0xFF404040) else White)
            .padding(horizontal = 14.dp)
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = "Search",
            tint = if (query.isBlank()) Color(0xFFDCDCDC) else Color(0xFF7A7A7A),
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = CmTypography.bodyLarge.copy(
                fontSize = 16.sp,
                color = if (query.isBlank()) White else Color(0xFF191919)
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            cursorBrush = SolidColor(if (query.isBlank()) White else Color(0xFF191919)),
            modifier = Modifier.weight(1f).padding(vertical = 13.dp),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        "What do you want to listen to?",
                        style = CmTypography.bodyLarge.copy(fontSize = 16.sp),
                        color = if (query.isBlank()) Color(0xFFDCDCDC) else Color(0xFF7A7A7A)
                    )
                } else {
                    inner()
                }
            }
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Clear",
                    tint = Color(0xFF7A7A7A),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun GenreCard(name: String, color: Color, modifier: Modifier = Modifier, onClick: (String) -> Unit = {}) {
    Box(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .clickable { onClick(name) }
    ) {
        Text(
            name,
            style = CmTypography.bodyLarge.copy(fontSize = 16.sp),
            color = White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        )
        Icon(
            Icons.Filled.MusicNote,
            contentDescription = null,
            tint = Color(0x66000000),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 6.dp)
                .size(64.dp)
        )
    }
}

@Composable
fun TopResultCard(
    title: String,
    typeLabel: String,
    artUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    circular: Boolean = false,
    subtitle: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF2A2A2A))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        RemoteImage(
            url = artUrl,
            modifier = Modifier.size(84.dp),
            shape = if (circular) CircleShape else RoundedCornerShape(4.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                title,
                style = CmTypography.bodyLarge.copy(fontSize = 19.sp),
                color = White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = CmTypography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                typeLabel,
                style = CmTypography.labelSmall.copy(fontSize = 10.sp),
                color = White,
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF535353))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
fun ArtistCircle(artist: Artist, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(104.dp).clickable { onClick() }
    ) {
        RemoteImage(
            url = Repository.api?.coverArtUrl(artist.coverArt ?: artist.id),
            modifier = Modifier.size(96.dp),
            shape = CircleShape
        )
        Spacer(Modifier.height(8.dp))
        Text(
            artist.name,
            style = CmTypography.bodyMedium,
            color = White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
