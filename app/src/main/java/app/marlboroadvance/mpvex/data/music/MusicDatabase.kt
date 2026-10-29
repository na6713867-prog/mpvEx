package app.marlboroadvance.mpvex.data.music

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The music library's own database (Phase E): playlists and favorites only.
 *
 * It is deliberately separate from the video app's MpvExDatabase, which is
 * never touched by music features — a music-side mistake can therefore
 * never endanger video playlists, playback states, or watch history.
 */
@Database(
  entities = [
    MusicPlaylistEntity::class,
    MusicPlaylistItemEntity::class,
    MusicFavoriteEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
abstract class MusicDatabase : RoomDatabase() {

  abstract fun musicPlaylistDao(): MusicPlaylistDao

  companion object {
    @Volatile
    private var instance: MusicDatabase? = null

    fun getInstance(context: Context): MusicDatabase =
      instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
          context.applicationContext,
          MusicDatabase::class.java,
          "music.db",
        ).build().also { instance = it }
      }
  }
}
