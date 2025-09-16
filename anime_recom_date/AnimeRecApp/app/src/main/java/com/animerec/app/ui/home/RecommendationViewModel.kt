package com.animerec.app.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.animerec.app.AnimeRecApp
import com.animerec.app.data.Resource
import com.animerec.app.models.AnimeContent
import com.animerec.app.recommendation.RecommendationEngine
import com.animerec.app.recommendation.RecommendationMetrics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel for handling recommendations in the home screen.
 */
class RecommendationViewModel(application: Application) : AndroidViewModel(application) {
    
    private val TAG = "RecommendationViewModel"
    
    private val app = application as AnimeRecApp
    private val repository = app.repository
    private val recommendationEngine = app.recommendationEngine
    
    // Metrics tracker
    private val metrics = RecommendationMetrics()
    
    // Current recommendation cards
    private val _recommendations = MutableLiveData<Resource<List<AnimeContent>>>()
    val recommendations: LiveData<Resource<List<AnimeContent>>> = _recommendations
    
    // Keep track of pagination
    private var isLoading = false
    private var hasMoreData = true
    
    // For background recommendation fetching
    private var prefetchJob: Job? = null
    private val prefetchedRecommendations = mutableListOf<AnimeContent>()
    
    init {
        // Start background prefetching of recommendations
        startPrefetching()
    }
    
    /**
     * Load initial recommendations.
     */
    fun loadRecommendations() {
        if (isLoading || !hasMoreData) return
        
        // If we have prefetched recommendations, use them
        if (prefetchedRecommendations.isNotEmpty()) {
            val recommendations = prefetchedRecommendations.toList()
            prefetchedRecommendations.clear()
            _recommendations.value = Resource.Success(recommendations)
            
            // Start prefetching more in the background
            startPrefetching()
            return
        }
        
        // Otherwise, load from the engine
        isLoading = true
        _recommendations.value = Resource.Loading
        
        viewModelScope.launch {
            try {
                // Get user profile
                val userResource = repository.getUserProfile()
                if (userResource !is Resource.Success) {
                    _recommendations.value = Resource.Error("Failed to load user profile")
                    isLoading = false
                    return@launch
                }
                
                val user = userResource.data
                
                // Get recommendations
                val recommendationsResource = recommendationEngine.getRecommendations(user, 20)
                
                _recommendations.value = recommendationsResource
                hasMoreData = (recommendationsResource is Resource.Success) && 
                             (recommendationsResource.data.isNotEmpty())
            } catch (e: Exception) {
                Log.e(TAG, "Error loading recommendations", e)
                _recommendations.value = Resource.Error("Error loading recommendations: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * Load more recommendations.
     */
    fun loadMoreRecommendations() {
        if (isLoading || !hasMoreData) return
        
        // If we have prefetched recommendations, use them
        if (prefetchedRecommendations.isNotEmpty()) {
            val currentValue = _recommendations.value
            if (currentValue is Resource.Success) {
                val currentRecommendations = currentValue.data.toMutableList()
                val additionalRecommendations = prefetchedRecommendations.toList()
                prefetchedRecommendations.clear()
                
                currentRecommendations.addAll(additionalRecommendations)
                _recommendations.value = Resource.Success(currentRecommendations)
                
                // Start prefetching more in the background
                startPrefetching()
                return
            }
        }
        
        // Otherwise, load from the engine
        isLoading = true
        
        viewModelScope.launch {
            try {
                // Get user profile
                val userResource = repository.getUserProfile()
                if (userResource !is Resource.Success) {
                    _recommendations.value = Resource.Error("Failed to load user profile")
                    isLoading = false
                    return@launch
                }
                
                val user = userResource.data
                
                // Get more recommendations
                val newRecommendationsResource = recommendationEngine.getRecommendations(user, 10)
                
                if (newRecommendationsResource is Resource.Success) {
                    val newRecommendations = newRecommendationsResource.data
                    
                    // Combine with existing recommendations
                    val currentList = (_recommendations.value as? Resource.Success)?.data ?: emptyList()
                    val currentIds = currentList.map { it.id }.toSet()
                    
                    // Filter out duplicates
                    val uniqueNewRecommendations = newRecommendations.filter { it.id !in currentIds }
                    
                    // Combine lists
                    val combinedList = currentList + uniqueNewRecommendations
                    
                    _recommendations.value = Resource.Success(combinedList)
                    hasMoreData = uniqueNewRecommendations.isNotEmpty()
                } else if (newRecommendationsResource is Resource.Error) {
                    // Keep existing recommendations but show an error toast
                    Log.e(TAG, "Error loading more recommendations: ${newRecommendationsResource.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading more recommendations", e)
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * Record a user interaction with a content item.
     */
    fun recordInteraction(content: AnimeContent, interactionType: RecommendationEngine.InteractionType) {
        viewModelScope.launch {
            try {
                // Record in recommendation engine
                recommendationEngine.recordInteraction(content.id, interactionType)
                
                // Record in metrics
                metrics.recordInteraction(
                    interactionType,
                    determineRecommendationSource(content),
                    content.genres
                )
                
                // Log metrics occasionally
                if (metrics.getEngagementRate() > 0 && 
                    (Math.random() < 0.2)) { // 20% chance
                    metrics.logMetricsReport()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error recording interaction", e)
            }
        }
    }
    
    /**
     * Add to watchlist when swiped right.
     */
    fun addToWatchlist(content: AnimeContent) {
        recordInteraction(content, RecommendationEngine.InteractionType.LIKE)
    }
    
    /**
     * Mark as not interested when swiped left.
     */
    fun markAsNotInterested(content: AnimeContent) {
        recordInteraction(content, RecommendationEngine.InteractionType.DISLIKE)
    }
    
    /**
     * Mark as watched/completed when swiped up.
     */
    fun markAsWatched(content: AnimeContent) {
        recordInteraction(content, RecommendationEngine.InteractionType.SUPER_LIKE)
    }
    
    /**
     * Show details when swiped down.
     */
    fun showDetails(content: AnimeContent) {
        recordInteraction(content, RecommendationEngine.InteractionType.VIEW_DETAILS)
    }
    
    /**
     * Clear the recommendation cache.
     */
    fun refreshRecommendations() {
        viewModelScope.launch {
            try {
                recommendationEngine.clearCache()
                loadRecommendations()
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing recommendations", e)
            }
        }
    }
    
    /**
     * Start prefetching recommendations in the background.
     */
    private fun startPrefetching() {
        // Cancel any existing job
        prefetchJob?.cancel()
        
        // Start a new job
        prefetchJob = viewModelScope.launch {
            try {
                // Wait a bit before prefetching to avoid unnecessary API calls
                delay(2000)
                
                if (!isActive) return@launch
                
                // Get user profile
                val userResource = repository.getUserProfile()
                if (userResource !is Resource.Success) {
                    return@launch
                }
                
                val user = userResource.data
                
                // Get recommendations
                val recommendationsResource = recommendationEngine.getRecommendations(user, 10)
                
                if (recommendationsResource is Resource.Success) {
                    val recommendations = recommendationsResource.data
                    
                    // Add to prefetched list, avoiding duplicates
                    val currentIds = (_recommendations.value as? Resource.Success)?.data?.map { it.id } ?: emptyList()
                    val uniqueRecommendations = recommendations.filter { it.id !in currentIds }
                    
                    prefetchedRecommendations.addAll(uniqueRecommendations)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error prefetching recommendations", e)
            }
        }
    }
    
    /**
     * Determine which recommendation source most likely recommended a content item.
     * In a real implementation, this would track the actual source.
     */
    private fun determineRecommendationSource(content: AnimeContent): RecommendationSource {
        // Just using ID as a simple deterministic way to assign a source
        val sources = RecommendationSource.values()
        val randomIndex = (content.id % sources.size + sources.size) % sources.size
        return sources[randomIndex]
    }
    
    override fun onCleared() {
        super.onCleared()
        prefetchJob?.cancel()
    }
}