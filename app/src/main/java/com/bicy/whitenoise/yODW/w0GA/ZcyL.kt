package com.bicy.whitenoise.yODW.w0GA

import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bicy.whitenoise.R

sealed class FolderItem {
    data class Directory(val name: String) : FolderItem()
    data class Track(val track: com.bicy.whitenoise.xnef.MusicTrack, val isPlaying: Boolean) : FolderItem()
}

class FolderContentAdapter(
    initialOnDirectoryClick: (String) -> Unit,
    initialOnTrackClick: (com.bicy.whitenoise.xnef.MusicTrack) -> Unit
) : ListAdapter<FolderItem, RecyclerView.ViewHolder>(DiffCallback) {

    private var onDirectoryClick: (String) -> Unit = initialOnDirectoryClick
    private var onTrackClick: (com.bicy.whitenoise.xnef.MusicTrack) -> Unit = initialOnTrackClick
    
    fun updateClickListeners(
        newOnDirectoryClick: (String) -> Unit,
        newOnTrackClick: (com.bicy.whitenoise.xnef.MusicTrack) -> Unit
    ) {
        onDirectoryClick = newOnDirectoryClick
        onTrackClick = newOnTrackClick
    }

    object DiffCallback : DiffUtil.ItemCallback<FolderItem>() {
        override fun areItemsTheSame(oldItem: FolderItem, newItem: FolderItem): Boolean {
            return when {
                oldItem is FolderItem.Directory && newItem is FolderItem.Directory -> oldItem.name == newItem.name
                oldItem is FolderItem.Track && newItem is FolderItem.Track -> oldItem.track.id == newItem.track.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: FolderItem, newItem: FolderItem): Boolean {
            return oldItem == newItem
        }
    }

    class DirectoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val title: TextView = view.findViewById(R.id.title)
        val arrow: ImageView = view.findViewById(R.id.arrow)
        val content: View = view.findViewById(R.id.content)
        val shadow: View = view.findViewById(R.id.shadow)
    }

    class TrackViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = view.findViewById(R.id.root)
        val title: TextView = view.findViewById(R.id.title)
        val artist: TextView = view.findViewById(R.id.artist)
        val content: View = view.findViewById(R.id.content)
        val shadow: View = view.findViewById(R.id.shadow)
    }

    private var surfaceColor: Int = 0xFFFFFFFF.toInt()
    private var onSurfaceColor: Int = 0xFF000000.toInt()
    private var secondaryTextColor: Int = 0xFF666666.toInt()
    private var primaryColor: Int = 0xFF6200EE.toInt()
    private var onPrimaryColor: Int = 0xFFFFFFFF.toInt()
    private var strokeColor: Int = 0x33000000

    fun setColors(surface: Int, onSurface: Int, secondaryText: Int, primary: Int, onPrimary: Int) {
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

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is FolderItem.Directory -> 0
            is FolderItem.Track -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category, parent, false)
                val holder = DirectoryViewHolder(view)
                
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
                
                holder
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_playlist, parent, false)
                val holder = TrackViewHolder(view)
                
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
                
                holder
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        
        when (holder) {
            is DirectoryViewHolder -> {
                val dirItem = item as FolderItem.Directory
                holder.title.text = dirItem.name
                holder.title.setTextColor(onSurfaceColor)
                holder.icon.setImageResource(R.drawable.ic_folder)
                holder.icon.visibility = View.VISIBLE
                holder.arrow.setImageResource(R.drawable.ic_arrow_right)
                holder.arrow.visibility = View.VISIBLE
                holder.content.background = createBackgroundDrawable(surfaceColor, strokeColor)
                holder.content.setOnClickListener { onDirectoryClick(dirItem.name) }
            }
            is TrackViewHolder -> {
                val trackItem = item as FolderItem.Track
                holder.title.text = trackItem.track.title
                holder.artist.text = trackItem.track.artist ?: "未知艺术家"
                
                val density = holder.itemView.context.resources.displayMetrics.density
                val offsetX = if (trackItem.isPlaying) (-8 * density) else 0f
                
                if (trackItem.isPlaying) {
                    holder.title.setTextColor(onPrimaryColor)
                    holder.content.background = createBackgroundDrawable(primaryColor, strokeColor)
                } else {
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
                
                holder.content.setOnClickListener { onTrackClick(trackItem.track) }
            }
        }
    }
}
