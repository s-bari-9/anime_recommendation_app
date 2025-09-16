package com.animerec.app.recommendation

/**
 * Sources for recommendations to track effectiveness of different algorithms.
 */
enum class RecommendationSource {
    CONTENT_BASED,     // Based on content similarity
    COLLABORATIVE,     // Based on user similarity
    THEME_BASED,       // Based on themes and tropes
    POPULARITY,        // Based on overall popularity
    SEASONAL,          // Currently airing/publishing
    USER_HISTORY,      // Based on user's watch history
    MAL_SUGGESTIONS    // Directly from MAL API suggestions
}