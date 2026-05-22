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

data class CategoryItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val iconRes: Int? = null,
    val showArrow: Boolean = false
)

class CategoryAdapter(
    initialOnItemClick: (CategoryItem) -> Unit
) : ListAdapter<CategoryItem, CategoryAdapter.ViewHolder>(DiffCallback) {

    private var onItemClick: (CategoryItem) -> Unit = initialOnItemClick
    
    fun updateOnItemClick(newOnItemClick: (CategoryItem) -> Unit) {
        onItemClick = newOnItemClick
    }

    object DiffCallback : DiffUtil.ItemCallback<CategoryItem>() {
        override fun areItemsTheSame(oldItem: CategoryItem, newItem: CategoryItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CategoryItem, newItem: CategoryItem): Boolean {
            return oldItem == newItem
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val title: TextView = view.findViewById(R.id.title)
        val subtitle: TextView = view.findViewById(R.id.subtitle)
        val arrow: ImageView = view.findViewById(R.id.arrow)
        val content: View = view.findViewById(R.id.content)
        val shadow: View = view.findViewById(R.id.shadow)
    }

    private var surfaceColor: Int = 0xFFFFFFFF.toInt()
    private var onSurfaceColor: Int = 0xFF000000.toInt()
    private var secondaryTextColor: Int = 0xFF666666.toInt()
    private var strokeColor: Int = 0x33000000

    fun setColors(surface: Int, onSurface: Int, secondaryText: Int) {
        surfaceColor = surface
        onSurfaceColor = onSurface
        secondaryTextColor = secondaryText
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
            .inflate(R.layout.item_category, parent, false)
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
        
        holder.title.text = item.title
        holder.title.setTextColor(onSurfaceColor)
        
        if (item.subtitle != null) {
            holder.subtitle.text = item.subtitle
            holder.subtitle.setTextColor(secondaryTextColor)
            holder.subtitle.visibility = View.VISIBLE
        } else {
            holder.subtitle.visibility = View.GONE
        }
        
        if (item.iconRes != null) {
            holder.icon.setImageResource(item.iconRes)
            holder.icon.visibility = View.VISIBLE
        } else {
            holder.icon.visibility = View.GONE
        }
        
        if (item.showArrow) {
            holder.arrow.setImageResource(R.drawable.ic_arrow_right)
            holder.arrow.visibility = View.VISIBLE
        } else {
            holder.arrow.visibility = View.GONE
        }
        
        holder.content.background = createBackgroundDrawable(surfaceColor, strokeColor)
        
        holder.content.setOnClickListener {
            onItemClick(item)
        }
    }
}
