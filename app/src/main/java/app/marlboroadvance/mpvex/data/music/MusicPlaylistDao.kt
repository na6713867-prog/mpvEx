package app.marlboroadvance.mpvex.data.music

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Data access for music playlists and favorites (Phase E). */
@Dao
interface MusicPlaylistDao {

  @Query("SELECT * FROM music_playlists ORDER BY createdAtMs ASC")
  fun observePlaylists(): Flow<List<MusicPlaylistEntity>>

  @Query(
    "SELECT * FROM music_playlist_items WHERE playlistId = :playlistId " +
      "ORDER BY position ASC",
  )
  fun observeItems(playlistId: Long): Flow<List<MusicPlaylistItemEntity>>

  @Query(
    "SELECT playlistId, COUNT(*) AS itemCount FROM music_playlist_items " +
      "GROUP BY playlistId",
  )
  fun observeItemCounts(): Flow<List<MusicPlaylistCount>>

  @Query("SELECT * FROM music_favorites ORDER BY addedAtMs DESC")
  fun observeFavorites(): Flow<List<MusicFavoriteEntity>>

  @Insert
  suspend fun insertPlaylist(playlist: MusicPlaylistEntity): Long

  @Query("UPDATE music_playlists SET name = :name WHERE id = :id")
  suspend fun renamePlaylist(id: Long, name: String)

  @Query("DELETE FROM music_playlists WHERE id = :id")
  suspend fun deletePlaylist(id: Long)

  @Query(
    "SELECT MAX(position) FROM music_playlist_items WHERE playlistId = :playlistId",
  )
  suspend fun maxPosition(playlistId: Long): Int?

  @Query(
    "SELECT COUNT(*) FROM music_playlist_items " +
      "WHERE playlistId = :playlistId AND songId = :songId",
  )
  suspend fun itemExists(playlistId: Long, songId: Long): Int

  @Insert
  suspend fun insertItem(item: MusicPlaylistItemEntity)

  @Query(
    "DELETE FROM music_playlist_items " +
      "WHERE playlistId = :playlistId AND songId = :songId",
  )
  suspend fun deleteItem(playlistId: Long, songId: Long)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertFavorite(favorite: MusicFavoriteEntity)

  @Query("DELETE FROM music_favorites WHERE songId = :songId")
  suspend fun deleteFavorite(songId: Long)

  /** Drops entries whose song is no longer in the device library. */
  @Query("DELETE FROM music_playlist_items WHERE songId NOT IN (:validSongIds)")
  suspend fun deleteStaleItems(validSongIds: Collection<Long>)

  /** Drops favorites whose song is no longer in the device library. */
  @Query("DELETE FROM music_favorites WHERE songId NOT IN (:validSongIds)")
  suspend fun deleteStaleFavorites(validSongIds: Collection<Long>)

  @Query("DELETE FROM music_playlist_items")
  suspend fun deleteAllItems()

  @Query("DELETE FROM music_favorites")
  suspend fun deleteAllFavorites()
}
