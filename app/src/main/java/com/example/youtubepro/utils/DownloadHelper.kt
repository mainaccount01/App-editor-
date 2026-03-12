package com.example.youtubepro.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

class DownloadHelper(private val context: Context) {
    fun startDownload(directUrl: String, title: String, isAudio: Boolean) {
        val extension = if (isAudio) ".mp3" else ".mp4"
        val cleanTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val fileName = "$cleanTitle$extension"

        val request = DownloadManager.Request(Uri.parse(directUrl))
            .setTitle(cleanTitle)
            .setDescription("Downloading...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "YoutubePro/$fileName")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }
}
