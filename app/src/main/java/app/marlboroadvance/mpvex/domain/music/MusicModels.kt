package app.marlboroadvance.mpvex.domain.music

/**
 * Plain data models for the local music library (Phase B2).
 *
 * The data layer fills these models from Android's MediaStore. They are kept
 * free of Android framework types so the grouping logic built on top of them
 * can be unit tested on a plain JVM.
 */
data class Song(
  val id: Long,
  val title: String,
  val artist: String,
  val artistId: Long,
  val album: String,
  val albumId: Long,
  val durationMs: Long,
  /** Content URI of the song, as a string. */
  val uri: String,
  /** Absolute file path from MediaStore when available, otherwise null. */
  val dataPath: String?,
  /** Storage-relative path (API 29+) when available, otherwise null. */
  val relativePath: String?,
  /** Raw MediaStore track value (disc number * 1000 + track number). */
  val trackNumber: Int,
  val year: Int,
  val sizeBytes: Long,
  val mimeType: String?,
  val dateAddedSeconds: Long,
)

data class Album(
  val id: Long,
  val title: String,
  val artist: String,
  val songCount: Int,
  /** Earliest known year among the album's songs, or null when unknown. */
  val year: Int?,
  /** Album artwork content URI, as a string. */
  val artworkUri: String,
)

data class Artist(
  val id: Long,
  val name: String,
  val albumCount: Int,
  val songCount: Int,
)

data class MusicFolder(
  /** Folder path, used as the stable identifier of the folder. */
  val path: String,
  /** Last path segment, shown as the folder name. */
  val name: String,
  val songCount: Int,
)

/** A full snapshot of the local music library. Every list may be empty. */
data class MusicLibrary(
  val songs: List<Song>,
  val albums: List<Album>,
  val artists: List<Artist>,
  val folders: List<MusicFolder>,
) {
  companion object {
    val EMPTY = MusicLibrary(emptyList(), emptyList(), emptyList(), emptyList())
  }
}
