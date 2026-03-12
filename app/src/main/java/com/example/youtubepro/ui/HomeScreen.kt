package com.example.youtubepro.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.youtubepro.models.VideoModel
import com.example.youtubepro.utils.DownloadHelper
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    var isUrlMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var videoList by remember { mutableStateOf<List<VideoModel>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedVideo by remember { mutableStateOf<VideoModel?>(null) }
    var selectedUrl by remember { mutableStateOf<String?>(null) }
    var isPreparingDownload by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val downloadHelper = remember { DownloadHelper(context) }

    fun fetchSearchResults(query: String) {
        if (query.isBlank()) return
        isLoading = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val request = YoutubeDLRequest("ytsearch10:$query")
                val info = YoutubeDL.getInstance().getInfo(request)
                val results = info.entries?.map {
                    VideoModel(
                        id = it.id ?: "",
                        title = it.title ?: "Unknown Title",
                        thumbnailUrl = it.thumbnail ?: "",
                        duration = "${(it.duration / 60)}:${String.format("%02d", it.duration % 60)}",
                        url = it.webpageUrl ?: ""
                    )
                } ?: emptyList()
                withContext(Dispatchers.Main) {
                    videoList = results
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(context, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun prepareAndDownload(videoUrl: String, isAudio: Boolean) {
        isPreparingDownload = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val request = YoutubeDLRequest(videoUrl)
                if (isAudio) {
                    request.addOption("-f", "bestaudio")
                } else {
                    request.addOption("-f", "best")
                }
                val info = YoutubeDL.getInstance().getInfo(request)
                val directUrl = info.url ?: throw Exception("Direct URL not found")
                val title = info.title ?: "Video"
                
                withContext(Dispatchers.Main) {
                    downloadHelper.startDownload(directUrl, title, isAudio)
                    isPreparingDownload = false
                    Toast.makeText(context, "Download Started", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isPreparingDownload = false
                    Toast.makeText(context, "Failed to get download link", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Youtube Pro") },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isUrlMode) "URL" else "Search", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = isUrlMode,
                            onCheckedChange = { isUrlMode = it },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { navController.navigate("downloads") }) {
                            Icon(Icons.Default.Folder, contentDescription = "Downloads")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (isUrlMode) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text("Paste YouTube URL here...") },
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (urlInput.isNotBlank()) {
                            selectedUrl = urlInput
                            selectedVideo = VideoModel("", "URL Download", "", "", urlInput)
                            showDialog = true
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
                ) {
                    Text("Download")
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search YouTube...") },
                        singleLine = true
                    )
                    IconButton(onClick = { fetchSearchResults(searchQuery) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }

                if (isLoading || isPreparingDownload) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
                }

                LazyColumn {
                    items(videoList) { video ->
                        VideoItem(video, onDownloadClick = { 
                            selectedVideo = video
                            selectedUrl = video.url
                            showDialog = true
                        })
                    }
                }
            }
        }
    }

    if (showDialog && selectedUrl != null) {
        DownloadOptionsDialog(
            onDismiss = { showDialog = false },
            onConfirmDownload = { isAudio ->
                prepareAndDownload(selectedUrl!!, isAudio)
                showDialog = false
            }
        )
    }
}

@Composable
fun VideoItem(video: VideoModel, onDownloadClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = "Thumbnail",
            modifier = Modifier.size(120.dp, 80.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(video.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            Text(video.duration, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onDownloadClick) {
            Icon(Icons.Default.Download, contentDescription = "Download")
        }
    }
}

@Composable
fun DownloadOptionsDialog(
    onDismiss: () -> Unit,
    onConfirmDownload: (Boolean) -> Unit
) {
    var isAudioSelected by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download Options") },
        text = {
            Column {
                Text("Select Format:")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !isAudioSelected, onClick = { isAudioSelected = false })
                    Text("Video (Best Quality)")
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isAudioSelected, onClick = { isAudioSelected = true })
                    Text("Audio (MP3)")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirmDownload(isAudioSelected) }) {
                Text("Start Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
