package dev.nullpointless.canvasmusic.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import dev.nullpointless.canvasmusic.ui.LoadingSection
import dev.nullpointless.canvasmusic.ui.PlayFab
import dev.nullpointless.canvasmusic.ui.RemoteImage
import dev.nullpointless.canvasmusic.ui.SectionTitle
import dev.nullpointless.canvasmusic.ui.TextSecondary
import dev.nullpointless.canvasmusic.ui.TileBg
import dev.nullpointless.canvasmusic.ui.TrackRow
import dev.nullpointless.canvasmusic.ui.White
import dev.nullpointless.canvasmusic.ui.darken
import dev.nullpointless.canvasmusic.ui.fmtCount
import dev.nullpointless.canvasmusic.ui.loadState
import dev.nullpointless.canvasmusic.ui.rememberDominantColor

@Composable
fun ArtistScreen(
    artistId: String,
    onOpenAlbum: (String) -> Unit,
    onBack: () -> Unit
) {
    when (val st = loadState(artistId) { Repository.artist(artistId) }) {
        is LState.Loading -> LoadingSection(Modifier.fillMaxSize())
        is LState.Failed -> Column(Modifier.fillMaxSize()) {
            IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
            }
            ErrorSection(st.message)
        }
        is LState.Ready -> {
            val artist = st.data
            val artUrl = Repository.api?.coverArtUrl(artist.coverArt ?: artist.id)
            val topSt = loadState(artist.id) { Repository.topSongs(artist.name) }
            val top = (topSt as? LState.Ready)?.data ?: emptyList()
            val dominant = rememberDominantColor(artUrl)
            val nowItem by PlayerConnection.nowPlaying.collectAsState()
            val playing by PlayerConnection.isPlaying.collectAsState()

            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to dominant.darken(0.25f),
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
                        Box(Modifier.fillMaxWidth().height(300.dp)) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.statusBarsPadding().align(Alignment.TopStart).padding(4.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
                            }
                            RemoteImage(
                                url = artUrl,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .align(Alignment.Center),
                                shape = RoundedCornerShape(0.dp)
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0x00121212), Bg)
                                        )
                                    )
                            )
                            Text(
                                artist.name,
                                style = CmTypography.headlineLarge.copy(fontSize = 34.sp),
                                color = White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 16.dp, bottom = 10.dp)
                            )
                        }
                    }
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            PlayFab(
                                onClick = {
                                    if (top.isNotEmpty()) {
                                        PlayerConnection.playSongs(top, 0, contextName = artist.name)
                                    } else if (artist.album.isNotEmpty()) {
                                        val first = artist.album.first()
                                        PlayerConnection.playSongs(
                                            Repository.album(first.id).song,
                                            0,
                                            contextName = artist.name
                                        )
                                    }
                                },
                                playing = playing && nowItem?.mediaMetadata?.artist == artist.name
                            )
                            IconButton(
                                onClick = {
                                    if (top.isNotEmpty()) {
                                        PlayerConnection.playSongs(
                                            top, 0, shuffle = true, contextName = artist.name
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
                                    contentDescription = "Shuffle play",
                                    tint = GreenBright
                                )
                            }
                        }
                    }

                    if (top.isNotEmpty()) {
                        item { SectionTitle("Popular", Modifier.padding(top = 22.dp)) }
                        itemsIndexed(top.take(5), key = { _, s -> "top-${s.id}" }) { idx, song ->
                            TrackRow(
                                song = song,
                                isCurrent = nowItem?.mediaId == song.id,
                                isPlaying = playing,
                                showArt = true,
                                artUrl = Repository.api?.coverArtUrl(song.coverArt ?: song.albumId, 300),
                                onPlay = {
                                    PlayerConnection.playSongs(top, idx, contextName = artist.name)
                                },
                                onMore = { },
                                leading = {
                                    Text(
                                        "${idx + 1}",
                                        style = CmTypography.bodyMedium,
                                        color = TextSecondary
                                    )
                                },
                                trailing = {
                                    Text(
                                        "${fmtCount(song.playCount)} plays",
                                        style = CmTypography.bodySmall,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(end = 2.dp)
                                    )
                                }
                            )
                        }
                    }

                    if (artist.album.isNotEmpty()) {
                        item { SectionTitle("Discography", Modifier.padding(top = 22.dp)) }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                items(artist.album, key = { it.id }) { album ->
                                    MediaCard(
                                        title = album.name,
                                        subtitle = album.year?.toString() ?: "",
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
}
