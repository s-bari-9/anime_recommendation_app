package com.animerec.app.models

data class AnimeContent(
    val id: Int,
    val title: String,
    val alternativeTitles: Map<String, String> = mapOf(),
    val synopsis: String = "",
    val imageUrl: String = "",
    val type: ContentType = ContentType.ANIME,
    val status: String = "",
    val genres: List<String> = listOf(),
    val rating: Double = 0.0,
    val releaseYear: Int? = null,
    val episodes: Int? = null,
    val chapters: Int? = null,
    val volumes: Int? = null,
    val trailerUrl: String? = null,
    val malScore: Double = 0.0,
    val userScore: Int? = null,
    val airingStatus: String = "",
    val isFavorite: Boolean = false,
    val inWatchlist: Boolean = false,
    val isCompleted: Boolean = false
)

enum class ContentType {
    ANIME,
    MANGA,
    NOVEL
}