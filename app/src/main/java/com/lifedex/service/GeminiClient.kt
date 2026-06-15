package com.lifedex.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.lifedex.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class GeminiClient(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val onStepLabelUpdate: ((String) -> Unit)? = null
) {
    private val TAG = "GeminiClient"

    private val POLYGON_SCHEMA_JSON = """
        {
          "type": "ARRAY",
          "items": {
            "type": "OBJECT",
            "properties": {
              "label": {
                "type": "STRING",
                "description": "Label of the object in Korean (e.g. 게코도마뱀)"
              },
              "box_2d": {
                "type": "ARRAY",
                "description": "Bounding box [ymin, xmin, ymax, xmax] normalized to 0-1000",
                "items": {
                  "type": "INTEGER"
                }
              },
              "polygon": {
                "type": "ARRAY",
                "description": "A list of points outlining the object contour in order. Each point is an array [y, x] normalized to 0-1000. Provide 20-30 points to trace the outline.",
                "items": {
                  "type": "ARRAY",
                  "items": {
                    "type": "INTEGER"
                  }
                }
              }
            },
            "required": ["label", "box_2d", "polygon"]
          }
        }
    """.trimIndent()

    private fun resizeBitmapIfNecessary(src: Bitmap, maxDim: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= maxDim && h <= maxDim) {
            return src
        }
        val ratio = w.toFloat() / h.toFloat()
        val (newW, newH) = if (w > h) {
            maxDim to (maxDim / ratio).toInt()
        } else {
            (maxDim * ratio).toInt() to maxDim
        }
        Log.d(TAG, "Resizing bitmap from ${w}x${h} to ${newW}x${newH} for Gemini API")
        return Bitmap.createScaledBitmap(src, newW, newH, true)
    }

    suspend fun generateWithFallback(
        prompt: String,
        bitmap: Bitmap,
        isJson: Boolean = false,
        jsonSchemaStr: String? = null,
        enableGrounding: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val models = listOf("gemini-2.5-flash", "gemini-3.5-flash", "gemini-2.5-flash-lite")
        
        val resized = resizeBitmapIfNecessary(bitmap, 768)
        val outputStream = java.io.ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val base64Image = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
        
        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            
        var lastException: Exception? = null
        val maxRetries = 2  // Max retries per model for 429 errors
        
        for (modelName in models) {
            for (attempt in 0..maxRetries) {
                try {
                    if (attempt > 0) {
                        Log.d(TAG, "Retry attempt $attempt for model $modelName")
                    } else {
                        Log.d(TAG, "Trying Gemini generateContent via OkHttp with model: $modelName")
                    }
                    
                    val payload = JSONObject().apply {
                        put("contents", JSONArray().apply {
                            put(JSONObject().apply {
                                put("parts", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("inlineData", JSONObject().apply {
                                            put("mimeType", "image/jpeg")
                                            put("data", base64Image)
                                        })
                                    })
                                    put(JSONObject().apply {
                                        put("text", prompt)
                                    })
                                })
                            })
                        })
                        
                        if (isJson) {
                            put("generationConfig", JSONObject().apply {
                                put("responseMimeType", "application/json")
                                put("temperature", 0.0)
                                if (jsonSchemaStr != null) {
                                    put("responseSchema", JSONObject(jsonSchemaStr))
                                }
                            })
                        }

                        if (enableGrounding) {
                            put("tools", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("google_search", JSONObject())
                                })
                            })
                        }
                    }
                    
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val requestBody = payload.toString().toRequestBody(mediaType)
                    
                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()
                        
                    client.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string()
                        
                        // Handle rate limit with retry
                        if (response.code == 429) {
                            Log.w(TAG, "Rate limited (429) for $modelName, attempt $attempt")
                            val isQuotaExceeded = responseBody?.contains("Quota exceeded", ignoreCase = true) == true ||
                                                  responseBody?.contains("quota", ignoreCase = true) == true
                            if (isQuotaExceeded) {
                                Log.e(TAG, "Quota limit is zero or exceeded. Aborting retries immediately.")
                                throw RuntimeException("Quota exceeded: $responseBody")
                            }
                            if (attempt < maxRetries) {
                                val retryDelaySec = try {
                                    val errJson = JSONObject(responseBody ?: "{}")
                                    val details = errJson.getJSONObject("error").getJSONArray("details")
                                    var delaySec = 25L
                                    for (i in 0 until details.length()) {
                                        val detail = details.getJSONObject(i)
                                        if (detail.getString("@type").contains("RetryInfo")) {
                                            val delayStr = detail.getString("retryDelay")
                                            delaySec = delayStr.replace("s", "").toDoubleOrNull()?.toLong() ?: 25L
                                            break
                                        }
                                    }
                                    delaySec
                                } catch (e: Exception) { 25L }
                                
                                val waitMs = (retryDelaySec + 2) * 1000  // Add 2s buffer
                                Log.d(TAG, "Waiting ${waitMs}ms before retry...")
                                onStepLabelUpdate?.invoke("API 대기 중... (${retryDelaySec}초)")
                                delay(waitMs)
                                continue  // Retry same model
                            }
                            throw RuntimeException("Rate limited after $maxRetries retries: $responseBody")
                        }
                        
                        if (!response.isSuccessful) {
                            throw RuntimeException("HTTP ${response.code}: $responseBody")
                        }
                        if (responseBody != null) {
                            val resJson = JSONObject(responseBody)
                            val candidates = resJson.getJSONArray("candidates")
                            if (candidates.length() > 0) {
                                val candidate = candidates.getJSONObject(0)
                                val content = candidate.getJSONObject("content")
                                val parts = content.getJSONArray("parts")
                                if (parts.length() > 0) {
                                    val text = parts.getJSONObject(0).getString("text")
                                    if (text.isNotBlank()) {
                                        Log.d(TAG, "Successfully generated content with model $modelName via OkHttp")
                                        if (resized != bitmap) {
                                            resized.recycle()
                                        }
                                        return@withContext text
                                    }
                                }
                            }
                        }
                        throw RuntimeException("Empty response or invalid structure: $responseBody")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Gemini OkHttp call failed for model $modelName (attempt $attempt): ${e.message}")
                    lastException = e
                    if (e.message?.contains("429") == true && attempt < maxRetries) {
                        continue  // Will retry
                    }
                    break  // Move to next model
                }
            }
        }
        
        if (resized != bitmap) {
            resized.recycle()
        }
        throw lastException ?: RuntimeException("All Gemini models failed")
    }

    suspend fun analyzeImageWithGemini(bitmap: Bitmap): List<String>? {
        return try {
            Log.d(TAG, "Requesting Gemini analysis for bitmap...")
            val prompt = "Identify the main object in this image in detail. Since this is an encyclopedia app, identify the specific species, breed, model, or type of the object (e.g. instead of just '고양이', specify '러시안 블루'; instead of '나무', specify '소나무'; instead of '꽃', specify '장미'). Answer with exactly 3 clear related nouns in Korean, separated only by a comma (e.g., '러시안 블루, 고양이, 반려동물'). Do not write a sentence, just the 3 nouns."
            val detected = generateWithFallback(prompt, bitmap, isJson = false, enableGrounding = true).trim()
            Log.d(TAG, "Gemini response: $detected")
            if (detected.isNotEmpty()) {
                detected.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API classification error: ${e.message}", e)
            throw e
        }
    }

    data class GeminiDetectedObject(
        val label: String,
        val box: List<Int>,
        val polygon: List<List<Float>>?
    )

    suspend fun detectObjectsWithGemini(bitmap: Bitmap): List<GeminiDetectedObject> {
        try {
            Log.d(TAG, "Requesting Gemini object detection with contour polygons...")
            val prompt = "Detect all prominent objects in the image. For each object, locate it using a 2D bounding box (box_2d) and trace its precise contour outline using a polygon of 20 to 35 points [y, x] in order. Provide the label in Korean."
            val jsonText = generateWithFallback(prompt, bitmap, isJson = true, jsonSchemaStr = POLYGON_SCHEMA_JSON).trim()
            Log.d(TAG, "Gemini Object Detection response: $jsonText")
            
            val cleanedJson = if (jsonText.startsWith("```")) {
                val lines = jsonText.split("\n")
                lines.filter { !it.trim().startsWith("```") }.joinToString("\n").trim()
            } else {
                jsonText
            }
            
            val jsonArray = JSONArray(cleanedJson)
            val detectedObjects = mutableListOf<GeminiDetectedObject>()
            val maxObjects = 4
            val numToProcess = minOf(jsonArray.length(), maxObjects)
            
            for (i in 0 until numToProcess) {
                val obj = jsonArray.getJSONObject(i)
                val label = obj.getString("label")
                val boxArray = obj.getJSONArray("box_2d")
                
                val box = listOf(
                    boxArray.getInt(0),
                    boxArray.getInt(1),
                    boxArray.getInt(2),
                    boxArray.getInt(3)
                )
                
                val polygonArray = obj.optJSONArray("polygon")
                var polygonPoints: MutableList<List<Float>>? = null
                if (polygonArray != null && polygonArray.length() >= 3) {
                    polygonPoints = mutableListOf()
                    for (j in 0 until polygonArray.length()) {
                        val pt = polygonArray.getJSONArray(j)
                        val yNorm = pt.getDouble(0).toFloat() / 1000f
                        val xNorm = pt.getDouble(1).toFloat() / 1000f
                        polygonPoints.add(listOf(xNorm, yNorm))
                    }
                }
                
                detectedObjects.add(GeminiDetectedObject(label, box, polygonPoints))
            }
            return detectedObjects
        } catch (e: Exception) {
            Log.e(TAG, "Gemini object detection failed: ${e.message}", e)
            throw e
        }
    }

    suspend fun generateImageWithFallback(
        prompt: String,
        bitmap: Bitmap
    ): Bitmap? = withContext(Dispatchers.IO) {
        val models = listOf("gemini-3.1-flash-image", "gemini-3-pro-image", "gemini-2.5-flash-image")
        
        val resized = resizeBitmapIfNecessary(bitmap, 512)
        val outputStream = java.io.ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val base64Image = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
        
        val client = OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            
        var lastException: Exception? = null
        val maxRetries = 2  // Max retries per model for 429 errors
        
        for (modelName in models) {
            for (attempt in 0..maxRetries) {
                try {
                    if (attempt > 0) {
                        Log.d(TAG, "Retry attempt $attempt for image model $modelName")
                    } else {
                        Log.d(TAG, "Trying Gemini generateContent for image via OkHttp with model: $modelName")
                    }
                    
                    val payload = JSONObject().apply {
                        put("contents", JSONArray().apply {
                            put(JSONObject().apply {
                                put("parts", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("inlineData", JSONObject().apply {
                                            put("mimeType", "image/png")
                                            put("data", base64Image)
                                        })
                                    })
                                    put(JSONObject().apply {
                                        put("text", prompt)
                                    })
                                })
                            })
                        })
                        
                        put("generationConfig", JSONObject().apply {
                            put("responseModalities", JSONArray().apply {
                                put("TEXT")
                                put("IMAGE")
                            })
                            put("imageConfig", JSONObject().apply {
                                put("aspectRatio", "3:4")
                                put("imageSize", "1K")
                            })
                        })
                    }
                    
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val requestBody = payload.toString().toRequestBody(mediaType)
                    
                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()
                        
                    client.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string()
                        
                        // Handle rate limit with retry
                        if (response.code == 429) {
                            Log.w(TAG, "Rate limited (429) for image model $modelName, attempt $attempt")
                            val isQuotaExceeded = responseBody?.contains("Quota exceeded", ignoreCase = true) == true ||
                                                  responseBody?.contains("quota", ignoreCase = true) == true
                            if (isQuotaExceeded) {
                                Log.e(TAG, "Quota limit is zero or exceeded. Aborting retries immediately.")
                                throw RuntimeException("Quota exceeded: $responseBody")
                            }
                            if (attempt < maxRetries) {
                                val retryDelaySec = 25L
                                val waitMs = (retryDelaySec + 2) * 1000
                                Log.d(TAG, "Waiting ${waitMs}ms before retry...")
                                onStepLabelUpdate?.invoke("이미지 생성 대기 중... (${retryDelaySec}초)")
                                delay(waitMs)
                                continue  // Retry same model
                            }
                            throw RuntimeException("Rate limited after $maxRetries retries: $responseBody")
                        }
                        
                        if (!response.isSuccessful) {
                            throw RuntimeException("HTTP ${response.code}: $responseBody")
                        }
                        if (responseBody != null) {
                            val resJson = JSONObject(responseBody)
                            val candidates = resJson.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val candidate = candidates.getJSONObject(0)
                                val content = candidate.optJSONObject("content")
                                if (content != null) {
                                    val parts = content.optJSONArray("parts")
                                    if (parts != null && parts.length() > 0) {
                                        for (i in 0 until parts.length()) {
                                            val part = parts.getJSONObject(i)
                                            if (part.has("inlineData")) {
                                                val inlineData = part.getJSONObject("inlineData")
                                                val base64Data = inlineData.getString("data")
                                                val imageBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                                                val generatedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                                if (generatedBitmap != null) {
                                                    Log.d(TAG, "Successfully generated card image with model $modelName")
                                                    if (resized != bitmap) {
                                                        resized.recycle()
                                                    }
                                                    return@withContext generatedBitmap
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        throw RuntimeException("Empty response or invalid structure: $responseBody")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Gemini image call failed for model $modelName (attempt $attempt): ${e.message}")
                    lastException = e
                    if (e.message?.contains("429") == true && attempt < maxRetries) {
                        continue  // Will retry
                    }
                    break  // Move to next model
                }
            }
        }
        
        if (resized != bitmap) {
            resized.recycle()
        }
        throw lastException ?: RuntimeException("All Gemini image models failed")
    }
}
