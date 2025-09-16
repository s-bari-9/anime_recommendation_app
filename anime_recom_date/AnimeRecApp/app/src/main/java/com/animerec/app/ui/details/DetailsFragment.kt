package com.animerec.app.ui.details

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.animerec.app.R
import com.animerec.app.data.Resource
import com.animerec.app.models.AnimeContent
import com.animerec.app.models.ContentType
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Fragment for displaying content details.
 */
class DetailsFragment : Fragment() {
    
    private val TAG = "DetailsFragment"
    private lateinit var viewModel: DetailsViewModel
    
    // UI components
    private lateinit var coverImageView: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var typeAndStatusTextView: TextView
    private lateinit var ratingTextView: TextView
    private lateinit var synopsisTextView: TextView
    private lateinit var genresChipGroup: ChipGroup
    private lateinit var watchStatusButton: Button
    private lateinit var watchTrailerButton: Button
    private lateinit var userRatingBar: RatingBar
    private lateinit var similarContentRecyclerView: RecyclerView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var errorTextView: TextView
    
    private lateinit var similarContentAdapter: SimilarContentAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[DetailsViewModel::class.java]
        
        // Get content ID and type from arguments
        arguments?.getInt("contentId")?.let { contentId ->
            val contentTypeStr = arguments?.getString("contentType") ?: ContentType.ANIME.name
            val contentType = ContentType.valueOf(contentTypeStr)
            viewModel.loadContentDetails(contentId, contentType)
            viewModel.loadSimilarContent(contentId)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_details, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI components
        coverImageView = view.findViewById(R.id.coverImageView)
        titleTextView = view.findViewById(R.id.titleTextView)
        typeAndStatusTextView = view.findViewById(R.id.typeAndStatusTextView)
        ratingTextView = view.findViewById(R.id.ratingTextView)
        synopsisTextView = view.findViewById(R.id.synopsisTextView)
        genresChipGroup = view.findViewById(R.id.genresChipGroup)
        watchStatusButton = view.findViewById(R.id.watchStatusButton)
        watchTrailerButton = view.findViewById(R.id.watchTrailerButton)
        userRatingBar = view.findViewById(R.id.userRatingBar)
        similarContentRecyclerView = view.findViewById(R.id.similarContentRecyclerView)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        errorTextView = view.findViewById(R.id.errorTextView)
        
        // Set up similar content recycler view
        similarContentAdapter = SimilarContentAdapter(requireContext()) { similarContent ->
            val bundle = Bundle().apply {
                putInt("contentId", similarContent.id)
                putString("contentType", similarContent.type.name)
            }
            
            // Replace the current fragment with a new instance with the new content ID
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, DetailsFragment::class.java, bundle)
                .addToBackStack(null)
                .commit()
        }
        
        similarContentRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = similarContentAdapter
        }
        
        // Observe content details
        viewModel.contentDetails.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading(true)
                    errorTextView.visibility = View.GONE
                }
                is Resource.Success -> {
                    showLoading(false)
                    errorTextView.visibility = View.GONE
                    updateUIWithContentDetails(resource.data)
                }
                is Resource.Error -> {
                    showLoading(false)
                    errorTextView.visibility = View.VISIBLE
                    errorTextView.text = resource.message
                    Log.e(TAG, "Error loading content details: ${resource.message}")
                }
            }
        }
        
        // Observe similar content
        viewModel.similarContent.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success && resource.data.isNotEmpty()) {
                similarContentAdapter.submitList(resource.data)
                similarContentRecyclerView.visibility = View.VISIBLE
            } else {
                similarContentRecyclerView.visibility = View.GONE
            }
        }
        
        // Set up user rating bar listener
        userRatingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) {
                val content = (viewModel.contentDetails.value as? Resource.Success)?.data
                if (content != null) {
                    val score = (rating * 2).toInt() // Convert 0-5 to 0-10
                    viewModel.rateContent(content, score)
                    Toast.makeText(context, "Rated ${content.title} $score/10", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateUIWithContentDetails(content: AnimeContent) {
        // Set title
        titleTextView.text = content.title
        
        // Set type and status
        val typeText = when (content.type) {
            ContentType.ANIME -> "Anime"
            ContentType.MANGA -> "Manga"
            ContentType.NOVEL -> "Light Novel"
        }
        typeAndStatusTextView.text = "$typeText • ${formatStatus(content.status)}"
        
        // Set rating
        if (content.malScore > 0) {
            ratingTextView.text = "★ ${String.format("%.1f", content.malScore)}"
            ratingTextView.visibility = View.VISIBLE
        } else {
            ratingTextView.visibility = View.GONE
        }
        
        // Set synopsis
        synopsisTextView.text = content.synopsis
        
        // Set genres
        genresChipGroup.removeAllViews()
        for (genre in content.genres) {
            val chip = Chip(requireContext()).apply {
                text = genre
                isCheckable = false
            }
            genresChipGroup.addView(chip)
        }
        
        // Set user rating
        userRatingBar.rating = content.userScore?.toFloat()?.div(2) ?: 0f
        
        // Load cover image
        if (content.imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(content.imageUrl)
                .centerCrop()
                .into(coverImageView)
        }
        
        // Set up watch status button
        watchStatusButton.text = when {
            content.isCompleted -> "Completed"
            content.inWatchlist -> if (content.type == ContentType.ANIME) "In Plan to Watch" else "In Plan to Read"
            else -> if (content.type == ContentType.ANIME) "Add to Plan to Watch" else "Add to Plan to Read"
        }
        
        watchStatusButton.setOnClickListener {
            val newStatus = when {
                content.isCompleted -> if (content.type == ContentType.ANIME) "watching" else "reading"
                content.inWatchlist -> "completed"
                else -> if (content.type == ContentType.ANIME) "plan_to_watch" else "plan_to_read"
            }
            viewModel.updateStatus(content, newStatus)
        }
        
        // Set up watch trailer button
        if (content.type == ContentType.ANIME && !content.trailerUrl.isNullOrEmpty()) {
            watchTrailerButton.visibility = View.VISIBLE
            watchTrailerButton.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content.trailerUrl))
                startActivity(intent)
            }
        } else {
            watchTrailerButton.visibility = View.GONE
        }
    }
    
    private fun formatStatus(status: String): String {
        return when (status) {
            "plan_to_watch" -> "Plan to Watch"
            "plan_to_read" -> "Plan to Read"
            "watching" -> "Currently Airing"
            "reading" -> "Currently Publishing"
            "completed" -> "Completed"
            "on_hold" -> "On Hold"
            "dropped" -> "Dropped"
            else -> status.capitalize()
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        coverImageView.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        titleTextView.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        typeAndStatusTextView.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        ratingTextView.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        synopsisTextView.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        genresChipGroup.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        watchStatusButton.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        watchTrailerButton.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        userRatingBar.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
    }
}