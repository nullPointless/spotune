package dev.nullpointless.canvasmusic.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * In-app launcher icon switching.
 *
 * Android only allows the real launcher icon to be one of the icons compiled
 * into the APK, selected via <activity-alias> components. User-uploaded images
 * cannot replace the real icon; instead we offer a pinned home-screen shortcut
 * with a custom adaptive bitmap icon, which behaves like an app icon.
 */
object IconSwitcher {

    data class Variant(val id: String, val label: String)

    val VARIANTS = listOf(
        Variant("default", "Classic Green"),
        Variant("noir", "Noir"),
        Variant("classic", "Pure White"),
        Variant("midnight", "Midnight")
    )

    private fun aliasComponent(context: Context, variant: String): ComponentName {
        val pkg = context.packageName
        return when (variant) {
            "noir" -> ComponentName(pkg, "$pkg.alias.IconNoir")
            "classic" -> ComponentName(pkg, "$pkg.alias.IconClassic")
            "midnight" -> ComponentName(pkg, "$pkg.alias.IconMidnight")
            else -> ComponentName(pkg, "$pkg.alias.IconDefault")
        }
    }

    /** Enable the alias for [variant]; disable all others. Safe to call repeatedly. */
    fun applyVariant(context: Context, variant: String) {
        val pm = context.packageManager
        // Enable the chosen one first so the app never vanishes from the launcher.
        for (v in VARIANTS) {
            val cn = aliasComponent(context, v.id)
            val state = if (v.id == variant) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(cn, state, PackageManager.DONT_KILL_APP)
        }
    }

    /** Whether the launcher supports pinning shortcuts (needed for custom icons). */
    fun supportsCustomIcon(context: Context): Boolean =
        ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    /**
     * Decode a user-picked image, center-crop it square, and pin it to the home
     * screen as a "Spotune" shortcut with an adaptive icon. Returns false when
     * the launcher does not support pinning or the image could not be decoded.
     */
    fun pinCustomIcon(context: Context, image: Uri): Boolean {
        val bitmap = decodeSquareBitmap(context, image, 432)
            ?: return false
        val intent = Intent(context, dev.nullpointless.canvasmusic.MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
        // Unique id per upload so re-pinning never collides with an existing shortcut.
        val shortcut = ShortcutInfoCompat.Builder(context, "spotune-icon-${System.currentTimeMillis()}")
            .setShortLabel("Spotune")
            .setIcon(IconCompat.createWithAdaptiveBitmap(bitmap))
            .setIntent(intent)
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

    private fun decodeSquareBitmap(context: Context, uri: Uri, size: Int): Bitmap? {
        return try {
            // Query bounds first so huge photos don't blow the heap.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            var w = bounds.outWidth
            var h = bounds.outHeight
            while (w / 2 >= size * 2 && h / 2 >= size * 2) {
                sample *= 2
                w /= 2
                h /= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val src = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null

            // Center-crop to square, then scale to the target size.
            val side = minOf(src.width, src.height)
            val x = (src.width - side) / 2
            val y = (src.height - side) / 2
            val cropped = Bitmap.createBitmap(src, x, y, side, side)
            if (cropped != src) src.recycle()
            val scaled = Bitmap.createScaledBitmap(cropped, size, size, true)
            if (scaled != cropped) cropped.recycle()
            scaled
        } catch (e: Exception) {
            null
        }
    }
}
