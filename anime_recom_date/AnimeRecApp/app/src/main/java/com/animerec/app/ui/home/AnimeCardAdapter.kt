package com.animerec.app.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.animerec.app.R
import com.animerec.app.models.AnimeContent
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions

/**
 * Adapter for anime/manga card items in the swipe stack.
 */
class AnimeCardAdapter(private val context: Context) : 
    ListAdapter<AnimeContent, AnimeCardAdapter.AnimeCardViewHolder>(AnimeContentDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimeCardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_anime_card, parent, false)
        return AnimeCardViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: AnimeCardViewHolder, position: Int) {
        val content = getItem(position)
        holder.bind(content)
    }
    
    inner class AnimeCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val coverImageView: ImageView = itemView.findViewById(R.id.anime_cover_image)
        private val titleTextView: TextView = itemView.findViewById(R.id.anime_title)
        private val genresTextView: TextView = itemView.findViewById(R.id.anime_genres)
        private val ratingTextView: TextView = itemView.findViewById(R.id.anime_rating)
        private val synopsisTextView: TextView = itemView.findViewById(R.id.anime_synopsis)
        
        fun bind(content: AnimeContent) {
            // Set title
            titleTextView.text = content.title
            
            // Set genres
            genresTextView.text = content.genres.joinToString(", ")
            
            // Set rating
            if (content.malScore > 0) {
                ratingTextView.text = "★ ${String.format("%.1f", content.malScore)}"
                ratingTextView.visibility = View.VISIBLE
            } else {
                ratingTextView.visibility = View.GONE
            }
            
            // Set synopsis
            synopsisTextView.text = content.synopsis
            
            // Load image with Glide with optimized settings
            if (content.imageUrl.isNotEmpty()) {
                val requestOptions = RequestOptions()
                    .placeholder(R.drawable.gradient_overlay)
                    .error(R.drawable.gradient_overlay)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                
                Glide.with(context)
                    .load(content.imageUrl)
                    .apply(requestOptions)
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .into(coverImageView)
            } else {
                // Set placeholder if no image URL available
                coverImageView.setImageResource(R.drawable.gradient_overlay)
            }
            
            // Set content type indicator based on type
            val typeIndicator = when (content.type) {
                com.animerec.app.models.ContentType.ANIME -> R.drawable.gradient_overlay // Replace with specific indicators
                com.animerec.app.models.ContentType.MANGA -> R.drawable.gradient_overlay
                com.animerec.app.models.ContentType.NOVEL -> R.drawable.gradient_overlay
            }
        }
    }
    
    /**
     * DiffUtil for efficient updates.
     */
    class AnimeContentDiffCallback : DiffUtil.ItemCallback<AnimeContent>() {
        override fun areItemsTheSame(oldItem: AnimeContent, newItem: AnimeContent): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: AnimeContent, newItem: AnimeContent): Boolean {
            return oldItem == newItem
        }
    }
    
    override fun onViewRecycled(holder: AnimeCardViewHolder) {
        super.onViewRecycled(holder)
        // Clear the image when the view is recycled to prevent memory leaks
        Glide.with(context).clear(holder.itemView.findViewById<ImageView>(R.id.anime_cover_image))
    }
}