package com.animerec.app

import android.app.Application
import android.content.ComponentCallbacks2
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.animerec.app.auth.AuthManager
import com.animerec.app.data.AnimeRepository
import com.animerec.app.di.ServiceLocator
import com.animerec.app.data.AnimeRepositoryImpl
import com.animerec.app.recommendation.RecommendationEngine
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AnimeRecApp : Application(), ComponentCallbacks2 {

    private val TAG = "AnimeRecApp"
    private val dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    
    // Application scope for coroutines that should survive configuration changes
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Lazy initialization of components
    val repository: AnimeRepository by lazy {
        ServiceLocator.provideAnimeRepository(applicationContext)
    }
    
    val authManager: AuthManager by lazy {
        ServiceLocator.provideAuthManager(applicationContext)
    }
    
    val recommendationEngine: RecommendationEngine by lazy {
        ServiceLocator.provideRecommendationEngine(applicationContext)
    }
    
    companion object {
        // For MyAnimeList OAuth - Register at https://myanimelist.net/apiconfig
        const val CLIENT_ID = "c65a18864c7e9d6ef7a8f5c7eb6f5a06"
        
        // OAuth endpoints
        const val REDIRECT_URI = "animerec://auth"
        const val MAL_AUTH_URL = "https://myanimelist.net/v1/oauth2/authorize"
        const val MAL_TOKEN_URL = "https://myanimelist.net/v1/oauth2/token"
        
        // API base URL
        const val MAL_API_BASE_URL = "https://api.myanimelist.net/v2/"
        
        // Status codes for anime/manga lists
        const val STATUS_WATCHING = "watching"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_ON_HOLD = "on_hold"
        const val STATUS_DROPPED = "dropped"
        const val STATUS_PLAN_TO_WATCH = "plan_to_watch"
        
        const val STATUS_READING = "reading"
        const val STATUS_PLAN_TO_READ = "plan_to_read"
        
        // API field groups to request together
        const val ANIME_FIELDS = "id,title,main_picture,alternative_titles,synopsis,mean,rank,popularity,num_list_users,media_type,status,genres,my_list_status,num_episodes,start_season,broadcast,source,average_episode_duration,rating,pictures,background,related_anime,related_manga,recommendations,studios,statistics"
        const val MANGA_FIELDS = "id,title,main_picture,alternative_titles,synopsis,mean,rank,popularity,num_list_users,media_type,status,genres,my_list_status,num_volumes,num_chapters,authors{first_name,last_name},pictures,background,related_anime,related_manga,recommendations"
        
        // Database name
        const val DATABASE_NAME = "anime_rec_database"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application initializing")
        
        // Initialize Glide with optimized settings
        Glide.init(this, GlideBuilder().apply {
            setDefaultRequestOptions(
                RequestOptions()
                    .format(DecodeFormat.PREFER_RGB_565) // Uses less memory
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .skipMemoryCache(false)
            )
        })
        
        // Initialize other components as needed
        applicationScope.launch {
            // Perform app initialization tasks that don't need to happen immediately
            preloadEssentialData()
        }
    }
    
    private suspend fun preloadEssentialData() {
        // Preload essential data
        try {
            // Check if we have a valid authentication
            if (authManager.isAuthenticated()) {
                // Preload user profile
                repository.getUserProfile()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading essential data", e)
        }
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                // App is in the foreground but system is low on memory
                Glide.get(this).clearMemory()
            }
            
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                // App's UI is hidden, reduce memory usage
                Glide.get(this).trimMemory(level)
            }
            
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                // App is in the background, clear all caches
                Glide.get(this).clearMemory()
                // Clear API response cache
                (repository as? AnimeRepositoryImpl)?.clearCache()
            }
        }
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        // Clear all memory caches
        Glide.get(this).clearMemory()
        (repository as? AnimeRepositoryImpl)?.clearCache()
    }
}