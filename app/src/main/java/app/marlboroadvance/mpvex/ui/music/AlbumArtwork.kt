package app.marlboroadvance.mpvex.ui.music

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")

/** Small in-memory cache so scrolling lists do not reload the same artwork. */
private object AlbumArtCache {
  private val cache = LruCache<Long, ImageBitmap>(64)

  fun get(albumId: Long): ImageBitmap? = cache.get(albumId)

  fun put(albumId: Long, bitmap: ImageBitmap) {
    cache.put(albumId, bitmap)
  }
}

private fun decodeAlbumArt(context: Context, albumId: Long, targetSizePx: Int): Bitmap? {
  val uri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId)
  val resolver = context.contentResolver
  val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
  if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
  var sample = 1
  while (bounds.outWidth / (sample * 2) >= targetSizePx &&
    bounds.outHeight / (sample * 2) >= targetSizePx
  ) {
    sample *= 2
  }
  val options = BitmapFactory.Options().apply { inSampleSize = sample }
  return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}

/**
 * Shows an album's artwork from MediaStore, or a music-note placeholder when
 * the album has no artwork. Images are decoded off the main thread and cached.
 */
@Composable
fun AlbumArtwork(
  albumId: Long,
  modifier: Modifier = Modifier,
  targetSizePx: Int = 256,
) {
  val context = LocalContext.current
  val art by produceState<ImageBitmap?>(initialValue = AlbumArtCache.get(albumId), albumId) {
    if (value == null) {
      val loaded = withContext(Dispatchers.IO) {
        try {
          decodeAlbumArt(context.applicationContext, albumId, targetSizePx)?.asImageBitmap()
        } catch (e: Exception) {
          null
        }
      }
      if (loaded != null) {
        AlbumArtCache.put(albumId, loaded)
      }
      value = loaded
    }
  }

  Box(
    modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
    contentAlignment = Alignment.Center,
  ) {
    val current = art
    if (current != null) {
      Image(
        bitmap = current,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
      )
    } else {
      Icon(
        imageVector = Icons.Filled.MusicNote,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(8.dp),
      )
    }
  }
}
