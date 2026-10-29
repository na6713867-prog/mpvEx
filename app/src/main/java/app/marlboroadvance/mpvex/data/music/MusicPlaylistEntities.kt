package app.marlboroadvance.mpvex.data.music

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Music playlists and favorites (Phase E), stored in the separate
 * [MusicDatabase] — never in the video app's MpvExDatabase.
 *
 * A stored song reference keeps the MediaStore id (the join key against a
 * fresh library scan), the playable content URI, and a title/artist
 * snapshot, mirroring how the video playlists identify files.
 */
@Entity(tableName = "music_playlists")
data class MusicPlaylistEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val createdAtMs: Long,
)

@Entity(
  tableName = "music_playlist_items",
  foreignKeys = [
    ForeignKey(
      entity = MusicPlaylistEntity::class,
      parentColumns = ["id"],
      childColumns = ["playlistId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index("playlistId")],
)
data class MusicPlaylistItemEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val playlistId: Long,
  val songId: Long,
  val uri: String,
  val title: String,
  val artist: String,
  val albumId: Long,
  val durationMs: Long,
  val position: Int,
)

/** A favorite song. The song id alone is the identity. */
@Entity(tableName = "music_favorites")
data class MusicFavoriteEntity(
  @PrimaryKey val songId: Long,
  val addedAtMs: Long,
)

/** Projection: how many songs each playlist holds. */
data class MusicPlaylistCount(
  val playlistId: Long,
  val itemCount: Int,
)
