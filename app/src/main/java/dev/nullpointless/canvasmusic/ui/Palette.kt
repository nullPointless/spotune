package dev.nullpointless.canvasmusic.ui

import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberDominantColor(artUrl: String?, fallback: Color = Color(0xFF2A2A2A)): Color {
    val context = LocalContext.current
    return produceState(fallback, artUrl) {
        if (artUrl == null) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                val loader = context.imageLoader
                val request = ImageRequest.Builder(context)
                    .data(artUrl)
                    .allowHardware(false)
                    .size(128)
                    .build()
                val result = loader.execute(request)
                val bmp = (result.drawable as? BitmapDrawable)?.bitmap
                if (bmp != null) {
                    val palette = Palette.from(bmp).maximumColorCount(24).generate()
                    palette.dominantSwatch?.let { Color(it.rgb) }
                } else null
            }.getOrNull() ?: fallback
        }
    }.value
}

fun Color.darken(fraction: Float): Color = lerp(this, Color(0xFF0A0A0A), fraction)

fun Color.soften(fraction: Float): Color = lerp(this, Color(0xFF121212), fraction)
