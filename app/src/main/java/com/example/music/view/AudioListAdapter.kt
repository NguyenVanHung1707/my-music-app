package com.example.music.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.music.R
import com.example.music.data.AudioTrack

class AudioListAdapter(
    private var tracks: List<AudioTrack>,
    private val songClicked: SongClicked
) : RecyclerView.Adapter<AudioListAdapter.ViewHolder>() {

    private var selectedPosition = -1

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(viewGroup.context).inflate(R.layout.track_item, viewGroup, false)
        )
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val track = tracks[position]
        viewHolder.bind(track, position)
        
        // Handle selection highlighting
        viewHolder.mainItem.isSelected = position == selectedPosition
        
        viewHolder.mainItem.setOnClickListener {
            val currentPosition = viewHolder.bindingAdapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            // Update selection
            val previousSelected = selectedPosition
            selectedPosition = currentPosition

            // Refresh previous selected item
            if (previousSelected != -1) {
                notifyItemChanged(previousSelected)
            }

            // Refresh current selected item
            notifyItemChanged(selectedPosition)

            // Callback click
            songClicked.onSongClicked(track)
        }
    }

    override fun getItemCount(): Int = tracks.size

    fun addSongs(songs: MutableList<AudioTrack>) {
        tracks = songs
        notifyDataSetChanged()
    }

    fun setSelectedTrack(trackId: Int) {
        val newPosition = tracks.indexOfFirst { it.id == trackId }
        if (newPosition != -1) {
            val previousSelected = selectedPosition
            selectedPosition = newPosition

            if (previousSelected != -1) {
                notifyItemChanged(previousSelected)
            }
            notifyItemChanged(selectedPosition)
        }
    }

    interface SongClicked {
        fun onSongClicked(song: AudioTrack)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.textViewSongTitle)
        private val artist: TextView = itemView.findViewById(R.id.textViewArtistName)
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val mainItem: ConstraintLayout = itemView.findViewById(R.id.mainConstraint)

        fun bind(track: AudioTrack, position: Int) {
            title.text = track.title
            artist.text = track.artist
            imageView.setImageResource(R.drawable.ic_music_display)
            
            // Handle selection highlighting
            val isSelected = position == selectedPosition
            mainItem.setBackgroundResource(
                if (isSelected) {
                    R.drawable.selected_bg
                } else {
                    R.drawable.item_background
                }
            )
        }
    }
} 