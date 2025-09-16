# Recommendation Engine Documentation

## Overview

The recommendation engine in this application employs a hybrid approach that combines multiple recommendation techniques to provide personalized anime, manga, and light novel recommendations. The engine is designed to learn from user interactions (swipes) and continuously improve its recommendations over time.

## Core Components

### 1. Enhanced Recommendation Engine

The `EnhancedRecommendationEngine` is the main orchestrator that combines multiple recommendation strategies:

- **Content-based filtering**: Recommends items similar to what the user has liked in the past
- **Collaborative filtering**: Recommends items liked by users with similar tastes
- **Theme-based recommendations**: Analyzes content themes, emotional tones, and tropes
- **Popularity-based recommendations**: Provides reliable fallback recommendations

The engine dynamically adjusts the weight of each strategy based on data availability and user feedback, ensuring optimal recommendations even with limited data.

### 2. Personalized Weight Manager

The `PersonalizedWeightManager` learns which recommendation sources are most effective for each user by tracking:

- Which recommendation source generated each recommendation
- How users interact with recommendations from each source
- Adjusting weights over time to favor sources that generate more positive interactions

This creates a feedback loop where the system continuously improves and becomes more tailored to each user's preferences.

### 3. User Preference Model

The `UserPreferenceModel` maintains a detailed profile of user preferences including:

- Genre preferences (learned from user interactions)
- Release period preferences (modern vs. classic)
- Content status preferences (ongoing vs. completed)
- Not-interested genres (avoiding repetitive unwanted recommendations)

These preferences are used to adjust the final ranking of recommendations.

### 4. Theme-Based Recommender

The `ThemeBasedRecommender` goes beyond simple genre matching by analyzing:

- **Theme keywords**: Identifying narrative themes like "revenge", "coming of age", etc.
- **Emotional tones**: Detecting if content is uplifting, dark, funny, intense, etc.
- **Settings**: Identifying worldbuilding elements like school, medieval, futuristic, etc.
- **Tropes**: Recognizing common storytelling patterns like "chosen one", "power fantasy", etc.

This semantic analysis enables recommendations based on deeper content similarities rather than just metadata.

## Recommendation Process

1. **Data Collection**:
   - User's watch/read history from MyAnimeList
   - User's explicit favorites
   - Genre preferences
   - Past interactions (swipes)

2. **Candidate Generation**:
   - Generate candidates from multiple sources (genre recommendations, popular items, seasonal items)
   - Filter out already seen content and "not interested" items

3. **Multi-strategy Scoring**:
   - Run multiple recommendation algorithms in parallel
   - Each algorithm produces a scored list of recommendations

4. **Weighted Merging**:
   - Combine recommendations using personalized weights for each strategy
   - Track which strategy contributed to each recommendation for feedback learning

5. **Personalization Adjustment**:
   - Apply user preference model to boost items matching learned preferences
   - Apply recency, completion status, and other adjustments

6. **Caching**:
   - Cache recommendations for performance
   - Invalidate cache when preferences change

## User Interaction Learning

User interactions with the recommendation cards provide crucial feedback:

- **Swipe Right (Like)**: Adds to watchlist and boosts similar content
- **Swipe Left (Dislike)**: Marks as not interested and reduces similar content
- **Swipe Up (Super Like)**: Marks as already watched and significantly boosts similar content
- **Swipe Down (View Details)**: Shows interest but provides neutral feedback

Each interaction updates both the user preference model and the personalized weight manager, making the system progressively more accurate.

## Implementation Details

### Extensible Architecture

The engine is designed with extensibility in mind:

- Strategy interfaces allow easy addition of new recommendation approaches
- All weight parameters can be adjusted or learned through user interaction
- Cache system prevents redundant API calls and calculations

### Cold Start Handling

For new users with limited data, the system:

- Places higher weight on popularity-based recommendations
- Generates synthetic user data for collaborative filtering
- Relies more heavily on explicit genre preferences

### Performance Considerations

- Parallel processing of recommendation strategies
- In-memory caching with configurable expiration
- Efficient filtering and merging algorithms
- Recommendation batch size management for pagination

## Future Enhancements

Potential areas for improvement:

1. **Advanced NLP**: Incorporate deeper natural language processing for synopsis analysis
2. **Neural Network Models**: Add deep learning models for content embedding
3. **Time-aware Recommendations**: Factor in seasonal preferences and trends
4. **Social Recommendations**: Incorporate recommendations from friends/followed users
5. **Contextual Awareness**: Consider time of day, day of week in recommendations
6. **Diversity Injection**: Ensure recommendation diversity to prevent echo chambers