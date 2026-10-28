package dev.nullpointless.canvasmusic.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nullpointless.canvasmusic.playback.PlayerConnection
import dev.nullpointless.canvasmusic.ui.Bg
import dev.nullpointless.canvasmusic.ui.CmTypography
import dev.nullpointless.canvasmusic.ui.EmptySection
import dev.nullpointless.canvasmusic.ui.GreenBright
import dev.nullpointless.canvasmusic.ui.RemoteImage
import dev.nullpointless.canvasmusic.ui.SectionTitle
import dev.nullpointless.canvasmusic.ui.TextSecondary
import dev.nullpointless.canvasmusic.ui.White

@Composable
fun QueueScreen(onBack: () -> Unit) {
    val queue by PlayerConnection.queue.collectAsState()
    val nowItem by PlayerConnection.nowPlaying.collectAsState()
    val playing by PlayerConnection.isPlaying.collectAsState()
    val currentIndex by PlayerConnection.currentIndex.collectAsState()
    val contextTitle by PlayerConnection.queueTitle.collectAsState()

    val upcoming = if (queue.isEmpty()) emptyList() else queue.drop(currentIndex + 1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
            }
            Column {
                Text("Queue", style = CmTypography.headlineLarge.copy(fontSize = 22.sp), color = White)
                if (contextTitle != null) {
                    Text(
                        "Playing from ${contextTitle}",
                        style = CmTypography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (nowItem != null) {
                item { SectionTitle("Now playing", Modifier.padding(top = 14.dp)) }
                item {
                    QueueRow(
                        item = nowItem!!,
                        isCurrent = true,
                        isPlaying = playing,
                        onClick = { },
                        onRemove = null
                    )
                }
            }
            item { SectionTitle("Next up", Modifier.padding(top = 14.dp)) }
            if (upcoming.isEmpty()) {
                item { EmptySection("Nothing queued next.\nPlay an album or playlist to fill the queue.") }
            }
            itemsIndexed(upcoming, key = { _, m -> m.mediaId + m.hashCode() }) { i, mediaItem ->
                QueueRow(
                    item = mediaItem,
                    isCurrent = false,
                    isPlaying = false,
                    onClick = {
                        PlayerConnection.playSongAt(currentIndex + 1 + i)
                    },
                    onRemove = {
                        PlayerConnection.removeAt(currentIndex + 1 + i)
                    }
                )
            }
        }
    }
}

@Composable
private fun QueueRow(
    item: androidx.media3.common.MediaItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        RemoteImage(
            url = item.mediaMetadata.artworkUri?.toString(),
            modifier = Modifier.size(46.dp),
            shape = RoundedCornerShape(4.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.mediaMetadata.title?.toString() ?: "",
                style = CmTypography.bodyLarge,
                color = if (isCurrent) GreenBright else White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                item.mediaMetadata.artist?.toString() ?: "",
                style = CmTypography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isCurrent && isPlaying) {
            dev.nullpointless.canvasmusic.ui.EqIndicator()
            Spacer(Modifier.width(12.dp))
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}
