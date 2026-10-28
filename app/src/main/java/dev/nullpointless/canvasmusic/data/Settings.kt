package dev.nullpointless.canvasmusic.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

object Settings {
    private lateinit var appContext: Context

    private val KEY_SERVER = stringPreferencesKey("server")
    private val KEY_USER = stringPreferencesKey("user")
    private val KEY_SALT = stringPreferencesKey("salt")
    private val KEY_TOKEN = stringPreferencesKey("token")

    data class Creds(val server: String, val user: String, val salt: String, val token: String)

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val creds: Flow<Creds?> get() = appContext.dataStore.data.map { p ->
        val s = p[KEY_SERVER]
        val u = p[KEY_USER]
        val sa = p[KEY_SALT]
        val t = p[KEY_TOKEN]
        if (s != null && u != null && sa != null && t != null) Creds(s, u, sa, t) else null
    }

    suspend fun save(server: String, user: String, salt: String, token: String) {
        appContext.dataStore.edit { p ->
            p[KEY_SERVER] = server
            p[KEY_USER] = user
            p[KEY_SALT] = salt
            p[KEY_TOKEN] = token
        }
    }

    suspend fun clear() {
        appContext.dataStore.edit { it.clear() }
    }
}
