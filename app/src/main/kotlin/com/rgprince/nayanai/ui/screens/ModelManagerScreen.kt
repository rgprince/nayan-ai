package com.rgprince.nayanai.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rgprince.nayanai.data.ChatRepository
import com.rgprince.nayanai.model.ConversionState
import com.rgprince.nayanai.model.ModelConverter
import com.rgprince.nayanai.ui.components.GlassCard
import com.rgprince.nayanai.ui.components.LiquidButton
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerScreen(
    onNavigateToChat: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val converter = remember { ModelConverter(context) }
    val repository = remember { ChatRepository(context) }
    
    var conversionState by remember { mutableStateOf<ConversionState>(ConversionState.Idle) }
    var selectedModelPath by remember { mutableStateOf<String?>(null) }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = getRealPathFromUri(context, it)
            if (file != null) {
                selectedModelPath = file
                scope.launch {
                    converter.convertModel(file).collect { state ->
                        conversionState = state
                        if (state is ConversionState.Success) {
                            // Create a new chat session
                            val sessionId = repository.createChatSession("Nayan GPT-2")
                            onNavigateToChat(sessionId)
                        }
                    }
                }
            } else {
                // File selection failed
                conversionState = ConversionState.Error("Failed to access file. Please try again or check file permissions.")
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model Manager") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch("*/*") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import Model")
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val state = conversionState) {
                is ConversionState.Idle -> {
                    EmptyModelState(
                        onImportClick = {  filePickerLauncher.launch("*/*") }
                    )
                }
                is ConversionState.Converting -> {
                    ConversionProgress(message = state.progress)
                }
                is ConversionState.Success -> {
                    ConversionSuccess(
                        modelPath = state.outputPath,
                        sizeMb = state.sizeMb
                    )
                }
                is ConversionState.Error -> {
                    ConversionError(
                        message = state.message,
                        onRetry = { conversionState = ConversionState.Idle }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyModelState(onImportClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Model Imported",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Import a PyTorch (.pt) model to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            LiquidButton(
                text = "Import Model",
                onClick = onImportClick
            )
        }
    }
}

@Composable
fun ConversionProgress(message: String) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Converting Model",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ConversionSuccess(modelPath: String, sizeMb: Double) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Model Ready!",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Size: %.2f MB".format(sizeMb),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = modelPath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ConversionError(message: String, onRetry: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Conversion Failed",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(24.dp))
            LiquidButton(
                text = "Try Again",
                onClick = onRetry
            )
        }
    }
}

private fun getRealPathFromUri(context: android.content.Context, uri: Uri): String? {
    return try {
        android.util.Log.d("ModelManager", "Attempting to access file from URI: $uri")
        val input = context.contentResolver.openInputStream(uri)
        if (input == null) {
            android.util.Log.e("ModelManager", "Failed to open input stream for URI: $uri")
            return null
        }
        
        val file = File(context.cacheDir, "temp_model.pt")
        android.util.Log.d("ModelManager", "Copying file to: ${file.absolutePath}")
        
        input.use { inputStream ->
            file.outputStream().use { outputStream ->
                val bytesCopied = inputStream.copyTo(outputStream)
                android.util.Log.d("ModelManager", "Copied $bytesCopied bytes")
            }
        }
        
        if (file.exists()) {
            android.util.Log.d("ModelManager", "File successfully copied. Size: ${file.length()} bytes")
            file.absolutePath
        } else {
            android.util.Log.e("ModelManager", "File not found after copy")
            null
        }
    } catch (e: Exception) {
        android.util.Log.e("ModelManager", "Error accessing file", e)
        e.printStackTrace()
        null
    }
}
