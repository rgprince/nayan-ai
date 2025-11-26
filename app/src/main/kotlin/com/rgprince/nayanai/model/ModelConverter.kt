package com.rgprince.nayanai.model

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

sealed class ConversionState {
    object Idle : ConversionState()
    data class Converting(val progress: String) : ConversionState()
    data class Success(val outputPath: String, val sizeMb: Double) : ConversionState()
    data class Error(val message: String) : ConversionState()
}

class ModelConverter(private val context: Context) {
    
    init {
        if (!Python.isStarted()) {
            AndroidPlatform.start(context)
        }
    }
    
    fun convertModel(inputPath: String): Flow<ConversionState> = flow {
        emit(ConversionState.Converting("Initializing Python environment..."))
        
        try {
            val python = Python.getInstance()
            val converter = python.getModule("converter")
            
            emit(ConversionState.Converting("Loading PyTorch model..."))
            
            // Generate output path
            val inputFile = File(inputPath)
            val outputPath = inputFile.parent + "/" + inputFile.nameWithoutExtension + ".onnx"
            
            emit(ConversionState.Converting("Converting to ONNX format..."))
            
            // Call Python conversion function
            val result = converter.callAttr("convert_to_onnx", inputPath, outputPath)
            
            val success = result["success"]?.toBoolean() ?: false
            val message = result["message"]?.toString() ?: "Unknown error"
            
            if (success) {
                val sizeMb = result["model_size_mb"]?.toDouble() ?: 0.0
                emit(ConversionState.Success(outputPath, sizeMb))
            } else {
                emit(ConversionState.Error(message))
            }
            
        } catch (e: Exception) {
            emit(ConversionState.Error("Conversion failed: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    suspend fun getModelInfo(onnxPath: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        try {
            val python = Python.getInstance()
            val converter = python.getModule("converter")
            val result = converter.callAttr("get_model_info", onnxPath)
            
            val success = result["success"]?.toBoolean() ?: false
            if (success) {
                mapOf(
                    "size_mb" to (result["size_mb"]?.toDouble() ?: 0.0),
                    "path" to (result["path"]?.toString() ?: "")
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
