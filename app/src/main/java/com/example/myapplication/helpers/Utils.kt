package com.example.music.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.example.music.R
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit

object Utils {

    /**
     * Lấy album art từ file audio
     */
    fun songArt(path: String, context: Context): Bitmap {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            if (retriever.embeddedPicture != null) {
                val inputStream: InputStream = ByteArrayInputStream(retriever.embeddedPicture)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                retriever.release()
                bitmap
            } else {
                getLargeIcon(context)
            }
        } catch (e: Exception) {
            retriever.release()
            getLargeIcon(context)
        }
    }

    /**
     * Lấy default icon khi không có album art
     */
    private fun getLargeIcon(context: Context): Bitmap {
        return BitmapFactory.decodeResource(context.resources, R.drawable.ic_music_display)
    }

    /**
     * Format duration từ milliseconds thành MM:SS
     */
    fun formatDuration(duration: Long): String {
        return String.format(
            Locale.getDefault(), "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(duration),
            TimeUnit.MILLISECONDS.toSeconds(duration) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
        )
    }

    /**
     * Format track number (loại bỏ phần nghìn nếu có)
     */
    fun formatTrack(trackNumber: Int): Int {
        var formatted = trackNumber
        if (trackNumber >= 1000) {
            formatted = trackNumber % 1000
        }
        return formatted
    }

    /**
     * Chuyển đổi milliseconds thành seconds
     */
    fun millisToSeconds(milliseconds: Long): Int {
        return (milliseconds / 1000).toInt()
    }

    /**
     * Chuyển đổi seconds thành milliseconds  
     */
    fun secondsToMillis(seconds: Int): Long {
        return seconds * 1000L
    }

    /**
     * Kiểm tra file có tồn tại không
     */
    fun fileExists(path: String): Boolean {
        return try {
            java.io.File(path).exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Lấy file extension từ path
     */
    fun getFileExtension(path: String): String {
        return try {
            path.substring(path.lastIndexOf(".") + 1).lowercase()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Check if file is audio format
     */
    fun isAudioFile(path: String): Boolean {
        val extension = getFileExtension(path)
        val audioFormats = listOf("mp3", "aac", "wav", "flac", "ogg", "m4a", "wma")
        return audioFormats.contains(extension)
    }
} 