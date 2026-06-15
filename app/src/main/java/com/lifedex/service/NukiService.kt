package com.lifedex.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ProcessedSubject(
    val stickerBitmap: Bitmap,
    val croppedContextBitmap: Bitmap?,
    val maskColor: Int
)

data class ProcessedImageResult(
    val subjects: List<ProcessedSubject>,
    val combinedMaskOverlay: Bitmap,
    val safeBitmap: Bitmap
)

class NukiService(private val context: Context) {

    private val TAG = "NukiService"

    private val segmenter = SubjectSegmentation.getClient(
        SubjectSegmenterOptions.Builder()
            .enableForegroundConfidenceMask()
            .enableForegroundBitmap()
            .build()
    )

    private suspend fun <T> awaitTask(task: com.google.android.gms.tasks.Task<T>): T = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        task.addOnSuccessListener { cont.resume(it) { } }
        task.addOnFailureListener { cont.resumeWithException(it) }
    }

    suspend fun processSubjectImages(imageUri: Uri): ProcessedImageResult = withContext(Dispatchers.IO) {
        val originalBitmap = loadBitmapFromUri(imageUri)
            ?: throw RuntimeException("Failed to load bitmap from URI")
        val fallbackColor = Color.parseColor("#E91E63")
        
        var safeBitmap = originalBitmap
        try {
            var w = originalBitmap.width
            var h = originalBitmap.height
            if (w % 16 != 0 || h % 16 != 0) {
                w = w - (w % 16)
                h = h - (h % 16)
                if (w <= 0) w = 16
                if (h <= 0) h = 16
                safeBitmap = Bitmap.createScaledBitmap(originalBitmap, w, h, true)
            }
            
            val inputImage = InputImage.fromBitmap(safeBitmap, 0)
            val taskResult = awaitTask(segmenter.process(inputImage))
            
            val processedSubjects = mutableListOf<ProcessedSubject>()
            
            val fgBitmap = taskResult.foregroundBitmap
            if (fgBitmap != null) {
                val targetHeight = 500
                val targetWidth = (fgBitmap.width * (targetHeight.toFloat() / fgBitmap.height)).toInt()
                val resizedBitmap = Bitmap.createScaledBitmap(fgBitmap, targetWidth, targetHeight, true)

                val strokeSize = 15f
                val paddedWidth = resizedBitmap.width + (strokeSize * 2).toInt()
                val paddedHeight = resizedBitmap.height + (strokeSize * 2).toInt()
                
                val finalBitmap = Bitmap.createBitmap(paddedWidth, paddedHeight, Bitmap.Config.ARGB_8888)
                val finalCanvas = Canvas(finalBitmap)
                
                val paint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = strokeSize * 2
                    strokeJoin = Paint.Join.ROUND
                    strokeCap = Paint.Cap.ROUND
                }
                val alphaBitmap = resizedBitmap.extractAlpha()
                finalCanvas.drawBitmap(alphaBitmap, strokeSize, strokeSize, paint)
                alphaBitmap.recycle()
                
                val originalPaint = Paint().apply {
                    isAntiAlias = true
                    isFilterBitmap = true
                }
                finalCanvas.drawBitmap(resizedBitmap, strokeSize, strokeSize, originalPaint)
                resizedBitmap.recycle()
                
                val fallbackColors = listOf("#FFD54F", "#4FC3F7", "#81C784", "#E57373", "#BA68C8", "#FFB74D", "#4DB6AC")
                val subjectColor = Color.parseColor(fallbackColors.random())
                processedSubjects.add(ProcessedSubject(finalBitmap, originalBitmap, subjectColor))
            }
            
            val combinedOverlay = Bitmap.createBitmap(safeBitmap.width, safeBitmap.height, Bitmap.Config.ARGB_8888)
            val combinedCanvas = Canvas(combinedOverlay)
            val paint = Paint()
            val colorMatrix = android.graphics.ColorMatrix()
            colorMatrix.setSaturation(0.2f)
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
            combinedCanvas.drawBitmap(safeBitmap, 0f, 0f, paint)
            
            if (fgBitmap != null) {
                val overlayPaint = Paint().apply {
                    isAntiAlias = true
                    isFilterBitmap = true
                }
                combinedCanvas.drawBitmap(fgBitmap, 0f, 0f, overlayPaint)
            }
            
            return@withContext ProcessedImageResult(processedSubjects, combinedOverlay, safeBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "processSubjectImages failed: ${e.message}", e)
            
            val targetSize = 500
            val fallbackBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val fallbackCanvas = Canvas(fallbackBitmap)
            val path = android.graphics.Path()
            path.addCircle(targetSize / 2f, targetSize / 2f, targetSize / 2f, android.graphics.Path.Direction.CW)
            fallbackCanvas.clipPath(path)
            
            val scale = targetSize.toFloat() / Math.max(originalBitmap.width, originalBitmap.height)
            val scaledW = (originalBitmap.width * scale).toInt()
            val scaledH = (originalBitmap.height * scale).toInt()
            val resizedBg = Bitmap.createScaledBitmap(originalBitmap, scaledW, scaledH, true)
            val dx = (targetSize - scaledW) / 2f
            val dy = (targetSize - scaledH) / 2f
            fallbackCanvas.drawBitmap(resizedBg, dx, dy, null)
            resizedBg.recycle()
            
            val fallbackSubject = ProcessedSubject(fallbackBitmap, originalBitmap, fallbackColor)
            return@withContext ProcessedImageResult(listOf(fallbackSubject), originalBitmap, originalBitmap)
        }
    }

    fun applyPolygonStickerBorder(original: Bitmap, normalizedPoints: List<android.graphics.PointF>): Bitmap {
        val w = original.width
        val h = original.height
        
        val targetHeight = 500
        val targetWidth = (w * (targetHeight.toFloat() / h)).toInt()
        val scaledOriginal = Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
        
        val strokeSize = 15f
        val paddedWidth = targetWidth + (strokeSize * 2).toInt()
        val paddedHeight = targetHeight + (strokeSize * 2).toInt()
        
        val finalBitmap = Bitmap.createBitmap(paddedWidth, paddedHeight, Bitmap.Config.ARGB_8888)
        val finalCanvas = Canvas(finalBitmap)
        
        val path = Path()
        if (normalizedPoints.isNotEmpty()) {
            val first = normalizedPoints.first()
            path.moveTo(first.x * targetWidth + strokeSize, first.y * targetHeight + strokeSize)
            for (i in 1 until normalizedPoints.size) {
                val pt = normalizedPoints[i]
                path.lineTo(pt.x * targetWidth + strokeSize, pt.y * targetHeight + strokeSize)
            }
            path.close()
        }
        
        val strokePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = strokeSize * 2
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        finalCanvas.drawPath(path, strokePaint)
        
        val fillPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        finalCanvas.drawPath(path, fillPaint)
        
        val srcRect = android.graphics.Rect(0, 0, targetWidth, targetHeight)
        val dstRect = android.graphics.RectF(strokeSize, strokeSize, targetWidth + strokeSize.toFloat(), targetHeight + strokeSize.toFloat())
        val imagePaint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        }
        finalCanvas.drawBitmap(scaledOriginal, srcRect, dstRect, imagePaint)
        
        scaledOriginal.recycle()
        return finalBitmap
    }

    fun applyCapsuleStickerBorder(original: Bitmap): Bitmap {
        val w = original.width
        val h = original.height
        
        val targetHeight = 500
        val targetWidth = (w * (targetHeight.toFloat() / h)).toInt()
        val scaledOriginal = Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
        
        val strokeSize = 15f
        val paddedWidth = targetWidth + (strokeSize * 2).toInt()
        val paddedHeight = targetHeight + (strokeSize * 2).toInt()
        
        val finalBitmap = Bitmap.createBitmap(paddedWidth, paddedHeight, Bitmap.Config.ARGB_8888)
        val finalCanvas = Canvas(finalBitmap)
        
        val rectF = android.graphics.RectF(strokeSize, strokeSize, targetWidth + strokeSize.toFloat(), targetHeight + strokeSize.toFloat())
        val cornerRadius = Math.min(targetWidth, targetHeight) / 2f
        
        val strokePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = strokeSize * 2
            isAntiAlias = true
        }
        finalCanvas.drawRoundRect(rectF, cornerRadius, cornerRadius, strokePaint)
        
        val fillPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        finalCanvas.drawRoundRect(rectF, cornerRadius, cornerRadius, fillPaint)
        
        val srcRect = android.graphics.Rect(0, 0, targetWidth, targetHeight)
        val imagePaint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        }
        finalCanvas.drawBitmap(scaledOriginal, srcRect, rectF, imagePaint)
        
        scaledOriginal.recycle()
        return finalBitmap
    }

    fun saveNukiToFile(bitmap: Bitmap): String {
        return try {
            val file = File(context.cacheDir, "nuki_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
    
    fun saveCardImageToFile(bitmap: Bitmap): String {
        return saveNukiToFile(bitmap)
    }

    fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val maxDim = 800
            val resolver = context.contentResolver
            
            val options = android.graphics.BitmapFactory.Options()
            options.inJustDecodeBounds = true
            resolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream, null, options)
            }
            
            var inSampleSize = 1
            if (options.outHeight > maxDim || options.outWidth > maxDim) {
                val halfHeight: Int = options.outHeight / 2
                val halfWidth: Int = options.outWidth / 2
                while (halfHeight / inSampleSize >= maxDim && halfWidth / inSampleSize >= maxDim) {
                    inSampleSize *= 2
                }
            }
            
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            
            var bitmap = resolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream, null, options)
            }
            
            if (bitmap == null) return null

            resolver.openInputStream(uri)?.use { stream ->
                val exifInterface = androidx.exifinterface.media.ExifInterface(stream)
                val orientation = exifInterface.getAttributeInt(
                    androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                )
                
                val matrix = android.graphics.Matrix()
                when (orientation) {
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }
                
                if (orientation == androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 ||
                    orientation == androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 ||
                    orientation == androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270) {
                    val rotatedBitmap = Bitmap.createBitmap(bitmap!!, 0, 0, bitmap!!.width, bitmap!!.height, matrix, true)
                    if (rotatedBitmap != bitmap) {
                        bitmap!!.recycle()
                        bitmap = rotatedBitmap
                    }
                }
            }
            
            val tightlyPacked = Bitmap.createBitmap(bitmap!!.width, bitmap!!.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(tightlyPacked)
            canvas.drawBitmap(bitmap!!, 0f, 0f, null)
            bitmap!!.recycle()
            
            return tightlyPacked
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from URI: ${e.message}")
            null
        }
    }
}
