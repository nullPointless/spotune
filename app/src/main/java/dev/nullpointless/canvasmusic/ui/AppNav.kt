package dev.nullpointless.canvasmusic.ui

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.nullpointless.canvasmusic.R
import dev.nullpointless.canvasmusic.data.Repository
import dev.nullpointless.canvasmusic.playback.PlayerConnection
import dev.nullpointless.canvasmusic.screens.AlbumScreen
import dev.nullpointless.canvasmusic.screens.ArtistScreen
import dev.nullpointless.canvasmusic.screens.GenreScreen
import dev.nullpointless.canvasmusic.screens.HomeScreen
import dev.nullpointless.canvasmusic.screens.LibraryScreen
import dev.nullpointless.canvasmusic.screens.LikedScreen
import dev.nullpointless.canvasmusic.screens.LoginScreen
import dev.nullpointless.canvasmusic.screens.NowPlayingScreen
import dev.nullpointless.canvasmusic.screens.PlaylistScreen
import dev.nullpointless.canvasmusic.screens.QueueScreen
import dev.nullpointless.canvasmusic.screens.SearchScreen

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main?tab={tab}"
    const val MAIN_PATTERN = "main?tab={tab}"
    const val ALBUM = "album/{id}"
    const val ARTIST = "artist/{id}"
    const val PLAYLIST = "playlist/{id}"
    const val LIKED = "liked"
    const val GENRE = "genre/{name}"
    const val NOW_PLAYING = "nowplaying"
    const val QUEUE = "queue"

    fun main(tab: String = "home") = "main?tab=$tab"
    fun album(id: String) = "album/$id"
    fun artist(id: String) = "artist/$id"
    fun playlist(id: String) = "playlist/$id"
    fun genre(name: String) = "genre/${Uri.encode(name)}"
}

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val signedIn by Repository.signedIn.collectAsState()

    LaunchedEffect(signedIn) {
        when (signedIn) {
            false -> nav.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
            true -> if (route == Routes.LOGIN) {
                nav.navigate(Routes.main()) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            }
            null -> Unit
        }
    }

    val chromeRoutes = remember {
        setOf(Routes.MAIN_PATTERN, Routes.ALBUM, Routes.ARTIST, Routes.PLAYLIST, Routes.LIKED, Routes.GENRE, Routes.QUEUE)
    }
    val showChrome = route in chromeRoutes

    Scaffold(
        containerColor = Bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showChrome) {
                Column {
                    MiniPlayer(onOpen = { nav.navigate(Routes.NOW_PLAYING) })
                    val tab = backStackEntry?.arguments?.getString("tab") ?: "home"
                    BottomBar(currentTab = tab) { t ->
                        nav.navigate(Routes.main(t)) {
                            popUpTo(Routes.MAIN_PATTERN) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = Routes.LOGIN,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            enterTransition = { fadeIn(androidx.compose.animation.core.tween(180)) },
            exitTransition = { fadeOut(androidx.compose.animation.core.tween(180)) }
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onDone = {
                        nav.navigate(Routes.main()) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                Routes.MAIN_PATTERN,
                arguments = listOf(navArgument("tab") { defaultValue = "home" })
            ) {
                val tab = it.arguments?.getString("tab") ?: "home"
                MainTabs(
                    tab = tab,
                    onOpenAlbum = { id -> nav.navigate(Routes.album(id)) },
                    onOpenArtist = { id -> nav.navigate(Routes.artist(id)) },
                    onOpenPlaylist = { id -> nav.navigate(Routes.playlist(id)) },
                    onOpenLiked = { nav.navigate(Routes.LIKED) },
                    onOpenGenre = { name -> nav.navigate(Routes.genre(name)) }
                )
            }

            composable(Routes.ALBUM) { entry ->
                AlbumScreen(
                    albumId = entry.arguments?.getString("id") ?: "",
                    onOpenArtist = { id -> nav.navigate(Routes.artist(id)) },
                    onBack = { nav.popBackStack() }
                )
            }

            composable(Routes.ARTIST) { entry ->
                ArtistScreen(
                    artistId = entry.arguments?.getString("id") ?: "",
                    onOpenAlbum = { id -> nav.navigate(Routes.album(id)) },
                    onBack = { nav.popBackStack() }
                )
            }

            composable(Routes.PLAYLIST) { entry ->
                PlaylistScreen(
                    playlistId = entry.arguments?.getString("id") ?: "",
                    onOpenArtist = { id -> nav.navigate(Routes.artist(id)) },
                    onBack = { nav.popBackStack() }
                )
            }

            composable(Routes.LIKED) {
                LikedScreen(onBack = { nav.popBackStack() })
            }

            composable(Routes.GENRE) { entry ->
                GenreScreen(
                    genreName = entry.arguments?.getString("name") ?: "",
                    onOpenArtist = { id -> nav.navigate(Routes.artist(id)) },
                    onBack = { nav.popBackStack() }
                )
            }

            composable(
                Routes.NOW_PLAYING,
                enterTransition = {
                    slideInVertically(androidx.compose.animation.core.tween(280)) { it } +
                        fadeIn(androidx.compose.animation.core.tween(280))
                },
                exitTransition = {
                    slideOutVertically(androidx.compose.animation.core.tween(240)) { it } +
                        fadeOut(androidx.compose.animation.core.tween(240))
                }
            ) {
                NowPlayingScreen(
                    onOpenArtist = { id -> nav.navigate(Routes.artist(id)) },
                    onBack = { nav.popBackStack() },
                    onOpenQueue = { nav.navigate(Routes.QUEUE) }
                )
            }

            composable(Routes.QUEUE) {
                QueueScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}

@Composable
fun MainTabs(
    tab: String,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenLiked: () -> Unit,
    onOpenGenre: (String) -> Unit
) {
    val tabIndex = when (tab) {
        "search" -> 1
        "library" -> 2
        else -> 0
    }
    Box(Modifier.fillMaxSize()) {
        when (tabIndex) {
            0 -> HomeScreen(onOpenAlbum = onOpenAlbum, onOpenArtist = onOpenArtist, onOpenPlaylist = onOpenPlaylist, onOpenLiked = onOpenLiked)
            1 -> SearchScreen(onOpenAlbum = onOpenAlbum, onOpenArtist = onOpenArtist, onOpenGenre = onOpenGenre)
            else -> LibraryScreen(onOpenAlbum = onOpenAlbum, onOpenArtist = onOpenArtist, onOpenPlaylist = onOpenPlaylist, onOpenLiked = onOpenLiked, onOpenGenre = onOpenGenre)
        }
    }
}

@Composable
fun MiniPlayer(onOpen: () -> Unit) {
    val item by PlayerConnection.nowPlaying.collectAsState()
    val playing by PlayerConnection.isPlaying.collectAsState()
    val pos by PlayerConnection.positionMs.collectAsState()
    val dur by PlayerConnection.durationMs.collectAsState()
    val current = item ?: return
    val progress = if (dur > 0) pos.toFloat() / dur else 0f

    Column(Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2F2F2F))
                .clickable { onOpen() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
            ) {
                val art = current.mediaMetadata.artworkUri?.toString()
                RemoteImage(
                    url = art,
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        current.mediaMetadata.title?.toString() ?: "",
                        style = CmTypography.bodyLarge,
                        color = White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        current.mediaMetadata.artist?.toString() ?: "",
                        style = CmTypography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { PlayerConnection.togglePlayPause() }) {
                    Icon(
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                        tint = White
                    )
                }
                IconButton(onClick = { PlayerConnection.next() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = White)
                }
            }
            ThinProgress(
                progress = progress,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 4.dp)
            )
        }
    }
}

private data class TabSpec(val id: String, val icon: ImageVector, val label: String)

@Composable
fun BottomBar(currentTab: String, onSelect: (String) -> Unit) {
    val tabs = remember {
        listOf(
            TabSpec("home", Icons.Filled.Home, "Home"),
            TabSpec("search", Icons.Filled.Search, "Search"),
            TabSpec("library", Icons.Filled.LibraryMusic, "Your Library")
        )
    }
    Column(Modifier.background(Color(0xFF0A0A0A))) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(58.dp)
        ) {
            tabs.forEach { tab ->
                val selected = currentTab == tab.id
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(tab.id) }
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) White else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        tab.label,
                        style = CmTypography.labelSmall.copy(fontSize = 10.sp),
                        color = if (selected) White else TextSecondary
                    )
                }
            }
        }
    }
}
