package dev.nullpointless.canvasmusic

import android.app.Application
import dev.nullpointless.canvasmusic.data.Repository
import dev.nullpointless.canvasmusic.data.Settings
import dev.nullpointless.canvasmusic.playback.PlayerConnection
import dev.nullpointless.canvasmusic.ui.IconSwitcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MusicApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Settings MUST be initialized first: Repository.init() reads
        // Settings.creds immediately, and Settings.appContext is lateinit.
        Settings.init(this)
        Repository.init(this)
        PlayerConnection.init(this)

        // Re-assert the saved launcher icon variant so the enabled
        // activity-alias always matches what the user picked.
        appScope.launch {
            runCatching {
                IconSwitcher.applyVariant(this@MusicApp, Settings.iconVariant.first())
            }
        }
    }

    override fun onTerminate() {
        appScope.cancel()
        super.onTerminate()
    }
}
