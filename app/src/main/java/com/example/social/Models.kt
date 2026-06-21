package com.example.social

import kotlinx.serialization.Serializable

@Serializable
data class VideoResponse(
    val videos: List<VideoItem> = emptyList()
)

@Serializable
data class VideoItem(
    val filename: String,
    val url: String
)

@Serializable
data class UploadResponse(
    val message: String,
    val video: VideoItem
)
