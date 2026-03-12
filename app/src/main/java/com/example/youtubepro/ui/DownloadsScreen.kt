package com.example.youtubepro.ui

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.youtubepro.models.DownloadedFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var downloadedFiles by remember { mutableStateOf<List<DownloadedFile>>(emptyList()) }

    fun loadFiles() {
        val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "YoutubePro")
        if (downloadDir.exists() && downloadDir.isDirectory) {
            val files = downloadDir.listFiles()?.filter { it.isFile }?.map {
                DownloadedFile(it.name, it.absolutePath, "${it.length() / (1024 * 1024)} MB")
            } ?: emptyList()
            downloadedFiles = files
        }
    }

    LaunchedEffect(Unit) {
        loadFiles()
    }

    fun deleteFile(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
            loadFiles()
            Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
        }
    }

    fun moveToMovies(filePath: String) {
        coroutineScope.launch(Dispatchers.IO) {
            val file = File(filePath)
            if (file.exists()) {
                val destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                if (!destDir.exists()) destDir.mkdirs()
                val destFile = File(destDir, file.name)
                try {
                    file.copyTo(destFile, overwrite = true)
                    file.delete()
                    withContext(Dispatchers.Main) {
                        loadFiles()
                        Toast.makeText(context, "Moved to Movies folder", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to move file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(downloadedFiles) { file ->
                DownloadedFileItem(
                    file = file,
                    onDelete = { deleteFile(file.filePath) },
                    onMove = { moveToMovies(file.filePath) }
                )
            }
        }
    }
}

@Composable
fun DownloadedFileItem(file: DownloadedFile, onDelete: () -> Unit, onMove: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(file.fileName, style = MaterialTheme.typography.titleMedium)
            Text(file.size, style = MaterialTheme.typography.bodySmall)
        }
        
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Save to Device (Movies)") },
                    onClick = {
                        expanded = false
                        onMove()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        expanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}
