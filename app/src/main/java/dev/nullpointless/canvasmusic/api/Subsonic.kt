package dev.nullpointless.canvasmusic.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class ApiException(val code: Int, message: String) : Exception(message)

@Serializable
data class Song(
    val id: String,
    val parent: String? = null,
    val title: String = "",
    val artist: String? = null,
    val artistId: String? = null,
    val album: String? = null,
    val albumId: String? = null,
    val track: Int? = null,
    val year: Int? = null,
    val size: Long? = null,
    val contentType: String? = null,
    val suffix: String? = null,
    val duration: Long? = null,
    val bitRate: Long? = null,
    val path: String? = null,
    val coverArt: String? = null,
    val playCount: Long? = null,
    val discNumber: Int? = null,
    val created: String? = null,
    val starred: String? = null
)

@Serializable
data class Album(
    val id: String,
    val name: String = "",
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Long? = null,
    val duration: Long? = null,
    val playCount: Long? = null,
    val year: Int? = null,
    val genre: String? = null,
    val created: String? = null,
    val starred: String? = null,
    val song: List<Song> = emptyList()
)

@Serializable
data class Artist(
    val id: String,
    val name: String = "",
    val coverArt: String? = null,
    val albumCount: Long? = null,
    val starred: String? = null,
    val album: List<Album> = emptyList()
)

@Serializable
data class Playlist(
    val id: String,
    val name: String = "",
    val comment: String? = null,
    val owner: String? = null,
    @SerialName("public") val isPublic: Boolean? = null,
    val songCount: Long? = null,
    val duration: Long? = null,
    val coverArt: String? = null,
    val created: String? = null,
    val changed: String? = null,
    val entry: List<Song> = emptyList()
)

@Serializable
data class GenreValue(val value: String = "", val songCount: Long? = null, val albumCount: Long? = null)

// --- response wrappers ---

@Serializable
data class ApiError(val code: Int = 0, val message: String = "")

@Serializable
data class IgnoreRoot(val status: String? = null)

@Serializable
data class PingRoot(val status: String = "", val error: ApiError? = null)

@Serializable
data class Index(val name: String = "", val artist: List<Artist> = emptyList())

@Serializable
data class Indexes(val ignoredArticles: String? = null, val index: List<Index> = emptyList())

@Serializable
data class ArtistsRoot(val artists: Indexes? = null)

@Serializable
data class AlbumList(val album: List<Album> = emptyList())

@Serializable
data class AlbumListRoot(val albumList2: AlbumList? = null)

@Serializable
data class AlbumRoot(val album: Album? = null)

@Serializable
data class ArtistRoot(val artist: Artist? = null)

@Serializable
data class Songs(val song: List<Song> = emptyList())

@Serializable
data class TopSongsRoot(val topSongs: Songs? = null)

@Serializable
data class RandomSongsRoot(val randomSongs: Songs? = emptyList())

@Serializable
data class SongsByGenreRoot(val songsByGenre: Songs? = null)

@Serializable
data class SearchResult(
    val artist: List<Artist> = emptyList(),
    val album: List<Album> = emptyList(),
    val song: List<Song> = emptyList()
)

@Serializable
data class SearchRoot(val searchResult3: SearchResult? = null)

@Serializable
data class PlaylistsWrapper(val playlist: List<Playlist> = emptyList())

@Serializable
data class PlaylistsRoot(val playlists: PlaylistsWrapper? = null)

@Serializable
data class PlaylistRoot(val playlist: Playlist? = null)

@Serializable
data class Starred(
    val artist: List<Artist> = emptyList(),
    val album: List<Album> = emptyList(),
    val song: List<Song> = emptyList()
)

@Serializable
data class StarredRoot(val starred2: Starred? = null)

@Serializable
data class GenresWrapper(val genre: List<GenreValue> = emptyList())

@Serializable
data class GenresRoot(val genres: GenresWrapper? = null)

class SubsonicApi(
    val baseUrl: String,
    val user: String,
    private val salt: String,
    private val token: String
) {
    companion object {
        const val CLIENT = "CanvasMusic"
        const val VERSION = "1.16.1"

        fun md5Hex(s: String): String =
            MessageDigest.getInstance("MD5").digest(s.toByteArray())
                .joinToString("") { "%02x".format(it) }

        fun makeSalt(): String {
            val chars = "0123456789abcdef"
            return List(12) { chars[Random.nextInt(16)] }.joinToString("")
        }

        fun tokenFor(password: String, salt: String): String = md5Hex(password + salt)
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private fun builder(endpoint: String, vararg params: Pair<String, Any?>): HttpUrl.Builder {
        val base = baseUrl.toHttpUrlOrNull()
            ?: throw ApiException(-1, "Invalid server URL")
        val b = base.newBuilder().addPathSegments("rest/$endpoint.view")
        b.addQueryParameter("u", user)
        b.addQueryParameter("t", token)
        b.addQueryParameter("s", salt)
        b.addQueryParameter("v", VERSION)
        b.addQueryParameter("c", CLIENT)
        b.addQueryParameter("f", "json")
        for ((k, v) in params) {
            if (v != null) b.addQueryParameter(k, v.toString())
        }
        return b
    }

    fun url(endpoint: String, vararg params: Pair<String, Any?>): String =
        builder(endpoint, *params).build().toString()

    private suspend inline fun <reified T> call(
        endpoint: String,
        vararg params: Pair<String, Any?>
    ): T = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(builder(endpoint, *params).build()).build()
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: throw ApiException(-1, "Empty response")
            if (!resp.isSuccessful) throw ApiException(resp.code, "HTTP ${resp.code}")
            val root = json.parseToJsonElement(body)
                .jsonObject["subsonic-response"]?.jsonObject
                ?: throw ApiException(-1, "Unexpected response format")
            val status = root["status"]?.jsonPrimitive?.contentOrNull ?: "failed"
            if (status != "ok") {
                val err = root["error"] as? JsonObject
                val code = err?.get("code")?.jsonPrimitive?.intOrNull ?: 0
                val msg = err?.get("message")?.jsonPrimitive?.contentOrNull ?: "Unknown error"
                throw ApiException(code, msg)
            }
            json.decodeFromJsonElement<T>(root)
        }
    }

    suspend fun ping(): PingRoot = call("ping")

    suspend fun artists(): List<Artist> =
        call<ArtistsRoot>("getArtists").artists?.index?.flatMap { it.artist } ?: emptyList()

    suspend fun albumList(type: String, size: Int = 20, offset: Int = 0): List<Album> =
        call<AlbumListRoot>("getAlbumList2", "type" to type, "size" to size, "offset" to offset)
            .albumList2?.album ?: emptyList()

    suspend fun album(id: String): Album =
        call<AlbumRoot>("getAlbum", "id" to id).album
            ?: throw ApiException(-1, "Album not found")

    suspend fun artist(id: String): Artist =
        call<ArtistRoot>("getArtist", "id" to id).artist
            ?: throw ApiException(-1, "Artist not found")

    suspend fun topSongs(artistName: String, count: Int = 10): List<Song> =
        call<TopSongsRoot>("getTopSongs", "artist" to artistName, "count" to count)
            .topSongs?.song ?: emptyList()

    suspend fun search(query: String): SearchResult =
        call<SearchRoot>(
            "search3", "query" to query,
            "artistCount" to 12, "albumCount" to 14, "songCount" to 30
        ).searchResult3 ?: SearchResult()

    suspend fun randomSongs(size: Int = 20): List<Song> =
        call<RandomSongsRoot>("getRandomSongs", "size" to size).randomSongs?.song ?: emptyList()

    suspend fun songsByGenre(genre: String, count: Int = 60): List<Song> =
        call<SongsByGenreRoot>("getSongsByGenre", "genre" to genre, "count" to count)
            .songsByGenre?.song ?: emptyList()

    suspend fun genres(): List<GenreValue> =
        call<GenresRoot>("getGenres").genres?.genre ?: emptyList()

    suspend fun playlists(): List<Playlist> =
        call<PlaylistsRoot>("getPlaylists").playlists?.playlist ?: emptyList()

    suspend fun playlist(id: String): Playlist =
        call<PlaylistRoot>("getPlaylist", "id" to id).playlist
            ?: throw ApiException(-1, "Playlist not found")

    suspend fun starred(): Starred =
        call<StarredRoot>("getStarred2").starred2 ?: Starred()

    suspend fun star(id: String) { call<IgnoreRoot>("star", "id" to id) }
    suspend fun unstar(id: String) { call<IgnoreRoot>("unstar", "id" to id) }

    suspend fun scrobble(id: String, submission: Boolean) {
        call<IgnoreRoot>("scrobble", "id" to id, "submission" to submission)
    }

    fun coverArtUrl(coverArtId: String?, size: Int = 600): String? =
        coverArtId?.let { url("getCoverArt", "id" to it, "size" to size) }

    fun streamUrl(songId: String): String = url("stream", "id" to songId, "maxBitRate" to 0)
}
