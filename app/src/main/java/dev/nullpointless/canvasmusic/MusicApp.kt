package dev.nullpointless.canvasmusic

import android.app.Application
import dev.nullpointless.canvasmusic.data.Repository
import dev.nullpointless.canvasmusic.data.Settings
import dev.nullpointless.canvasmusic.playback.PlayerConnection

class MusicApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Settings MUST be initialized first: Repository.init() reads
        // Settings.creds immediately, and Settings.appContext is lateinit.
        Settings.init(this)
        Repository.init(this)
        PlayerConnection.init(this)
    }
}
