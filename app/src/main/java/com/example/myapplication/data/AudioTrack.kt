package com.example.music.data

data class AudioTrack(
    val id: Int = 0,
    val title: String,
    val artist: String,
    val album: String = "Unknown Album",
    val duration: Int = 0, // milliseconds
    val path: String? = null,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val artistId: Int = 0
) 