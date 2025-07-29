package com.example.music.audio

import android.media.MediaPlayer
import com.example.music.data.AudioTrack
import com.example.music.audio.PlaybackInfoListener.*

interface PlayerAdapter {
    fun isMediaPlayer(): Boolean
    fun isPlaying(): Boolean
    fun isReset(): Boolean
    fun getCurrentSong(): AudioTrack?
    @State fun getState(): Int
    fun getPlayerPosition(): Int
    fun getMediaPlayer(): MediaPlayer?
    fun initMediaPlayer()
    fun release()
    fun resumeOrPause()
    fun reset()
    fun instantReset()
    fun skip(isNext: Boolean)
    fun seekTo(position: Int)
    fun setPlaybackInfoListener(playbackInfoListener: PlaybackInfoListener)
    fun registerNotificationActionsReceiver(isRegister: Boolean)
    fun setCurrentSong(song: AudioTrack, songs: List<AudioTrack>)
    fun onPauseActivity()
    fun onResumeActivity()
} 