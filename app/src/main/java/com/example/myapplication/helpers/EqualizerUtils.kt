package com.example.music.helpers

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.widget.Toast

object EqualizerUtils {
    
    /**
     * Kiểm tra device có hỗ trợ equalizer không
     */
    fun hasEqualizer(context: Context): Boolean {
        val effects = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
        val pm = context.packageManager
        val ri = pm.resolveActivity(effects, 0)
        return ri != null
    }

    /**
     * Mở audio effect session
     */
    fun openAudioEffectSession(context: Context, sessionId: Int) {
        val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        context.sendBroadcast(intent)
    }

    /**
     * Đóng audio effect session
     */
    fun closeAudioEffectSession(context: Context, sessionId: Int) {
        val audioEffectsIntent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        context.sendBroadcast(audioEffectsIntent)
    }

    /**
     * Mở equalizer UI
     */
    fun openEqualizer(activity: Activity, mediaPlayer: MediaPlayer?) {
        val sessionId = mediaPlayer?.audioSessionId

        if (sessionId == AudioEffect.ERROR_BAD_VALUE) {
            notifyNoSessionId(activity)
        } else {
            try {
                val effects = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                effects.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                effects.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                activity.startActivityForResult(effects, 0)
            } catch (notFound: ActivityNotFoundException) {
                Toast.makeText(activity, "No equalizer app found", Toast.LENGTH_SHORT).show()
                notFound.printStackTrace()
            }
        }
    }

    /**
     * Thông báo không có session ID
     */
    fun notifyNoSessionId(context: Context) {
        Toast.makeText(context, "Play a song first", Toast.LENGTH_SHORT).show()
    }

    /**
     * Kiểm tra và setup equalizer cho MediaPlayer
     */
    fun setupEqualizer(context: Context, mediaPlayer: MediaPlayer?): Boolean {
        return try {
            if (mediaPlayer != null && hasEqualizer(context)) {
                openAudioEffectSession(context, mediaPlayer.audioSessionId)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
} 