package com.example.music.view

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.music.R
import com.example.music.data.AudioTrack
import com.example.music.audio.PlaybackNotifier
import com.example.music.audio.BackgroundAudioService
import com.example.music.audio.PlaybackInfoListener
import com.example.music.audio.PlayerAdapter
import com.example.music.helpers.SongProvider

class HomeScreenActivity : AppCompatActivity(), View.OnClickListener, AudioListAdapter.SongClicked {

    private lateinit var seekBar: SeekBar
    private lateinit var playPause: ImageButton
    private lateinit var next: ImageButton
    private lateinit var previous: ImageButton
    private lateinit var songTitle: TextView
    private lateinit var imageViewControl: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var audioListAdapter: AudioListAdapter

    private var mBackgroundAudioService: BackgroundAudioService? = null
    private var mIsBound: Boolean = false
    private var mPlayerAdapter: PlayerAdapter? = null
    private var mUserIsSeeking = false
    private var mPlaybackListener: PlaybackListener? = null
    private var deviceSongs: MutableList<AudioTrack>? = null
    private var mPlaybackNotifier: PlaybackNotifier? = null

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            mBackgroundAudioService = (iBinder as BackgroundAudioService.LocalBinder).instance
            mPlayerAdapter = mBackgroundAudioService!!.audioEngine
            mPlaybackNotifier = mBackgroundAudioService!!.playbackNotifier

            if (mPlaybackListener == null) {
                mPlaybackListener = PlaybackListener()
                mPlayerAdapter!!.setPlaybackInfoListener(mPlaybackListener!!)
            }
            if (mPlayerAdapter != null && mPlayerAdapter!!.isPlaying()) {
                restorePlayerStatus()
            }
            checkReadStoragePermissions()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBackgroundAudioService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        doBindService()
        setViews()
        initializeSeekBar()
    }

    override fun onPause() {
        super.onPause()
        doUnbindService()
        if (mPlayerAdapter != null && mPlayerAdapter!!.isMediaPlayer()) {
            mPlayerAdapter!!.onPauseActivity()
        }
    }

    override fun onResume() {
        super.onResume()
        doBindService()
        if (mPlayerAdapter != null && mPlayerAdapter!!.isPlaying()) {
            restorePlayerStatus()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            getMusic() // Load sample data instead
        } else {
            getMusic()
        }
    }

    private fun setViews() {
        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Sample Music Player"

        // Initialize views
        playPause = findViewById(R.id.buttonPlayPause)
        next = findViewById(R.id.buttonNext)
        previous = findViewById(R.id.buttonPrevious)
        seekBar = findViewById(R.id.seekBar)
        songTitle = findViewById(R.id.songTitle)
        imageViewControl = findViewById(R.id.imageViewControl)
        recyclerView = findViewById(R.id.recyclerView)

        // Set click listeners
        playPause.setOnClickListener(this)
        next.setOnClickListener(this)
        previous.setOnClickListener(this)

        // Initialize with sample data first
        deviceSongs = SongProvider.getSampleTracks().toMutableList()
    }

    private fun checkReadStoragePermissions() {
        android.util.Log.d("MainActivity", "🔍 ===== CHECKING STORAGE PERMISSIONS =====")
        
        // Check for modern permission (Android 13+)
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        android.util.Log.d("MainActivity", "📱 Android version: ${android.os.Build.VERSION.SDK_INT}")
        android.util.Log.d("MainActivity", "🔑 Required permission: $permission")

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.w("MainActivity", "❌ Permission not granted - requesting...")
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1)
        } else {
            android.util.Log.d("MainActivity", "✅ Permission already granted - loading music...")
            getMusic()
        }
    }

    private fun getMusic() {
        android.util.Log.d("MainActivity", "🎵 ===== LOADING MUSIC =====")
        
        val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        android.util.Log.d("MainActivity", "🔑 Final permission check: $hasPermission")
        
        // ALWAYS try MediaStore first if we have permission
        deviceSongs = SongProvider.getMusicData(this, hasPermission).toMutableList()
        
        android.util.Log.d("MainActivity", "📊 Total songs loaded: ${deviceSongs?.size}")
        deviceSongs?.take(3)?.forEach { song ->
            android.util.Log.d("MainActivity", "🎶 Sample: ${song.title} - ${song.artist}")
        }
        
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        audioListAdapter = AudioListAdapter(deviceSongs!!, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = audioListAdapter
    }

    private fun updatePlayingInfo(restore: Boolean, startPlay: Boolean) {
        if (startPlay && mPlayerAdapter?.getMediaPlayer() != null) {
            mPlayerAdapter!!.getMediaPlayer()?.start()
            Handler(Looper.getMainLooper()).postDelayed({
                if (mBackgroundAudioService != null && mPlaybackNotifier != null) {
                    mBackgroundAudioService!!.startForeground(
                        PlaybackNotifier.NOTIFICATION_ID,
                        mPlaybackNotifier!!.createNotification()
                    )
                }
            }, 200)
        }

        val selectedSong = mPlayerAdapter!!.getCurrentSong()
        songTitle.text = selectedSong?.title
        val duration = selectedSong?.duration ?: 0
        seekBar.max = duration
        imageViewControl.setImageResource(R.drawable.ic_music_display)

        if (restore) {
            seekBar.progress = mPlayerAdapter!!.getPlayerPosition()
            updatePlayingStatus()

            Handler(Looper.getMainLooper()).postDelayed({
                if (mBackgroundAudioService != null && mBackgroundAudioService!!.isRestoredFromPause) {
                    mBackgroundAudioService!!.stopForeground(false)
                    if (mPlaybackNotifier != null) {
                        mPlaybackNotifier!!.notificationManager.notify(
                            PlaybackNotifier.NOTIFICATION_ID,
                            mPlaybackNotifier!!.createNotification()
                        )
                    }
                    mBackgroundAudioService!!.isRestoredFromPause = false
                }
            }, 200)
        }
    }

    private fun updatePlayingStatus() {
        val drawable = if (mPlayerAdapter!!.getState() != PlaybackInfoListener.State.PAUSED)
            R.drawable.baseline_pause_24
        else
            R.drawable.ic_play
        playPause.post { playPause.setImageResource(drawable) }
    }

    private fun restorePlayerStatus() {
        seekBar.isEnabled = mPlayerAdapter!!.isMediaPlayer()

        if (mPlayerAdapter != null && mPlayerAdapter!!.isMediaPlayer()) {
            mPlayerAdapter!!.onResumeActivity()
            updatePlayingInfo(true, false)
        }
    }

    private fun doBindService() {
        val startNotStickyIntent = Intent(this, BackgroundAudioService::class.java)
        startService(startNotStickyIntent)
        
        bindService(Intent(this, BackgroundAudioService::class.java), mConnection, Context.BIND_AUTO_CREATE)
        mIsBound = true
    }

    private fun doUnbindService() {
        if (mIsBound) {
            unbindService(mConnection)
            mIsBound = false
        }
    }

    private fun onSongSelected(song: AudioTrack, songs: List<AudioTrack>) {
        if (!seekBar.isEnabled) {
            seekBar.isEnabled = true
        }
        try {
            mPlayerAdapter!!.setCurrentSong(song, songs)
            mPlayerAdapter!!.initMediaPlayer()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun skipPrev() {
        if (checkIsPlayer()) {
            mPlayerAdapter!!.instantReset()
        }
    }

    private fun resumeOrPause() {
        if (checkIsPlayer()) {
            mPlayerAdapter!!.resumeOrPause()
        } else {
            if (deviceSongs?.isNotEmpty() == true) {
                onSongSelected(deviceSongs!![0], deviceSongs!!)
            }
        }
    }

    private fun skipNext() {
        if (checkIsPlayer()) {
            mPlayerAdapter!!.skip(true)
        }
    }

    private fun checkIsPlayer(): Boolean {
        return mPlayerAdapter?.isMediaPlayer() == true
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.buttonPlayPause -> resumeOrPause()
            R.id.buttonNext -> skipNext()
            R.id.buttonPrevious -> skipPrev()
        }
    }

    private fun initializeSeekBar() {
        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                var userSelectedPosition = 0

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    mUserIsSeeking = true
                }

                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        userSelectedPosition = progress
                    }
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    mUserIsSeeking = false
                    mPlayerAdapter?.seekTo(userSelectedPosition)
                }
            })
    }

    override fun onSongClicked(song: AudioTrack) {
        onSongSelected(song, deviceSongs!!)
        audioListAdapter.setSelectedTrack(song.id)
    }

    internal inner class PlaybackListener : PlaybackInfoListener() {
        override fun onPositionChanged(position: Int) {
            if (!mUserIsSeeking) {
                seekBar.progress = position
            }
        }

        override fun onStateChanged(@State state: Int) {
            updatePlayingStatus()
            if (mPlayerAdapter!!.getState() != State.PAUSED) {
                updatePlayingInfo(false, true)
            }
        }

        override fun onPlaybackCompleted() {
            // After playback is complete
        }
    }
} 