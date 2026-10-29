package app.marlboroadvance.mpvex.data.music

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import app.marlboroadvance.mpvex.domain.music.MusicLibrary
import app.marlboroadvance.mpvex.domain.music.Song

/**
 * Reads the local music library from Android's MediaStore.
 *
 * The repository never throws for the expected failure cases: without the
 * audio permission, or when the device has no music, it returns empty
 * results. It only reads metadata; it does not play anything.
 */
class MusicLibraryRepository(context: Context) {

  private val appContext = context.applicationContext

  /** True when the app currently may read the audio library. */
  fun hasAudioPermission(): Boolean = AudioPermission.hasAudioPermission(appContext)

  /** All music files known to MediaStore, sorted by title. */
  fun loadSongs(): List<Song> {
    if (!hasAudioPermission()) return emptyList()

    val hasRelativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val projection = mutableListOf(
      MediaStore.Audio.Media._ID,
      MediaStore.Audio.Media.TITLE,
      MediaStore.Audio.Media.ARTIST,
      MediaStore.Audio.Media.ARTIST_ID,
      MediaStore.Audio.Media.ALBUM,
      MediaStore.Audio.Media.ALBUM_ID,
      MediaStore.Audio.Media.DURATION,
      MediaStore.Audio.Media.DATA,
      MediaStore.Audio.Media.TRACK,
      MediaStore.Audio.Media.YEAR,
      MediaStore.Audio.Media.SIZE,
      MediaStore.Audio.Media.MIME_TYPE,
      MediaStore.Audio.Media.DATE_ADDED,
    )
    if (hasRelativePath) {
      // RELATIVE_PATH only exists on API 29+, so it is only requested there.
      projection += MediaStore.Audio.Media.RELATIVE_PATH
    }

    val cursor = try {
      appContext.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection.toTypedArray(),
        "${MediaStore.Audio.Media.IS_MUSIC} != 0",
        null,
        "${MediaStore.Audio.Media.TITLE} ASC",
      )
    } catch (e: SecurityException) {
      null
    } ?: return emptyList()

    val songs = mutableListOf<Song>()
    cursor.use { c ->
      val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
      val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
      val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
      val artistIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
      val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
      val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
      val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
      val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
      val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
      val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
      val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
      val mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
      val dateAddedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
      val relativePathCol =
        if (hasRelativePath) c.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH) else -1

      while (c.moveToNext()) {
        val id = c.getLong(idCol)
        val dataPath = c.getString(dataCol)?.takeIf { it.isNotBlank() }
        songs += Song(
          id = id,
          title = c.getString(titleCol)?.takeIf { it.isNotBlank() }
            ?: dataPath?.substringAfterLast('/')?.substringBeforeLast('.')
            ?: UNKNOWN_TITLE,
          artist = c.getString(artistCol).cleanMetadata(UNKNOWN_ARTIST),
          artistId = c.getLong(artistIdCol),
          album = c.getString(albumCol).cleanMetadata(UNKNOWN_ALBUM),
          albumId = c.getLong(albumIdCol),
          durationMs = c.getLong(durationCol),
          uri = MusicGrouping.songUri(id),
          dataPath = dataPath,
          relativePath = if (relativePathCol >= 0) c.getString(relativePathCol) else null,
          trackNumber = c.getInt(trackCol),
          year = c.getInt(yearCol),
          sizeBytes = c.getLong(sizeCol),
          mimeType = c.getString(mimeCol),
          dateAddedSeconds = c.getLong(dateAddedCol),
        )
      }
    }
    return songs
  }

  /** The full library: songs plus the albums, artists and folders in them. */
  fun loadLibrary(): MusicLibrary {
    val songs = loadSongs()
    if (songs.isEmpty()) return MusicLibrary.EMPTY
    return MusicLibrary(
      songs = songs,
      albums = MusicGrouping.buildAlbums(songs),
      artists = MusicGrouping.buildArtists(songs),
      folders = MusicGrouping.buildFolders(songs),
    )
  }

  private fun String?.cleanMetadata(fallback: String): String =
    this?.takeIf { it.isNotBlank() && it != MEDIASTORE_UNKNOWN } ?: fallback

  private companion object {
    const val MEDIASTORE_UNKNOWN = "<unknown>"
    const val UNKNOWN_TITLE = "Unknown title"
    const val UNKNOWN_ARTIST = "Unknown artist"
    const val UNKNOWN_ALBUM = "Unknown album"
  }
}
