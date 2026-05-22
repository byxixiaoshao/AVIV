package com.bicy.whitenoise.yODW.w0GA

import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bicy.whitenoise.R
import com.bicy.whitenoise.xnef.MusicTrack

class PlaylistAdapter(
    initialOnTrackClick: (Int) -> Unit
) : ListAdapter<PlaylistAdapter.TrackItem, PlaylistAdapter.ViewHolder>(DiffCallback) {

    private var onTrackClick: (Int) -> Unit = initialOnTrackClick
    
    fun updateOnTrackClick(newOnTrackClick: (Int) -> Unit) {
        onTrackClick = newOnTrackClick
    }

    data class TrackItem(
        val track: MusicTrack,
        val index: Int,
        val isPlaying: Boolean
    )

    object DiffCallback : DiffUtil.ItemCallback<TrackItem>() {
        override fun areItemsTheSame(oldItem: TrackItem, newItem: TrackItem): Boolean {
            return oldItem.track.id == newItem.track.id
        }

        override fun areContentsTheSame(oldItem: TrackItem, newItem: TrackItem): Boolean {
            return oldItem.track.id == newItem.track.id &&
                   oldItem.isPlaying == newItem.isPlaying &&
                   oldItem.track.title == newItem.track.title &&
                   oldItem.track.artist == newItem.track.artist
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.title)
        val artist: TextView = view.findViewById(R.id.artist)
        val root: View = view.findViewById(R.id.root)
        val content: View = view.findViewById(R.id.content)
        val shadow: View = view.findViewById(R.id.shadow)
    }

    private var surfaceColor: Int = 0xFFFFFFFF.toInt()
    private var onSurfaceColor: Int = 0xFF000000.toInt()
    private var secondaryTextColor: Int = 0xFF666666.toInt()
    private var primaryColor: Int = 0xFF6200EE.toInt()
    private var onPrimaryColor: Int = 0xFFFFFFFF.toInt()
    private var strokeColor: Int = 0x33000000

    fun setColors(
        surface: Int,
        onSurface: Int,
        secondaryText: Int,
        primary: Int,
        onPrimary: Int
    ) {
        surfaceColor = surface
        onSurfaceColor = onSurface
        secondaryTextColor = secondaryText
        primaryColor = primary
        onPrimaryColor = onPrimary
        strokeColor = (0x33000000 or (onSurface and 0x00FFFFFF))
    }
    
    private fun createBackgroundDrawable(backgroundColor: Int, strokeColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(backgroundColor)
            setStroke(2, strokeColor)
            cornerRadius = 24f
        }
    }
    
    private fun createShadowDrawable(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(0x40000000)
            cornerRadius = 24f
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist, parent, false)
        val holder = ViewHolder(view)
        
        holder.shadow.background = createShadowDrawable()
        
        holder.content.post {
            val density = parent.context.resources.displayMetrics.density
            val cornerRadius = 8 * density
            
            holder.content.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
                }
            }
            holder.content.clipToOutline = true
        }
        
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        
        holder.title.text = item.track.title
        holder.artist.text = item.track.artist ?: "未知艺术家"
        
        val density = holder.itemView.context.resources.displayMetrics.density
        val offsetX = if (item.isPlaying) (-8 * density) else 0f
        
        if (item.isPlaying) {
            holder.title.setTypeface(null, Typeface.BOLD)
            holder.title.setTextColor(onPrimaryColor)
            holder.content.background = createBackgroundDrawable(primaryColor, strokeColor)
        } else {
            holder.title.setTypeface(null, Typeface.NORMAL)
            holder.title.setTextColor(onSurfaceColor)
            holder.content.background = createBackgroundDrawable(surfaceColor, strokeColor)
        }
        
        holder.artist.setTextColor(secondaryTextColor)
        
        holder.root.post {
            holder.root.animate()
                .translationX(offsetX)
                .setDuration(200)
                .start()
        }
        
        holder.content.setOnClickListener {
            onTrackClick(item.index)
        }
    }
    
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            val item = getItem(position)
            val isPlaying = payloads.first() as? Boolean ?: item.isPlaying
            
            val density = holder.itemView.context.resources.displayMetrics.density
            val offsetX = if (isPlaying) (-8 * density) else 0f
            
            if (isPlaying) {
                holder.title.setTypeface(null, Typeface.BOLD)
                holder.title.setTextColor(onPrimaryColor)
                holder.content.background = createBackgroundDrawable(primaryColor, strokeColor)
            } else {
                holder.title.setTypeface(null, Typeface.NORMAL)
                holder.title.setTextColor(onSurfaceColor)
                holder.content.background = createBackgroundDrawable(surfaceColor, strokeColor)
            }
            
            holder.artist.setTextColor(secondaryTextColor)
            
            holder.root.post {
                holder.root.animate()
                    .translationX(offsetX)
                    .setDuration(200)
                    .start()
            }
        }
    }
}
