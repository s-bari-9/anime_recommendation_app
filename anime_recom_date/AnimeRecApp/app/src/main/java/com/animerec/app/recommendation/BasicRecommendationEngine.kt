package com.animerec.app.recommendation

import android.util.Log
import com.animerec.app.data.AnimeRepository
import com.animerec.app.data.Resource
import com.animerec.app.models.AnimeContent
import com.animerec.app.models.ContentType
import com.animerec.app.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * Basic implementation of the recommendation engine.
 * Uses a combination of genre-based filtering and user preferences.
 */
class BasicRecommendationEngine(
    private val repository: AnimeRepository,
    private val userPreferenceModel: UserPreferenceModel
) : RecommendationEngine {
    
    private val TAG = "BasicRecommendationEngine"
    
    // Cache for recommendations to avoid repeated API calls
    private val recommendationCache = mutableMapOf<String, Pair<List<AnimeContent>, Long>>()
    
    // Cache expiration time in milliseconds (30 minutes)
    private val CACHE_EXPIRATION = 30 * 60 * 1000L
    
    override suspend fun getRecommendations(
        user: User,
        limit: Int
    ): Resource<List<AnimeContent>> = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            val cacheKey = "recommendations_${user.name}_$limit"
            val cachedRecommendations = getFromCache(cacheKey)
            if (cachedRecommendations != null) {
                return@withContext Resource.Success(cachedRecommendations)
            }
            
            // Get all content types the user is interested in
            val contentTypes = user.contentPreferences
            if (contentTypes.isEmpty()) {
                return@withContext Resource.Error("No content types selected")
            }
            
            // Get recommendations for each content type
            val allRecommendations = mutableListOf<AnimeContent>()
            
            // Determine how many recommendations to get for each type
            val itemsPerType = limit / contentTypes.size
            
            // Get recommendations for each content type
            for (contentType in contentTypes) {
                val recommendationsForType = getRecommendationsForType(user, contentType, itemsPerType)
                if (recommendationsForType is Resource.Success) {
                    allRecommendations.addAll(recommendationsForType.data)
                }
            }
            
            // Sort and limit recommendations
            val filteredRecommendations = filterAndSortRecommendations(allRecommendations, user, limit)
            
            // Cache the results
            addToCache(cacheKey, filteredRecommendations)
            
            return@withContext Resource.Success(filteredRecommendations)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recommendations", e)
            return@withContext Resource.Error("Error getting recommendations: ${e.message}")
        }
    }
    
    override suspend fun getRecommendationsForType(
        user: User,
        contentType: String,
        limit: Int
    ): Resource<List<AnimeContent>> = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            val cacheKey = "recommendations_${user.name}_${contentType}_$limit"
            val cachedRecommendations = getFromCache(cacheKey)
            if (cachedRecommendations != null) {
                return@withContext Resource.Success(cachedRecommendations)
            }
            
            // Get user preferences
            val genres = user.genrePreferences
            
            // Get recommendations based on content type
            val recommendationsResource = when (contentType) {
                "anime" -> repository.getAnimeRecommendations(genres, limit * 2)
                "manga" -> repository.getMangaRecommendations(genres, limit * 2)
                "novels" -> repository.getNovelRecommendations(genres, limit * 2)
                else -> Resource.Error("Invalid content type: $contentType")
            }
            
            // Process recommendations
            if (recommendationsResource is Resource.Success) {
                val recommendations = recommendationsResource.data
                
                // Filter out not interested items
                val notInterestedResource = repository.getNotInterestedIds()
                val notInterestedIds = if (notInterestedResource is Resource.Success) {
                    notInterestedResource.data
                } else {
                    emptyList()
                }
                
                val filteredRecommendations = recommendations.filter { it.id !in notInterestedIds }
                
                // Sort recommendations based on user preferences
                val sortedRecommendations = userPreferenceModel.rankContentForUser(
                    filteredRecommendations,
                    user
                )
                
                // Limit to requested number
                val limitedRecommendations = sortedRecommendations.take(limit)
                
                // Cache the results
                addToCache(cacheKey, limitedRecommendations)
                
                return@withContext Resource.Success(limitedRecommendations)
            } else if (recommendationsResource is Resource.Error) {
                return@withContext Resource.Error(recommendationsResource.message)
            } else {
                return@withContext Resource.Error("Unknown error getting recommendations")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recommendations for type $contentType", e)
            return@withContext Resource.Error("Error getting recommendations: ${e.message}")
        }
    }
    
    override suspend fun getSimilarContent(
        contentId: Int,
        limit: Int
    ): Resource<List<AnimeContent>> = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            val cacheKey = "similar_${contentId}_$limit"
            val cachedRecommendations = getFromCache(cacheKey)
            if (cachedRecommendations != null) {
                return@withContext Resource.Success(cachedRecommendations)
            }
            
            // First, get the content details to determine its type and genres
            val contentResource = repository.getAnimeDetails(contentId)
            
            if (contentResource is Resource.Success) {
                val content = contentResource.data
                
                // Get similar content based on content type
                val similarResource = when (content.type) {
                    ContentType.ANIME -> repository.getAnimeRecommendations(content.genres, limit * 2)
                    ContentType.MANGA -> repository.getMangaRecommendations(content.genres, limit * 2)
                    ContentType.NOVEL -> repository.getNovelRecommendations(content.genres, limit * 2)
                }
                
                if (similarResource is Resource.Success) {
                    // Filter out the original content and sort by similarity
                    val filteredContent = similarResource.data
                        .filter { it.id != contentId }
                        .sortedByDescending { calculateSimilarity(content, it) }
                        .take(limit)
                    
                    // Cache the results
                    addToCache(cacheKey, filteredContent)
                    
                    return@withContext Resource.Success(filteredContent)
                } else if (similarResource is Resource.Error) {
                    return@withContext Resource.Error(similarResource.message)
                }
            } else if (contentResource is Resource.Error) {
                return@withContext Resource.Error(contentResource.message)
            }
            
            return@withContext Resource.Error("Failed to get similar content")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting similar content for ID $contentId", e)
            return@withContext Resource.Error("Error getting similar content: ${e.message}")
        }
    }
    
    override suspend fun recordInteraction(
        contentId: Int,
        interactionType: RecommendationEngine.InteractionType
    ): Resource<Boolean> = withContext(Dispatchers.IO) {
        try {
            // First, we need to get the content to know its type
            val contentResource = repository.getAnimeDetails(contentId)
            
            if (contentResource !is Resource.Success) {
                return@withContext Resource.Error("Failed to get content details")
            }
            
            val content = contentResource.data
            
            // Handle different interaction types
            when (interactionType) {
                RecommendationEngine.InteractionType.LIKE -> {
                    // Add to watchlist
                    val status = when (content.type) {
                        ContentType.ANIME -> "plan_to_watch"
                        ContentType.MANGA, ContentType.NOVEL -> "plan_to_read"
                    }
                    
                    val result = when (content.type) {
                        ContentType.ANIME -> repository.updateAnimeStatus(contentId, status)
                        ContentType.MANGA, ContentType.NOVEL -> repository.updateMangaStatus(contentId, status)
                    }
                    
                    // Update user preference model with this interaction
                    userPreferenceModel.updatePreferencesFromInteraction(content, true)
                    
                    return@withContext result
                }
                RecommendationEngine.InteractionType.DISLIKE -> {
                    // Mark as not interested
                    val result = repository.markAsNotInterested(contentId)
                    
                    // Update user preference model with this interaction
                    userPreferenceModel.updatePreferencesFromInteraction(content, false)
                    
                    return@withContext result
                }
                RecommendationEngine.InteractionType.SUPER_LIKE -> {
                    // Mark as completed
                    val status = "completed"
                    
                    val result = when (content.type) {
                        ContentType.ANIME -> repository.updateAnimeStatus(contentId, status)
                        ContentType.MANGA, ContentType.NOVEL -> repository.updateMangaStatus(contentId, status)
                    }
                    
                    // Update user preference model with this interaction (stronger positive)
                    userPreferenceModel.updatePreferencesFromInteraction(content, true, weight = 2.0)
                    
                    return@withContext result
                }
                RecommendationEngine.InteractionType.VIEW_DETAILS -> {
                    // No API update needed, just record the interaction locally
                    userPreferenceModel.updatePreferencesFromInteraction(content, true, weight = 0.5)
                    return@withContext Resource.Success(true)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recording interaction for ID $contentId", e)
            return@withContext Resource.Error("Error recording interaction: ${e.message}")
        }
    }
    
    override suspend fun clearCache(): Resource<Boolean> = withContext(Dispatchers.IO) {
        try {
            recommendationCache.clear()
            return@withContext Resource.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
            return@withContext Resource.Error("Error clearing cache: ${e.message}")
        }
    }
    
    /**
     * Calculate similarity score between two content items.
     * Higher scores indicate more similarity.
     */
    private fun calculateSimilarity(content1: AnimeContent, content2: AnimeContent): Double {
        var score = 0.0
        
        // Genre similarity (most important)
        val genreOverlap = content1.genres.intersect(content2.genres.toSet()).size
        score += genreOverlap * 10.0
        
        // Rating similarity
        if (content1.rating > 0 && content2.rating > 0) {
            val ratingDiff = Math.abs(content1.rating - content2.rating)
            score += (10.0 - ratingDiff) * 2.0
        }
        
        // Same type
        if (content1.type == content2.type) {
            score += 5.0
        }
        
        // Same status (airing, completed, etc.)
        if (content1.status == content2.status) {
            score += 3.0
        }
        
        return score
    }
    
    /**
     * Filter and sort recommendations based on user preferences.
     */
    private fun filterAndSortRecommendations(
        recommendations: List<AnimeContent>,
        user: User,
        limit: Int
    ): List<AnimeContent> {
        // Filter out duplicates
        val uniqueRecommendations = recommendations.distinctBy { it.id }
        
        // Sort by user preferences
        val sortedRecommendations = userPreferenceModel.rankContentForUser(
            uniqueRecommendations,
            user
        )
        
        // Limit to requested number
        return sortedRecommendations.take(min(limit, sortedRecommendations.size))
    }
    
    /**
     * Get recommendations from cache if available and not expired.
     */
    private fun getFromCache(key: String): List<AnimeContent>? {
        val cachedValue = recommendationCache[key]
        if (cachedValue != null) {
            val (recommendations, timestamp) = cachedValue
            val currentTime = System.currentTimeMillis()
            
            // Check if cache is still valid
            if (currentTime - timestamp < CACHE_EXPIRATION) {
                return recommendations
            } else {
                // Remove expired cache entry
                recommendationCache.remove(key)
            }
        }
        return null
    }
    
    /**
     * Add recommendations to cache.
     */
    private fun addToCache(key: String, recommendations: List<AnimeContent>) {
        recommendationCache[key] = Pair(recommendations, System.currentTimeMillis())
    }
}