package dev.nullpointless.canvasmusic.data

import android.content.Context
import dev.nullpointless.canvasmusic.api.Album
import dev.nullpointless.canvasmusic.api.Artist
import dev.nullpointless.canvasmusic.api.GenreValue
import dev.nullpointless.canvasmusic.api.Playlist
import dev.nullpointless.canvasmusic.api.SearchResult
import dev.nullpointless.canvasmusic.api.Song
import dev.nullpointless.canvasmusic.api.SubsonicApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class HomeData(
    val recent: List<Album> = emptyList(),
    val frequent: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val liked: List<Song> = emptyList(),
    val random: List<Song> = emptyList()
)

object Repository {
    private lateinit var appContext: Context
    private lateinit var scope: CoroutineScope

    var api: SubsonicApi? = null
        private set

    /** null = not yet restored from storage, true = signed in, false = signed out */
    val signedIn = MutableStateFlow<Boolean?>(null)

    private val _liked = MutableStateFlow<List<Song>?>(null)
    val liked: StateFlow<List<Song>?> = _liked

    private val albumCache = ConcurrentHashMap<String, Album>()
    private val artistCache = ConcurrentHashMap<String, Artist>()
    private var artistsCache: List<Artist>? = null
    private var albumsCache: List<Album>? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            // A crash inside this collector must never kill the app process:
            // guard it so a storage/IO failure surfaces as signed-out instead.
            runCatching {
                Settings.creds.collect { c ->
                    api = c?.let { SubsonicApi(it.server, it.user, it.salt, it.token) }
                    signedIn.value = c != null
                    invalidateCaches()
                    // Load the liked list eagerly so every heart/like UI in the
                    // app reflects the server state right after sign-in.
                    if (api != null) runCatching { refreshLiked() }
                }
            }.onFailure { e ->
                e.printStackTrace()
                signedIn.value = false
            }
        }
    }

    private fun invalidateCaches() {
        albumCache.clear()
        artistCache.clear()
        artistsCache = null
        albumsCache = null
        _liked.value = null
    }

    suspend fun signIn(server: String, user: String, password: String): Result<Unit> {
        val salt = SubsonicApi.makeSalt()
        val token = SubsonicApi.tokenFor(password, salt)
        val base = server.trim().removeSuffix("/")
        val test = SubsonicApi(base, user, salt, token)
        return try {
            test.ping()
            Settings.save(base, user, salt, token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        Settings.clear()
    }

    // ---------- home ----------

    suspend fun home(): HomeData = coroutineScope {
        val api = api ?: return@coroutineScope HomeData()
        val recent = async { runCatching { api.albumList("recent", 16) }.getOrDefault(emptyList()) }
        val frequent = async { runCatching { api.albumList("frequent", 16) }.getOrDefault(emptyList()) }
        val playlists = async { runCatching { api.playlists() }.getOrDefault(emptyList()) }
        val liked = async { runCatching { api.starred().song }.getOrDefault(emptyList()) }
        val random = async { runCatching { api.randomSongs(16) }.getOrDefault(emptyList()) }
        HomeData(recent.await(), frequent.await(), playlists.await(), liked.await(), random.await())
    }

    // ---------- detail ----------

    suspend fun album(id: String): Album {
        albumCache[id]?.let { return it }
        val a = api!!.album(id)
        albumCache[id] = a
        return a
    }

    suspend fun artist(id: String): Artist {
        artistCache[id]?.let { return it }
        val a = api!!.artist(id)
        artistCache[id] = a
        return a
    }

    suspend fun topSongs(artistName: String): List<Song> =
        runCatching { api!!.topSongs(artistName) }.getOrDefault(emptyList())

    suspend fun search(query: String): SearchResult = api!!.search(query)

    suspend fun genres(): List<GenreValue> = api!!.genres()

    suspend fun songsByGenre(genre: String): List<Song> = api!!.songsByGenre(genre)

    suspend fun artistsAll(): List<Artist> {
        artistsCache?.let { return it }
        val list = api!!.artists().sortedBy { it.name.lowercase() }
        artistsCache = list
        return list
    }

    suspend fun albumsAll(): List<Album> {
        albumsCache?.let { return it }
        val list = api!!.albumList("alphabeticalByName", 500)
        albumsCache = list
        return list
    }

    suspend fun playlists(): List<Playlist> = api!!.playlists()

    suspend fun playlist(id: String): Playlist = api!!.playlist(id)

    // ---------- liked ----------

    suspend fun refreshLiked() {
        val api = api ?: return
        runCatching { api.starred().song }.onSuccess { _liked.value = it }
    }

    /** Prefer Navidrome's real artist image; fall back to coverArt endpoint. */
    fun artistArtUrl(artist: Artist, size: Int = 600): String? =
        artist.artistImageUrl ?: api?.coverArtUrl(artist.coverArt ?: artist.id, size)

    fun isLiked(song: Song): Boolean {
        val l = _liked.value
        return if (l != null) l.any { it.id == song.id } else song.starred != null
    }

    fun isLikedId(id: String): Boolean {
        val l = _liked.value ?: return false
        return l.any { it.id == id }
    }

    suspend fun toggleLikeId(id: String) {
        // The song may not be in the liked list yet (that's the whole point of
        // liking it) — fall through with a minimal Song instead of bailing out.
        val song = _liked.value?.find { it.id == id }
        toggleLike(song ?: Song(id = id))
    }

    /** Star/unstar an album or artist by id, keeping caches in sync. Returns the new state. */
    suspend fun toggleStarItem(id: String, starred: Boolean): Boolean {
        val api = api ?: return starred
        val ok = runCatching {
            if (starred) api.unstar(id) else api.star(id)
        }.isSuccess
        if (!ok) return starred
        val newStarred = !starred
        albumCache[id]?.let { a -> albumCache[id] = a.copy(starred = if (newStarred) "starred" else null) }
        artistCache[id]?.let { a -> artistCache[id] = a.copy(starred = if (newStarred) "starred" else null) }
        return newStarred
    }

    suspend fun toggleLike(song: Song) {
        val api = api ?: return
        val nowLiked = isLiked(song)
        // Only update local state when the server accepted the star/unstar,
        // so the heart never lies about what the server actually stored.
        val ok = runCatching {
            if (nowLiked) api.unstar(song.id) else api.star(song.id)
        }.isSuccess
        if (!ok) return
        val base = _liked.value
            ?: runCatching { api.starred().song }.getOrDefault(emptyList())
        // Dedupe on add: if the list was just fetched fresh it may already
        // contain this song server-side.
        _liked.value = if (nowLiked) base.filterNot { it.id == song.id }
        else base.filterNot { it.id == song.id } + song.copy(starred = "liked")
        albumCache[song.albumId ?: ""]?.let { cached ->
            if (cached.song.any { it.id == song.id }) {
                albumCache[cached.id] = cached.copy(
                    song = cached.song.map {
                        if (it.id == song.id) it.copy(starred = if (nowLiked) null else "liked") else it
                    }
                )
            }
        }
    }
}
