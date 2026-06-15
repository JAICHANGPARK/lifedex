package com.lifedex.ui

import android.graphics.Bitmap
import android.net.Uri
import com.lifedex.data.GotchaCard
import com.lifedex.service.ProcessedSubject

data class GotchaUiState(
    val activeTab: String = "DEX",
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val processedBitmap: Bitmap? = null,
    val capturedImageUri: Uri? = null,
    val latitude: Double = 37.5665,
    val longitude: Double = 126.9780,
    val locationStatus: String = "Seoul (Default)",
    val rarityResult: String = "COMMON",
    val cardLevel: Int = 15,
    val titleInput: String = "",
    val isNewCardReady: Boolean = false,
    val suggestions: List<String> = listOf("Futuristic Artifact", "Cyber Cup", "Mechanical Gear"),
    val tempStickers: List<TempSticker> = emptyList(),
    val selectedStickerIndex: Int = 0,
    val isAwaitingSubjectSelection: Boolean = false,
    val combinedMaskImage: Bitmap? = null,
    val detectedSubjects: List<ProcessedSubject> = emptyList(),
    val apiErrorDetail: String? = null,
    val processingStep: Int = 0,
    val processingStepLabel: String = "",
    val cardImageBitmap: Bitmap? = null,
    val showCardTab: Boolean = false,
    val selectedDetailCard: GotchaCard? = null
)
