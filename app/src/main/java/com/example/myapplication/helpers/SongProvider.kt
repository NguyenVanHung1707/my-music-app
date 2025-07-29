package com.example.music.helpers

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import com.example.music.data.AudioTrack

object SongProvider {
    private val TITLE = 0
    private val TRACK = 1
    private val YEAR = 2
    private val DURATION = 3
    private val PATH = 4
    private val ALBUM = 5
    private val ARTIST_ID = 6
    private val ARTIST = 7

    private val BASE_PROJECTION = arrayOf(
        MediaStore.Audio.AudioColumns.TITLE, // 0
        MediaStore.Audio.AudioColumns.TRACK, // 1
        MediaStore.Audio.AudioColumns.YEAR, // 2
        MediaStore.Audio.AudioColumns.DURATION, // 3
        MediaStore.Audio.AudioColumns.DATA, // 4
        MediaStore.Audio.AudioColumns.ALBUM, // 5
        MediaStore.Audio.AudioColumns.ARTIST_ID, // 6
        MediaStore.Audio.AudioColumns.ARTIST // 7
    )

    fun getAllDeviceSongs(context: Context): MutableList<AudioTrack> {
        val cursor = makeSongCursor(context)
        return getSongs(cursor)
    }

    private fun getSongs(cursor: Cursor?): MutableList<AudioTrack> {
        val songs = ArrayList<AudioTrack>()
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val song = getSongFromCursorImpl(cursor)
                // Filter: Only songs >= 10 seconds (more inclusive)
                if (song.duration >= 10000) {
                    songs.add(song)
                }
            } while (cursor.moveToNext())
        }
        cursor?.close()
        return songs
    }

    private fun getSongFromCursorImpl(cursor: Cursor): AudioTrack {
        val title = cursor.getString(TITLE) ?: "Unknown Track"
        val trackNumber = cursor.getInt(TRACK)
        val year = cursor.getInt(YEAR)
        val duration = cursor.getInt(DURATION)
        val path = cursor.getString(PATH) ?: ""
        val album = cursor.getString(ALBUM) ?: "Unknown Album"
        val artistId = cursor.getInt(ARTIST_ID)
        val artist = cursor.getString(ARTIST) ?: "Unknown Artist"

        return AudioTrack(
            id = cursor.position, // Use cursor position as ID
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            path = path,
            trackNumber = trackNumber,
            year = year,
            artistId = artistId
        )
    }

    private fun makeSongCursor(context: Context): Cursor? {
        return try {
            // Modern Android query - search multiple locations
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1 AND ${MediaStore.Audio.Media.DURATION} > 10000"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                BASE_PROJECTION,
                selection,
                null,
                sortOrder
            )
        } catch (e: SecurityException) {
            // If no permission, return null
            null
        } catch (e: Exception) {
            // Any other error, return null
            null
        }
    }

    // Fast loading music - System sounds + Android defaults
    fun getSampleTracks(): List<AudioTrack> {
        return listOf(
            // System Ringtones Collection (Instant playback)
            AudioTrack(
                id = 1,
                title = "Default Ringtone",
                artist = "Android System",
                album = "System Ringtones",
                duration = 30000,
                path = "content://media/internal/audio/media/1"  // Default ringtone
            ),
            AudioTrack(
                id = 2,
                title = "Notification Sound",
                artist = "Android System",
                album = "System Notifications",
                duration = 3000,
                path = "content://settings/system/notification_sound"
            ),
            AudioTrack(
                id = 3,
                title = "Alarm Sound",
                artist = "Android System",
                album = "System Alarms",
                duration = 10000,
                path = "content://settings/system/alarm_alert"
            ),
            AudioTrack(
                id = 4,
                title = "Glass",
                artist = "Android Ringtones",
                album = "Classic Tones",
                duration = 25000,
                path = "content://media/internal/audio/media/28" // Common Android ringtone
            ),
            AudioTrack(
                id = 5,
                title = "Chime",
                artist = "Android Notifications",
                album = "Alert Sounds",
                duration = 2000,
                path = "content://media/internal/audio/media/62" // Common notification
            ),
            AudioTrack(
                id = 6,
                title = "Beep",
                artist = "Android System",
                album = "Simple Sounds",
                duration = 1000,
                path = "content://media/internal/audio/media/39"
            ),
            AudioTrack(
                id = 7,
                title = "Ding",
                artist = "Android Notifications",
                album = "Alert Sounds",
                duration = 1500,
                path = "content://media/internal/audio/media/41"
            ),
            AudioTrack(
                id = 8,
                title = "Buzz",
                artist = "Android System",
                album = "Vibration Sounds",
                duration = 2000,
                path = "content://media/internal/audio/media/45"
            ),
            AudioTrack(
                id = 9,
                title = "Pop",
                artist = "Android UI",
                album = "Interface Sounds",
                duration = 500,
                path = "content://media/internal/audio/media/48"
            ),
            AudioTrack(
                id = 10,
                title = "Click",
                artist = "Android UI",
                album = "Interface Sounds",
                duration = 300,
                path = "content://media/internal/audio/media/50"
            ),
            
            // Demo songs with realistic names but using system paths
            AudioTrack(
                id = 11,
                title = "Sóng Gió (Demo)",
                artist = "Jack - K-ICM",
                album = "V-Pop Hits",
                duration = 240000, // 4:00
                path = "content://settings/system/ringtone" // Fallback to system
            ),
            AudioTrack(
                id = 12,
                title = "Như Ngày Hôm Qua (Demo)",
                artist = "Sơn Tùng M-TP",
                album = "V-Pop Hits",
                duration = 280000, // 4:40
                path = "content://settings/system/notification_sound"
            ),
            AudioTrack(
                id = 13,
                title = "Chạy Ngay Đi (Demo)",
                artist = "Sơn Tùng M-TP",
                album = "V-Pop Hits",
                duration = 220000, // 3:40
                path = "content://settings/system/alarm_alert"
            ),
            AudioTrack(
                id = 14,
                title = "Em Của Ngày Hôm Qua (Demo)",
                artist = "Sơn Tùng M-TP",
                album = "Ballad Collection",
                duration = 250000, // 4:10
                path = "content://media/internal/audio/media/1"
            ),
            AudioTrack(
                id = 15,
                title = "Lạc Trôi (Demo)",
                artist = "Sơn Tùng M-TP",
                album = "Pop Vietnamese",
                duration = 270000, // 4:30
                path = "content://media/internal/audio/media/28"
            ),
            AudioTrack(
                id = 16,
                title = "Summer Vibes (Demo)",
                artist = "Electronic Band",
                album = "Electronic Hits",
                duration = 180000, // 3:00
                path = "content://media/internal/audio/media/62"
            ),
            AudioTrack(
                id = 17,
                title = "Jazz Café (Demo)",
                artist = "Smooth Jazz",
                album = "Jazz Lounge",
                duration = 320000, // 5:20
                path = "content://media/internal/audio/media/39"
            ),
            AudioTrack(
                id = 18,
                title = "Rock Anthem (Demo)",
                artist = "Demo Rockers",
                album = "Rock Collection",
                duration = 240000, // 4:00
                path = "content://media/internal/audio/media/41"
            ),
            AudioTrack(
                id = 19,
                title = "Piano Dreams (Demo)",
                artist = "Classical Demo",
                album = "Instrumental",
                duration = 180000, // 3:00
                path = "content://media/internal/audio/media/45"
            ),
            AudioTrack(
                id = 20,
                title = "Study Music (Demo)",
                artist = "Lo-Fi Beats",
                album = "Focus Collection",
                duration = 360000, // 6:00
                path = "content://media/internal/audio/media/48"
            )
        )
    }

    fun getMusicData(context: Context, hasPermission: Boolean): List<AudioTrack> {
        android.util.Log.d("SongProvider", "🎵 ===== LOADING MUSIC DATA =====")
        android.util.Log.d("SongProvider", "📱 Has permission: $hasPermission")
        
        return if (hasPermission) {
            android.util.Log.d("SongProvider", "🔍 Scanning device for real music...")
            
            val realSongs = getAllDeviceSongs(context)
            android.util.Log.d("SongProvider", "📊 Found ${realSongs.size} real songs from MediaStore")
            
            if (realSongs.isNotEmpty()) {
                // Log first few songs for debug
                realSongs.take(3).forEach { song ->
                    android.util.Log.d("SongProvider", "🎶 Real song: ${song.title} - ${song.artist} (${song.duration}ms)")
                    android.util.Log.d("SongProvider", "📁 Path: ${song.path}")
                }
                
                android.util.Log.d("SongProvider", "✅ Using ${realSongs.size} REAL songs from device")
                realSongs
            } else {
                android.util.Log.w("SongProvider", "⚠️ No real songs found - falling back to samples")
                getSampleTracks()
            }
        } else {
            android.util.Log.w("SongProvider", "❌ No permission - using sample tracks only")
            getSampleTracks()
        }
    }
} 