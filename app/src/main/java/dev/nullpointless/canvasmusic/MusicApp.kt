package dev.nullpointless.canvasmusic

import android.app.Application
import dev.nullpointless.canvasmusic.data.Repository
import dev.nullpointless.canvasmusic.playback.PlayerConnection

class MusicApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Repository.init(this)
        PlayerConnection.init(this)
    }
}
