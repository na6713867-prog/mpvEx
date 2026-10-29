package app.marlboroadvance.mpvex.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import app.marlboroadvance.mpvex.R

/**
 * Background playback service for the music player (Phase D).
 *
 * This service is separate from the video player's MediaPlaybackService: it
 * owns an ExoPlayer behind a Media3 media session, posts its own foreground
 * notification (own channel, notification ID 2, while the video service uses
 * ID 1) and never touches the video playback stack.
 *
 * Coordination with video playback happens through Android audio focus:
 * the player is built with focus handling enabled, so it pauses when the
 * video player (or a call, or another app) takes focus, and the video
 * player pauses in turn when music takes focus.
 */
class MusicPlaybackService : MediaSessionService() {

  private var mediaSession: MediaSession? = null

  override fun onCreate() {
    super.onCreate()

    val player = ExoPlayer.Builder(this)
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(C.USAGE_MEDIA)
          .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
          .build(),
        /* handleAudioFocus = */ true,
      )
      .setHandleAudioBecomingNoisy(true)
      .build()

    // A file that cannot be played must not wedge the queue: skip to the
    // next song when there is one, otherwise stop.
    player.addListener(
      object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
          if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.prepare()
          } else {
            player.stop()
          }
        }
      },
    )

    setMediaNotificationProvider(
      DefaultMediaNotification.Provider.Builder(this)
        .setChannelId(MUSIC_CHANNEL_ID)
        .setNotificationId(MUSIC_NOTIFICATION_ID)
        .setChannelName(R.string.music_playback_channel_name)
        .build(),
    )

    val sessionActivity = packageManager
      .getLaunchIntentForPackage(packageName)
      ?.let { launchIntent ->
        PendingIntent.getActivity(
          this,
          0,
          launchIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
      }

    mediaSession = MediaSession.Builder(this, player)
      .apply {
        if (sessionActivity != null) {
          setSessionActivity(sessionActivity)
        }
      }
      .build()
  }

  override fun onGetSession(
    controllerInfo: MediaSession.ControllerInfo,
  ): MediaSession? = mediaSession

  override fun onTaskRemoved(rootIntent: Intent?) {
    val player = mediaSession?.player
    if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
      stopSelf()
    }
    super.onTaskRemoved(rootIntent)
  }

  override fun onDestroy() {
    mediaSession?.run {
      player.release()
      release()
    }
    mediaSession = null
    super.onDestroy()
  }

  companion object {
    const val MUSIC_CHANNEL_ID = "mpvex_music_playback_channel"
    const val MUSIC_NOTIFICATION_ID = 2
  }
}
