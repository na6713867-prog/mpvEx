package app.marlboroadvance.mpvex.ui.music

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.marlboroadvance.mpvex.playback.MusicPlayerController
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState
import kotlin.math.roundToInt

private val QueueRowHeight = 64.dp

/**
 * The playback queue screen (Phase E): the songs lined up in the player,
 * in order. Tap a song to play it, use the cross button to remove it, and
 * long-press + drag a row to reorder. All edits go straight to the player
 * through [MusicPlayerController], which keeps its seek-duration list in
 * sync with every move and removal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(onClose: () -> Unit) {
  val context = LocalContext.current
  val controller = remember { MusicPlayerController.getInstance(context) }
  val state by controller.state.collectAsState()
  val queueItems by rememberUpdatedState(state.queueItems)

  LaunchedEffect(Unit) {
    controller.connect()
  }

  BackHandler(onBack = onClose)

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            if (state.queueSize > 0) "Queue · ${state.queueSize}" else "Queue",
          )
        },
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
      )
    },
  ) { paddingValues ->
    if (state.queueItems.isEmpty()) {
      EmptyState(
        icon = Icons.AutoMirrored.Filled.QueueMusic,
        title = "Queue is empty",
        message = "Songs you play will line up here.",
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
      )
      return@Scaffold
    }

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val itemHeightPx = with(density) { QueueRowHeight.toPx() }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragDeltaPx by remember { mutableFloatStateOf(0f) }

    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .pointerInput(Unit) {
          detectDragGesturesAfterLongPress(
            onDragStart = { offset ->
              val itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                offset.y >= info.offset && offset.y < info.offset + info.size
              }
              draggingIndex = itemInfo?.index
              dragDeltaPx = 0f
            },
            onDragEnd = {
              draggingIndex = null
              dragDeltaPx = 0f
            },
            onDragCancel = {
              draggingIndex = null
              dragDeltaPx = 0f
            },
            onDrag = { change, dragAmount ->
              change.consume()
              val current = draggingIndex ?: return@detectDragGesturesAfterLongPress
              dragDeltaPx += dragAmount.y
              val items = queueItems
              if (dragDeltaPx >= itemHeightPx && current < items.lastIndex) {
                controller.moveQueueItem(current, current + 1)
                draggingIndex = current + 1
                dragDeltaPx -= itemHeightPx
              } else if (dragDeltaPx <= -itemHeightPx && current > 0) {
                controller.moveQueueItem(current, current - 1)
                draggingIndex = current - 1
                dragDeltaPx += itemHeightPx
              }
            },
          )
        },
    ) {
      itemsIndexed(state.queueItems) { index, item ->
        val isCurrent = index == state.currentIndex
        val isDragging = index == draggingIndex
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(QueueRowHeight)
            .zIndex(if (isDragging) 1f else 0f)
            .offset {
              IntOffset(0, if (isDragging) dragDeltaPx.roundToInt() else 0)
            }
            .clickable { controller.playQueueItem(index) }
            .padding(horizontal = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Filled.DragHandle,
            contentDescription = "Drag to reorder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
              .padding(horizontal = 4.dp)
              .size(20.dp),
          )
          AlbumArtwork(
            albumId = item.albumId ?: -1L,
            modifier = Modifier
              .size(44.dp)
              .clip(RoundedCornerShape(8.dp)),
          )
          Spacer(Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = item.title,
              style = MaterialTheme.typography.bodyMedium,
              color = if (isCurrent) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurface
              },
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = item.artist,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          if (isCurrent) {
            Icon(
              imageVector = Icons.Filled.GraphicEq,
              contentDescription = "Now playing",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(4.dp))
          }
          IconButton(onClick = { controller.removeQueueItem(index) }) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = "Remove from queue",
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }
  }
}
