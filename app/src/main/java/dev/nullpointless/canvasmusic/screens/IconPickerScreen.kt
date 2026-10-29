package dev.nullpointless.canvasmusic.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nullpointless.canvasmusic.R
import dev.nullpointless.canvasmusic.data.Repository
import dev.nullpointless.canvasmusic.data.Settings
import dev.nullpointless.canvasmusic.ui.Bg
import dev.nullpointless.canvasmusic.ui.CardBg
import dev.nullpointless.canvasmusic.ui.CmTypography
import dev.nullpointless.canvasmusic.ui.GreenBright
import dev.nullpointless.canvasmusic.ui.IconSwitcher
import dev.nullpointless.canvasmusic.ui.TextSecondary
import dev.nullpointless.canvasmusic.ui.TextTertiary
import dev.nullpointless.canvasmusic.ui.White
import kotlinx.coroutines.launch

@Composable
fun IconPickerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val current by Settings.iconVariant.collectAsState(initial = "default")

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val ok = IconSwitcher.pinCustomIcon(context, uri)
            Toast.makeText(
                context,
                if (ok) "Check your home screen — the shortcut was added"
                else "Couldn't create the shortcut (launcher may not support it)",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
            }
            Text("App Icon", style = CmTypography.headlineLarge.copy(fontSize = 22.sp), color = White)
        }

        Text(
            "Built-in icons",
            style = CmTypography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        IconSwitcher.VARIANTS.forEach { variant ->
            val selected = current == variant.id
            val previewRes = when (variant.id) {
                "noir" -> R.drawable.ic_preview_noir
                "classic" -> R.drawable.ic_preview_classic
                "midnight" -> R.drawable.ic_preview_midnight
                else -> R.drawable.ic_preview_default
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            Settings.saveIconVariant(variant.id)
                            IconSwitcher.applyVariant(context, variant.id)
                        }
                        Toast.makeText(
                            context,
                            "Icon updated — may take a few seconds to appear",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Image(
                    painter = painterResource(previewRes),
                    contentDescription = variant.label,
                    modifier = Modifier.size(56.dp).clip(CircleShape)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    variant.label,
                    style = CmTypography.bodyLarge,
                    color = White,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    Icon(Icons.Filled.Check, contentDescription = "Selected", tint = GreenBright)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Your own image",
            style = CmTypography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CardBg)
                .clickable {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AddPhotoAlternate,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Upload image", style = CmTypography.bodyLarge, color = White)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Use any picture as a home-screen icon",
                    style = CmTypography.bodySmall,
                    color = TextSecondary
                )
            }
            Icon(
                Icons.Filled.AddPhotoAlternate,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            "Android doesn't allow apps to replace the real launcher icon with custom images. " +
                "Your picture is added as a home-screen shortcut that opens Spotune with your icon — " +
                "you can hide the original icon from your launcher settings if you like.",
            style = CmTypography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
            color = TextTertiary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}
