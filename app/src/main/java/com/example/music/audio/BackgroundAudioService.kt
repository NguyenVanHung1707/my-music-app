package com.example.music.audio

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class BackgroundAudioService : Service() {
    private val mIBinder = LocalBinder()

    var audioEngine: AudioEngine? = null
        private set

    var playbackNotifier: PlaybackNotifier? = null
        private set

    var isRestoredFromPause = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle notification actions
        intent?.action?.let { action ->
            when (action) {
                PlaybackNotifier.PREV_ACTION -> {
                    audioEngine?.instantReset()
                }
                PlaybackNotifier.PLAY_PAUSE_ACTION -> {
                    audioEngine?.resumeOrPause()
                }
                PlaybackNotifier.NEXT_ACTION -> {
                    audioEngine?.skip(true)
                }
                PlaybackNotifier.EXIT_ACTION -> {
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        audioEngine?.registerNotificationActionsReceiver(false)
        playbackNotifier = null
        audioEngine?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        if (audioEngine == null) {
            audioEngine = AudioEngine(this)
            playbackNotifier = PlaybackNotifier(this)
            audioEngine!!.registerNotificationActionsReceiver(true)
        }
        return mIBinder
    }

    inner class LocalBinder : Binder() {
        val instance: BackgroundAudioService
            get() = this@BackgroundAudioService
    }
} 