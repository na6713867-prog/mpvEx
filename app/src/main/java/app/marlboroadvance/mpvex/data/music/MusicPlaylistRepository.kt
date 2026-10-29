package app.marlboroadvance.mpvex.data.music

import android.content.Context
import app.marlboroadvance.mpvex.domain.music.Song
import kotlinx.coroutines.flow.Flow

/**
 * Music playlists and favorites (Phase E), backed by [MusicDatabase].
 *
 * Playlists keep songs in insertion order (a position column); adding a
 * song that is already in the playlist is a no-op. [cleanup] removes
 * entries whose song has disappeared from the device library, the same
 * dangling-entry rule the video playlists use.
 */
class MusicPlaylistRepository(context: Context) {

  private val dao = MusicDatabase.getInstance(context).musicPlaylistDao()

  val playlists: Flow<List<MusicPlaylistEntity>> = dao.observePlaylists()

  val itemCounts: Flow<List<MusicPlaylistCount>> = dao.observeItemCounts()

  val favorites: Flow<List<MusicFavoriteEntity>> = dao.observeFavorites()

  fun items(playlistId: Long): Flow<List<MusicPlaylistItemEntity>> =
    dao.observeItems(playlistId)

  suspend fun createPlaylist(name: String): Long =
    dao.insertPlaylist(
      MusicPlaylistEntity(
        name = name.trim(),
        createdAtMs = System.currentTimeMillis(),
      ),
    )

  suspend fun renamePlaylist(id: Long, name: String) {
    dao.renamePlaylist(id, name.trim())
  }

  /** Deleting a playlist deletes its items too (foreign-key cascade). */
  suspend fun deletePlaylist(id: Long) {
    dao.deletePlaylist(id)
  }

  /** Adds [song]; returns false when it is already in the playlist. */
  suspend fun addSong(playlistId: Long, song: Song): Boolean {
    if (dao.itemExists(playlistId, song.id) > 0) return false
    val position = (dao.maxPosition(playlistId) ?: -1) + 1
    dao.insertItem(
      MusicPlaylistItemEntity(
        playlistId = playlistId,
        songId = song.id,
        uri = song.uri,
        title = song.title,
        artist = song.artist,
        albumId = song.albumId,
        durationMs = song.durationMs,
        position = position,
      ),
    )
    return true
  }

  suspend fun removeSong(playlistId: Long, songId: Long) {
    dao.deleteItem(playlistId, songId)
  }

  suspend fun setFavorite(songId: Long, favorite: Boolean) {
    if (favorite) {
      dao.insertFavorite(
        MusicFavoriteEntity(
          songId = songId,
          addedAtMs = System.currentTimeMillis(),
        ),
      )
    } else {
      dao.deleteFavorite(songId)
    }
  }

  /**
   * Removes playlist entries and favorites whose song id is not in
   * [validSongIds] (the current library scan). Very large libraries skip
   * the SQL NOT IN cleanup rather than risk an oversized query.
   *
   * An empty scan is never trusted: MediaStore can briefly return no songs
   * at all (a rescan in progress, a permission race), and deleting every
   * entry on such a scan would silently wipe the user's playlists and
   * favorites. An empty [validSongIds] therefore cleans nothing.
   */
  suspend fun cleanup(validSongIds: Set<Long>) {
    if (validSongIds.isEmpty()) return
    if (validSongIds.size <= MAX_CLEANUP_IDS) {
      dao.deleteStaleItems(validSongIds)
      dao.deleteStaleFavorites(validSongIds)
    }
  }

  private companion object {
    const val MAX_CLEANUP_IDS = 30_000
  }
}
