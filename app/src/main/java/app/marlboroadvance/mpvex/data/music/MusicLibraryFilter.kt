package app.marlboroadvance.mpvex.data.music

import app.marlboroadvance.mpvex.domain.music.Album
import app.marlboroadvance.mpvex.domain.music.Artist
import app.marlboroadvance.mpvex.domain.music.MusicFolder
import app.marlboroadvance.mpvex.domain.music.Song
import java.util.Locale

/** How the song list can be ordered. */
enum class SongSort {
  TITLE,
  ARTIST,
  ALBUM,
  DURATION,
  DATE_ADDED,
}

/**
 * Pure search / sort / selection helpers for the music library UI (Phase C).
 * Kept free of Android framework calls so they can be unit tested.
 */
object MusicLibraryFilter {

  fun filterSongs(songs: List<Song>, query: String): List<Song> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return songs
    return songs.filter {
      it.title.lowercase().contains(q) ||
        it.artist.lowercase().contains(q) ||
        it.album.lowercase().contains(q)
    }
  }

  fun filterAlbums(albums: List<Album>, query: String): List<Album> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return albums
    return albums.filter {
      it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
    }
  }

  fun filterArtists(artists: List<Artist>, query: String): List<Artist> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return artists
    return artists.filter { it.name.lowercase().contains(q) }
  }

  fun filterFolders(folders: List<MusicFolder>, query: String): List<MusicFolder> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return folders
    return folders.filter {
      it.name.lowercase().contains(q) || it.path.lowercase().contains(q)
    }
  }

  fun sortSongs(songs: List<Song>, sort: SongSort, ascending: Boolean): List<Song> {
    val sorted = when (sort) {
      SongSort.TITLE -> songs.sortedBy { it.title.lowercase() }
      SongSort.ARTIST ->
        songs.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
      SongSort.ALBUM ->
        songs.sortedWith(
          compareBy({ it.album.lowercase() }, { it.trackNumber }, { it.title.lowercase() }),
        )
      SongSort.DURATION -> songs.sortedBy { it.durationMs }
      SongSort.DATE_ADDED -> songs.sortedBy { it.dateAddedSeconds }
    }
    return if (ascending) sorted else sorted.reversed()
  }

  fun sortAlbums(albums: List<Album>, ascending: Boolean): List<Album> {
    val sorted = albums.sortedBy { it.title.lowercase() }
    return if (ascending) sorted else sorted.reversed()
  }

  fun sortArtists(artists: List<Artist>, ascending: Boolean): List<Artist> {
    val sorted = artists.sortedBy { it.name.lowercase() }
    return if (ascending) sorted else sorted.reversed()
  }

  fun sortFolders(folders: List<MusicFolder>, ascending: Boolean): List<MusicFolder> {
    val sorted = folders.sortedBy { it.name.lowercase() }
    return if (ascending) sorted else sorted.reversed()
  }

  /** Songs of one album, in track order. */
  fun songsInAlbum(songs: List<Song>, albumId: Long): List<Song> =
    songs
      .filter { it.albumId == albumId }
      .sortedWith(compareBy({ it.trackNumber }, { it.title.lowercase() }))

  /** Songs of one artist, grouped by album and then track order. */
  fun songsByArtist(songs: List<Song>, artistId: Long): List<Song> =
    songs
      .filter { it.artistId == artistId }
      .sortedWith(
        compareBy({ it.album.lowercase() }, { it.trackNumber }, { it.title.lowercase() }),
      )

  /** Songs stored directly in one folder, in title order. */
  fun songsInFolder(songs: List<Song>, folderPath: String): List<Song> =
    songs
      .filter { MusicGrouping.folderPathOf(it) == folderPath }
      .sortedBy { it.title.lowercase() }

  /** Formats a duration in milliseconds as m:ss, or h:mm:ss at one hour+. */
  fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
      String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
      String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
  }
}
