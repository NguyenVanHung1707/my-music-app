package com.example.music.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.music.R
import com.example.music.data.AudioTrack
import com.example.music.view.HomeScreenActivity

class PlaybackNotifier(private val mBackgroundAudioService: BackgroundAudioService) {

    val notificationManager: NotificationManager = 
        mBackgroundAudioService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music Player Controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createNotification(): Notification {
        val mediaPlayerHolder = mBackgroundAudioService.audioEngine
        val selectedTrack = mediaPlayerHolder?.getCurrentSong()

        val notificationBuilder = NotificationCompat.Builder(mBackgroundAudioService, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_play)
            .setLargeIcon(getAlbumArt(selectedTrack))
            .setContentTitle(selectedTrack?.title ?: "Unknown")
            .setContentText(selectedTrack?.artist ?: "Unknown Artist")
            .setContentIntent(createContentIntent())
            .setDeleteIntent(createDeleteIntent())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)

        // Add action buttons
        notificationBuilder.addAction(createNotificationAction(PREV_ACTION, "Previous", R.drawable.baseline_skip_previous_24))
        
        val playPauseIcon = if (mediaPlayerHolder?.isPlaying() == true) {
            R.drawable.baseline_pause_24
        } else {
            R.drawable.ic_play
        }
        val playPauseText = if (mediaPlayerHolder?.isPlaying() == true) "Pause" else "Play"
        notificationBuilder.addAction(createNotificationAction(PLAY_PAUSE_ACTION, playPauseText, playPauseIcon))
        
        notificationBuilder.addAction(createNotificationAction(NEXT_ACTION, "Next", R.drawable.baseline_skip_next_24))

        // Set custom style for better appearance
        notificationBuilder.setStyle(
            NotificationCompat.BigTextStyle()
                .bigText("${selectedTrack?.artist}")
                .setBigContentTitle(selectedTrack?.title ?: "Unknown")
        )

        return notificationBuilder.build()
    }

    private fun getAlbumArt(track: AudioTrack?): Bitmap {
        return try {
            // Tạm thời dùng icon mặc định, sau này có thể load từ albumArt path
            BitmapFactory.decodeResource(mBackgroundAudioService.resources, R.drawable.ic_music_display)
        } catch (e: Exception) {
            BitmapFactory.decodeResource(mBackgroundAudioService.resources, R.drawable.ic_music_display)
        }
    }

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(mBackgroundAudioService, HomeScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        return PendingIntent.getActivity(mBackgroundAudioService, REQUEST_CODE, intent, flags)
    }

    private fun createDeleteIntent(): PendingIntent {
        val intent = Intent(mBackgroundAudioService, BackgroundAudioService::class.java).apply {
            action = EXIT_ACTION
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        return PendingIntent.getService(mBackgroundAudioService, REQUEST_CODE, intent, flags)
    }

    private fun createNotificationAction(action: String, title: String, icon: Int): NotificationCompat.Action {
        val intent = Intent(mBackgroundAudioService, BackgroundAudioService::class.java).apply {
            this.action = action
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getService(mBackgroundAudioService, REQUEST_CODE, intent, flags)
        
        return NotificationCompat.Action.Builder(icon, title, pendingIntent).build()
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "music_channel"
        const val REQUEST_CODE = 0
        
        const val PREV_ACTION = "com.example.music.prev"
        const val PLAY_PAUSE_ACTION = "com.example.music.playpause"
        const val NEXT_ACTION = "com.example.music.next"
        const val EXIT_ACTION = "com.example.music.exit"
    }
} 