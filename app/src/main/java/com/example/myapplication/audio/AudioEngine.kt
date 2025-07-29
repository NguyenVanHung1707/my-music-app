package com.example.music.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import com.example.music.data.AudioTrack
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class AudioEngine(private val mBackgroundAudioService: BackgroundAudioService?) :
    PlayerAdapter, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    private val mContext: Context = mBackgroundAudioService!!.applicationContext
    private val mAudioManager: AudioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    private var mMediaPlayer: MediaPlayer? = null
    private var mPlaybackInfoListener: PlaybackInfoListener? = null
    private var mExecutor: ScheduledExecutorService? = null
    private var mSeekBarPositionUpdateTask: Runnable? = null
    private var mSelectedSong: AudioTrack? = null
    private var mSongs: List<AudioTrack>? = null
    private var sReplaySong = false
    
    @PlaybackInfoListener.State
    private var mState: Int = PlaybackInfoListener.State.INVALID
    
    private var mPlaybackNotifier: PlaybackNotifier? = null
    private var mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
    private var mPlayOnFocusGain: Boolean = false

    private val mOnAudioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> mCurrentAudioFocusState = AUDIO_FOCUSED
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> 
                mCurrentAudioFocusState = AUDIO_NO_FOCUS_CAN_DUCK
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
                mPlayOnFocusGain = isMediaPlayer() && (mState == PlaybackInfoListener.State.PLAYING || mState == PlaybackInfoListener.State.RESUMED)
            }
            AudioManager.AUDIOFOCUS_LOSS -> mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
        }

        if (mMediaPlayer != null) {
            configurePlayerState()
        }
    }

    override fun initMediaPlayer() {
        try {
            if (mMediaPlayer != null) {
                mMediaPlayer!!.reset()
            } else {
                mMediaPlayer = MediaPlayer()
                mMediaPlayer!!.setOnPreparedListener(this)
                mMediaPlayer!!.setOnCompletionListener(this)
                mMediaPlayer!!.setOnErrorListener(this)
                mMediaPlayer!!.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK)
                mMediaPlayer!!.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                mPlaybackNotifier = mBackgroundAudioService!!.playbackNotifier
            }
            
            tryToGetAudioFocus()
            
            // Set data source based on path type
            val path = mSelectedSong!!.path
            when {
                path.isNullOrEmpty() -> throw IllegalArgumentException("Song path is null or empty")
                path.startsWith("content://") -> {
                    val uri = Uri.parse(path)
                    mMediaPlayer!!.setDataSource(mContext, uri)
                }
                path.startsWith("android.resource://") -> {
                    val uri = Uri.parse(path)
                    mMediaPlayer!!.setDataSource(mContext, uri)
                }
                else -> {
                    mMediaPlayer!!.setDataSource(path)
                }
            }
            
            // Use prepareAsync for modern Android
            mMediaPlayer!!.prepareAsync()
            
        } catch (e: Exception) {
            e.printStackTrace()
            skip(true)
        }
    }

    override fun onPrepared(mediaPlayer: MediaPlayer) {
        startUpdatingCallbackWithPosition()
        setStatus(PlaybackInfoListener.State.PLAYING)
        
        // Start foreground service notification
        if (mBackgroundAudioService != null && mPlaybackNotifier != null) {
            val notification = mPlaybackNotifier!!.createNotification()
            mBackgroundAudioService!!.startForeground(PlaybackNotifier.NOTIFICATION_ID, notification)
        }
    }

    override fun onCompletion(mediaPlayer: MediaPlayer) {
        if (mPlaybackInfoListener != null) {
            mPlaybackInfoListener!!.onStateChanged(PlaybackInfoListener.State.COMPLETED)
            mPlaybackInfoListener!!.onPlaybackCompleted()
        }

        if (sReplaySong) {
            if (isMediaPlayer()) {
                resetSong()
            }
            sReplaySong = false
        } else {
            skip(true)
        }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        setStatus(PlaybackInfoListener.State.PAUSED)
        return true
    }

    private fun tryToGetAudioFocus() {
        val result = mAudioManager.requestAudioFocus(
            mOnAudioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mCurrentAudioFocusState = AUDIO_FOCUSED
        } else {
            mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
        }
    }

    private fun giveUpAudioFocus() {
        if (mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
        }
    }

    private fun setStatus(@PlaybackInfoListener.State state: Int) {
        mState = state
        if (mPlaybackInfoListener != null) {
            mPlaybackInfoListener!!.onStateChanged(state)
        }
    }

    private fun resumeMediaPlayer() {
        if (!isPlaying() && mMediaPlayer != null) {
            mMediaPlayer!!.start()
            setStatus(PlaybackInfoListener.State.RESUMED)
            startUpdatingCallbackWithPosition()
            
            if (mBackgroundAudioService != null && mPlaybackNotifier != null) {
                val notification = mPlaybackNotifier!!.createNotification()
                mBackgroundAudioService!!.startForeground(PlaybackNotifier.NOTIFICATION_ID, notification)
            }
        }
    }

    private fun pauseMediaPlayer() {
        setStatus(PlaybackInfoListener.State.PAUSED)
        if (mMediaPlayer != null) {
            mMediaPlayer!!.pause()
        }
        stopUpdatingCallbackWithPosition()
        
        if (mBackgroundAudioService != null) {
            mBackgroundAudioService!!.stopForeground(false)
        }
        
        if (mPlaybackNotifier != null) {
            val notification = mPlaybackNotifier!!.createNotification()
            mPlaybackNotifier!!.notificationManager.notify(PlaybackNotifier.NOTIFICATION_ID, notification)
        }
    }

    private fun resetSong() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.seekTo(0)
            mMediaPlayer!!.start()
            setStatus(PlaybackInfoListener.State.PLAYING)
        }
    }

    private fun startUpdatingCallbackWithPosition() {
        if (mExecutor == null) {
            mExecutor = Executors.newSingleThreadScheduledExecutor()
        }
        if (mSeekBarPositionUpdateTask == null) {
            mSeekBarPositionUpdateTask = Runnable { updateProgressCallbackTask() }
        }

        mExecutor!!.scheduleAtFixedRate(
            mSeekBarPositionUpdateTask,
            0,
            1000,
            TimeUnit.MILLISECONDS
        )
    }

    private fun stopUpdatingCallbackWithPosition() {
        if (mExecutor != null) {
            mExecutor!!.shutdownNow()
            mExecutor = null
            mSeekBarPositionUpdateTask = null
        }
    }

    private fun updateProgressCallbackTask() {
        try {
            if (isMediaPlayer() && mMediaPlayer!!.isPlaying) {
                val currentPosition = mMediaPlayer!!.currentPosition
                if (mPlaybackInfoListener != null) {
                    mPlaybackInfoListener!!.onPositionChanged(currentPosition)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun configurePlayerState() {
        if (mCurrentAudioFocusState == AUDIO_NO_FOCUS_NO_DUCK) {
            pauseMediaPlayer()
        } else {
            if (mCurrentAudioFocusState == AUDIO_NO_FOCUS_CAN_DUCK) {
                mMediaPlayer!!.setVolume(VOLUME_DUCK, VOLUME_DUCK)
            } else {
                mMediaPlayer!!.setVolume(VOLUME_NORMAL, VOLUME_NORMAL)
            }

            if (mPlayOnFocusGain) {
                resumeMediaPlayer()
                mPlayOnFocusGain = false
            }
        }
    }

    // PlayerAdapter implementation
    override fun isMediaPlayer(): Boolean = mMediaPlayer != null
    override fun isPlaying(): Boolean = isMediaPlayer() && mMediaPlayer!!.isPlaying
    override fun isReset(): Boolean = sReplaySong
    override fun getCurrentSong(): AudioTrack? = mSelectedSong
    override fun getState(): Int = mState
    override fun getPlayerPosition(): Int = mMediaPlayer?.currentPosition ?: 0
    override fun getMediaPlayer(): MediaPlayer? = mMediaPlayer

    override fun release() {
        if (isMediaPlayer()) {
            mMediaPlayer!!.release()
            mMediaPlayer = null
            giveUpAudioFocus()
        }
        stopUpdatingCallbackWithPosition()
    }

    override fun resumeOrPause() {
        if (isPlaying()) {
            pauseMediaPlayer()
        } else {
            resumeMediaPlayer()
        }
    }

    override fun reset() {
        sReplaySong = !sReplaySong
    }

    override fun instantReset() {
        if (isMediaPlayer()) {
            if (mMediaPlayer!!.currentPosition < 5000) {
                skip(false)
            } else {
                resetSong()
            }
        }
    }

    override fun skip(isNext: Boolean) {
        getSkipSong(isNext)
    }

    private fun getSkipSong(isNext: Boolean) {
        if (mSongs == null || mSelectedSong == null) return
        
        val currentIndex = mSongs!!.indexOf(mSelectedSong)
        val index: Int

        try {
            index = if (isNext) currentIndex + 1 else currentIndex - 1
            mSelectedSong = mSongs!![index]
        } catch (e: IndexOutOfBoundsException) {
            mSelectedSong = if (currentIndex != 0) mSongs!![0] else mSongs!![mSongs!!.size - 1]
        }

        initMediaPlayer()
    }

    override fun seekTo(position: Int) {
        if (isMediaPlayer()) {
            mMediaPlayer!!.seekTo(position)
        }
    }

    override fun setPlaybackInfoListener(playbackInfoListener: PlaybackInfoListener) {
        mPlaybackInfoListener = playbackInfoListener
    }

    override fun registerNotificationActionsReceiver(isRegister: Boolean) {
        // Implementation would go here if needed
    }

    override fun setCurrentSong(song: AudioTrack, songs: List<AudioTrack>) {
        mSelectedSong = song
        mSongs = songs
    }

    override fun onPauseActivity() {
        stopUpdatingCallbackWithPosition()
    }

    override fun onResumeActivity() {
        startUpdatingCallbackWithPosition()
    }

    companion object {
        private const val VOLUME_DUCK = 0.2f
        private const val VOLUME_NORMAL = 1.0f
        private const val AUDIO_NO_FOCUS_NO_DUCK = 0
        private const val AUDIO_NO_FOCUS_CAN_DUCK = 1
        private const val AUDIO_FOCUSED = 2
    }
} 