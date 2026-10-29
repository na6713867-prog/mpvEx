package app.marlboroadvance.mpvex.data.music

import app.marlboroadvance.mpvex.domain.music.Album
import app.marlboroadvance.mpvex.domain.music.Artist
import app.marlboroadvance.mpvex.domain.music.MusicFolder
import app.marlboroadvance.mpvex.domain.music.Song

/**
 * Pure grouping logic that organises a flat song list into albums, artists
 * and folders. Kept free of Android framework calls so it can be unit tested.
 */
object MusicGrouping {

  private const val SONG_URI_BASE = "content://media/external/audio/media"
  private const val ALBUM_ART_URI_BASE = "content://media/external/audio/albumart"

  /** Content URI (as a string) of a song in MediaStore. */
  fun songUri(songId: Long): String = "$SONG_URI_BASE/$songId"

  /** Content URI (as a string) of an album's artwork. */
  fun albumArtUri(albumId: Long): String = "$ALBUM_ART_URI_BASE/$albumId"

  /** Groups [songs] by album id. Albums are sorted by title. */
  fun buildAlbums(songs: List<Song>): List<Album> =
    songs
      .groupBy { it.albumId }
      .map { (albumId, albumSongs) ->
        val first = albumSongs.first()
        Album(
          id = albumId,
          title = first.album,
          artist = first.artist,
          songCount = albumSongs.size,
          year = albumSongs.map { it.year }.filter { it > 0 }.minOrNull(),
          artworkUri = albumArtUri(albumId),
        )
      }
      .sortedBy { it.title.lowercase() }

  /** Groups [songs] by artist id. Artists are sorted by name. */
  fun buildArtists(songs: List<Song>): List<Artist> =
    songs
      .groupBy { it.artistId }
      .map { (artistId, artistSongs) ->
        Artist(
          id = artistId,
          name = artistSongs.first().artist,
          albumCount = artistSongs.map { it.albumId }.distinct().size,
          songCount = artistSongs.size,
        )
      }
      .sortedBy { it.name.lowercase() }

  /**
   * Groups [songs] by the folder that contains them. Songs without any path
   * information are skipped. Folders are sorted by name.
   */
  fun buildFolders(songs: List<Song>): List<MusicFolder> =
    songs
      .mapNotNull { song -> folderPathOf(song)?.let { path -> path to song } }
      .groupBy(keySelector = { it.first }, valueTransform = { it.second })
      .map { (path, folderSongs) ->
        MusicFolder(
          path = path,
          name = path.substringAfterLast('/').ifBlank { path },
          songCount = folderSongs.size,
        )
      }
      .sortedBy { it.name.lowercase() }

  /**
   * The folder that contains [song]: the parent of its file path when known,
   * otherwise its storage-relative path (API 29+), otherwise null.
   */
  fun folderPathOf(song: Song): String? {
    val dataPath = song.dataPath
    if (!dataPath.isNullOrBlank()) {
      val parent = dataPath.substringBeforeLast('/', missingDelimiterValue = "")
      if (parent.isNotBlank()) return parent
    }
    val relative = song.relativePath
    if (!relative.isNullOrBlank()) {
      return relative.trimEnd('/')
    }
    return null
  }
}
