package com.rgprince.nayanai.model

import ai.onnxruntime.*
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.LongBuffer
import kotlin.random.Random

class OnnxInference(private val context: Context) {
    private var session: OrtSession? = null
    private val tokenizer = BPETokenizer()
    private val env = OrtEnvironment.getEnvironment()
    
    suspend fun loadModel(modelPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val sessionOptions = OrtSession.SessionOptions()
            sessionOptions.setInterOpNumThreads(2)
            sessionOptions.setIntraOpNumThreads(4)
            
            session = env.createSession(modelPath, sessionOptions)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun generate(
        prompt: String,
        maxTokens: Int = 100,
        temperature: Float = 0.8f,
        topK: Int = 40
    ): String = withContext(Dispatchers.IO) {
        val currentSession = session ?: return@withContext "Error: Model not loaded"
        
        try {
            // Encode prompt
            val inputTokens = tokenizer.encode(prompt).toMutableList()
            val generatedText = StringBuilder(prompt)
            
            // Generate tokens
            repeat(maxTokens) {
                // Take last BLOCK_SIZE tokens if sequence is too long
                val contextTokens = if (inputTokens.size > BPETokenizer.BLOCK_SIZE) {
                    inputTokens.takeLast(BPETokenizer.BLOCK_SIZE).toLongArray()
                } else {
                    inputTokens.toLongArray()
                }
                
                // Create input tensor
                val shape = longArrayOf(1, contextTokens.size.toLong())
                val inputBuffer = LongBuffer.wrap(contextTokens)
                val inputTensor = OnnxTensor.createTensor(env, inputBuffer, shape)
                
                // Run inference
                val inputs = mapOf("input_ids" to inputTensor)
                val outputs = currentSession.run(inputs)
                
                // Get logits
                val output = outputs[0] as OnnxTensor
                val logits = output.floatBuffer.array()
                
                // The output shape is [batch_size, sequence_length, vocab_size]
                // We want the logits for the last token
                val vocabSize = BPETokenizer.VOCAB_SIZE
                val lastTokenLogits = logits.sliceArray(
                    (logits.size - vocabSize) until logits.size
                )
                
                // Apply temperature
                val scaledLogits = lastTokenLogits.map { it / temperature }
                
                // Sample next token
                val nextToken = if (topK > 0) {
                    sampleTopK(scaledLogits.toFloatArray(), topK)
                } else {
                    greedySample(scaledLogits.toFloatArray())
                }
                
                // Add to sequence
                inputTokens.add(nextToken)
                
                // Decode and append
                val nextChar = tokenizer.decodeToken(nextToken)
                generatedText.append(nextChar)
                
                // Cleanup
                inputTensor.close()
                outputs.close()
                
                // Stop if we generate a natural end (simplified)
                if (nextChar == "." || nextChar == "!" || nextChar == "?") {
                    if (Random.nextFloat() < 0.3f) break
                }
            }
            
            generatedText.toString()
            
        } catch (e: Exception) {
            e.printStackTrace()
            "Error during generation: ${e.message}"
        }
    }
    
    private fun greedySample(logits: FloatArray): Long {
        return logits.indices.maxByOrNull { logits[it] }?.toLong() ?: 0L
    }
    
    private fun sampleTopK(logits: FloatArray, k: Int): Long {
        // Get top-k indices
        val indexedLogits = logits.mapIndexed { index, value -> index to value }
        val topK = indexedLogits.sortedByDescending { it.second }.take(k)
        
        // Apply softmax to top-k
        val maxLogit = topK.maxOf { it.second }
        val expLogits = topK.map { (idx, logit) ->
            idx to Math.exp((logit - maxLogit).toDouble())
        }
        
        val sumExp = expLogits.sumOf { it.second }
        val probabilities = expLogits.map { (idx, exp) -> idx to (exp / sumExp) }
        
        // Sample from probabilities
        val random = Random.nextDouble()
        var cumulative = 0.0
        
        for ((idx, prob) in probabilities) {
            cumulative += prob
            if (random < cumulative) {
                return idx.toLong()
            }
        }
        
        return topK.first().first.toLong()
    }
    
    fun close() {
        session?.close()
        session = null
    }
}
