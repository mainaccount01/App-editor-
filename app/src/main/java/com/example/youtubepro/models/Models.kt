package com.example.youtubepro.models

data class VideoModel(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val duration: String,
    val url: String
)

data class DownloadedFile(
    val fileName: String,
    val filePath: String,
    val size: String
)
