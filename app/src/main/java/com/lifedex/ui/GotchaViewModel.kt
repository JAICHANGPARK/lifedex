package com.lifedex.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifedex.data.GotchaCard
import com.lifedex.data.GotchaDatabase
import com.lifedex.data.GotchaRepository
import com.lifedex.service.NukiService
import com.lifedex.service.ProcessedSubject
import com.lifedex.service.CardGenerator
import com.lifedex.service.GeminiClient
import com.google.android.gms.location.LocationCallback
import org.json.JSONArray
import org.json.JSONObject
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.async
import com.lifedex.BuildConfig
import com.lifedex.data.Hotspot
import com.lifedex.data.Hotspots

class GotchaViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "GotchaViewModel"
    private val db = GotchaDatabase.getDatabase(application)
    private val repository = GotchaRepository(db.gotchaDao())
    private val nukiService = NukiService(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val geminiClient = com.lifedex.service.GeminiClient(
        onStepLabelUpdate = { stepLabel ->
            processingStepLabel.value = stepLabel
        }
    )

    // Exposed lists of saved cards
    val cards: StateFlow<List<GotchaCard>> = repository.allCards
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Dynamic state variables
    private val isScanning = MutableStateFlow(false)
    private val scanProgress = MutableStateFlow(0f)
    private val processedBitmap = MutableStateFlow<Bitmap?>(null)
    private val capturedImageUri = MutableStateFlow<Uri?>(null)
    
    // Coordinates
    private val latitude = MutableStateFlow(37.5665) // Default: Seoul coordinates
    private val longitude = MutableStateFlow(126.9780)
    private val locationStatus = MutableStateFlow("Seoul (Default)")

    // Card Generation States
    private val rarityResult = MutableStateFlow("COMMON")
    private val cardLevel = MutableStateFlow(15)
    private val titleInput = MutableStateFlow("")
    private val isNewCardReady = MutableStateFlow(false)
    private val suggestions = MutableStateFlow<List<String>>(listOf("Futuristic Artifact", "Cyber Cup", "Mechanical Gear"))
    private val tempStickers = MutableStateFlow<List<TempSticker>>(emptyList())
    private val selectedStickerIndex = MutableStateFlow(0)
    
    // UI Selection States
    private val isAwaitingSubjectSelection = MutableStateFlow(false)
    private val combinedMaskImage = MutableStateFlow<Bitmap?>(null)
    private val detectedSubjects = MutableStateFlow<List<ProcessedSubject>>(emptyList())
    private val apiErrorDetail = MutableStateFlow<String?>(null)

    // Step-by-step processing state (0=idle, 1=segmentation, 2=sticker, 3=recognition, 4=card generation)
    private val processingStep = MutableStateFlow(0)
    private val processingStepLabel = MutableStateFlow("")
    private val cardImageBitmap = MutableStateFlow<Bitmap?>(null)  // Current Pokemon card image
    private val showCardTab = MutableStateFlow(false)  // Toggle between sticker/card view
    private var cardGenerationJob: kotlinx.coroutines.Job? = null

    // Current screen management
    private val activeTab = MutableStateFlow("DEX") // "DEX", "SCAN"

    // Card Details View
    private val selectedDetailCard = MutableStateFlow<GotchaCard?>(null)

    // Exposed single UI State flow combining all volatile states
    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<GotchaUiState> = kotlinx.coroutines.flow.combine(
        activeTab, isScanning, scanProgress, processedBitmap, capturedImageUri,
        latitude, longitude, locationStatus, rarityResult, cardLevel,
        titleInput, isNewCardReady, suggestions, tempStickers, selectedStickerIndex,
        isAwaitingSubjectSelection, combinedMaskImage, detectedSubjects,
        apiErrorDetail, processingStep, processingStepLabel, cardImageBitmap,
        showCardTab, selectedDetailCard
    ) { array ->
        GotchaUiState(
            activeTab = array[0] as String,
            isScanning = array[1] as Boolean,
            scanProgress = array[2] as Float,
            processedBitmap = array[3] as Bitmap?,
            capturedImageUri = array[4] as Uri?,
            latitude = array[5] as Double,
            longitude = array[6] as Double,
            locationStatus = array[7] as String,
            rarityResult = array[8] as String,
            cardLevel = array[9] as Int,
            titleInput = array[10] as String,
            isNewCardReady = array[11] as Boolean,
            suggestions = array[12] as List<String>,
            tempStickers = array[13] as List<TempSticker>,
            selectedStickerIndex = array[14] as Int,
            isAwaitingSubjectSelection = array[15] as Boolean,
            combinedMaskImage = array[16] as Bitmap?,
            detectedSubjects = array[17] as List<ProcessedSubject>,
            apiErrorDetail = array[18] as String?,
            processingStep = array[19] as Int,
            processingStepLabel = array[20] as String,
            cardImageBitmap = array[21] as Bitmap?,
            showCardTab = array[22] as Boolean,
            selectedDetailCard = array[23] as GotchaCard?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GotchaUiState()
    )

    // Helper functions for state updates from the UI
    fun updateActiveTab(tab: String) {
        activeTab.value = tab
    }

    fun selectDetailCard(card: GotchaCard?) {
        selectedDetailCard.value = card
    }

    fun dismissApiError() {
        apiErrorDetail.value = null
    }

    // Pre-curated famous spots to select if GPS isn't available
    val geographicHotspots = Hotspots.geographicHotspots

    fun setHotspot(hotspot: Hotspot) {
        latitude.value = hotspot.lat
        longitude.value = hotspot.lng
        locationStatus.value = "${hotspot.name} (Curated)"
    }

    /**
     * Attempts to read real GPS updates. If it fails, falls back gracefully.
     */
    fun fetchRealLocation() {
        try {
            locationStatus.value = "Locating target GPS..."
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5000).apply {
                setMinUpdateDistanceMeters(10f)
            }.build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { loc ->
                        latitude.value = loc.latitude
                        longitude.value = loc.longitude
                        locationStatus.value = "GPS Lock Active"
                        Log.d(TAG, "GPS coordinates locked: [${loc.latitude}, ${loc.longitude}]")
                    } ?: run {
                        setupRandomFallbackCoordinates()
                    }
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            ).addOnFailureListener {
                Log.e(TAG, "Location request failed: ${it.message}")
                setupRandomFallbackCoordinates()
            }
        } catch (se: SecurityException) {
            Log.w(TAG, "Location permission missing: ${se.message}")
            setupRandomFallbackCoordinates()
        } catch (e: Exception) {
            setupRandomFallbackCoordinates()
        }
    }

    private fun setupRandomFallbackCoordinates() {
        val randHotspot = geographicHotspots[Random.nextInt(geographicHotspots.size)]
        latitude.value = randHotspot.lat + (Random.nextDouble(-0.01, 0.01))
        longitude.value = randHotspot.lng + (Random.nextDouble(-0.01, 0.01))
        locationStatus.value = "${randHotspot.name} (Simulated)"
    }

    fun selectSticker(index: Int) {
        if (index in tempStickers.value.indices) {
            selectedStickerIndex.value = index
            val sticker = tempStickers.value[index]
            processedBitmap.value = sticker.bitmap
            titleInput.value = sticker.title
            rarityResult.value = sticker.rarity
            cardLevel.value = sticker.level
            suggestions.value = sticker.suggestions
            cardImageBitmap.value = sticker.cardBitmap
            showCardTab.value = false  // Reset to sticker view when switching
            
            if (sticker.cardBitmap == null) {
                generateCardForStickerIndex(index, debounceMs = 0L)
            }
        }
    }

    fun toggleCardView() {
        showCardTab.value = !showCardTab.value
    }

    fun updateCurrentStickerTitle(newTitle: String) {
        titleInput.value = newTitle
        val idx = selectedStickerIndex.value
        val list = tempStickers.value
        if (idx in list.indices) {
            list[idx].title = newTitle
            list[idx].cardBitmap = null
            if (selectedStickerIndex.value == idx) {
                cardImageBitmap.value = null
            }
            generateCardForStickerIndex(idx, debounceMs = 1500L)
        }
    }

    private fun generateCardForStickerIndex(index: Int, debounceMs: Long) {
        cardGenerationJob?.cancel()
        cardGenerationJob = viewModelScope.launch {
            if (debounceMs > 0) {
                delay(debounceMs)
            }
            val list = tempStickers.value
            if (index !in list.indices) return@launch
            val sticker = list[index]
            
            processingStep.value = 4
            processingStepLabel.value = "포켓몬 카드 생성 중..."
            
            val cardBitmap = withContext(Dispatchers.IO) {
                generatePokemonCardImage(
                    stickerBitmap = sticker.bitmap,
                    objectLabel = sticker.title,
                    rarity = sticker.rarity,
                    level = sticker.level
                )
            }
            
            if (cardBitmap != null) {
                val currentList = tempStickers.value.toMutableList()
                if (index in currentList.indices) {
                    currentList[index] = currentList[index].copy(cardBitmap = cardBitmap)
                    tempStickers.value = currentList
                    if (selectedStickerIndex.value == index) {
                        cardImageBitmap.value = cardBitmap
                    }
                }
            }
            
            processingStep.value = 0
            processingStepLabel.value = ""
        }
    }

    private fun determineRandomRarity(): String {
        val roll = Random.nextInt(1, 101)
        return when {
            roll <= 2 -> "LEGENDARY"
            roll <= 10 -> "EPIC"
            roll <= 40 -> "RARE"
            else -> "COMMON"
        }
    }

    private suspend fun analyzeImageWithGemini(bitmap: Bitmap): List<String>? {
        return try {
            geminiClient.analyzeImageWithGemini(bitmap)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                val fullError = "Gemini API Error details:\n\n${e.localizedMessage ?: e.message ?: "Unknown Exception"}\n\nStack trace:\n${e.stackTraceToString()}"
                apiErrorDetail.value = fullError
            }
            null
        }
    }

    private suspend fun detectObjectsWithGemini(uri: Uri): List<TempSticker> = withContext(Dispatchers.IO) {
        val original = nukiService.loadBitmapFromUri(uri) ?: return@withContext emptyList()
        try {
            Log.d(TAG, "Requesting Gemini object detection with contour polygons...")
            val detectedObjects = geminiClient.detectObjectsWithGemini(original)
            
            val stickers = mutableListOf<TempSticker>()
            for (obj in detectedObjects) {
                val label = obj.label
                val box = obj.box
                val ymin = box[0]
                val xmin = box[1]
                val ymax = box[2]
                val xmax = box[3]
                
                val left = (xmin.toFloat() / 1000f * original.width).toInt().coerceAtLeast(0)
                val top = (ymin.toFloat() / 1000f * original.height).toInt().coerceAtLeast(0)
                val right = (xmax.toFloat() / 1000f * original.width).toInt().coerceAtMost(original.width)
                val bottom = (ymax.toFloat() / 1000f * original.height).toInt().coerceAtMost(original.height)
                
                val w = right - left
                val h = bottom - top
                
                val polygonPoints = obj.polygon
                var stickerBitmap: Bitmap? = null
                
                if (polygonPoints != null && polygonPoints.size >= 3) {
                    val points = polygonPoints.map { pt ->
                        android.graphics.PointF(pt[0], pt[1])
                    }
                    stickerBitmap = nukiService.applyPolygonStickerBorder(original, points)
                }
                
                if (stickerBitmap == null && w > 0 && h > 0) {
                    val cropped = Bitmap.createBitmap(original, left, top, w, h)
                    stickerBitmap = nukiService.applyCapsuleStickerBorder(cropped)
                }
                
                if (stickerBitmap != null) {
                    val suggestionsList = listOf(label, "${label} 스티커", "생명체")
                    
                    stickers.add(
                        TempSticker(
                            bitmap = stickerBitmap,
                            title = label,
                            rarity = determineRandomRarity(),
                            level = Random.nextInt(10, 100),
                            suggestions = suggestionsList
                        )
                    )
                }
            }
            stickers
        } catch (e: Exception) {
            Log.e(TAG, "Gemini object detection failed: ${e.message}")
            withContext(Dispatchers.Main) {
                val fullError = "Gemini Object Detection Failure:\n\n${e.localizedMessage ?: e.message ?: "Unknown Exception"}\n\nStack trace:\n${e.stackTraceToString()}"
                apiErrorDetail.value = fullError
            }
            emptyList()
        } finally {
            original.recycle()
        }
    }

    private suspend fun generatePokemonCardImage(
        stickerBitmap: Bitmap,
        objectLabel: String,
        rarity: String,
        level: Int
    ): Bitmap? {
        return try {
            val seed = objectLabel.hashCode()
            val typeIndex = Math.abs(seed) % CardGenerator.PokemonType.values().size
            val pokemonType = CardGenerator.PokemonType.values()[typeIndex]
            val hpValue = (level * 2).coerceAtLeast(10) + 70

            val prompt = """
                Create a high-quality vertical Pokémon-style trading card image.
                The primary subject of the card must be the object from the provided image, stylized as a powerful character/creature matching the element type ${pokemonType.typeName}.
                
                Card Layout and Visual Requirements:
                1. Vertical card layout (3:4 aspect ratio) with rounded corners and a premium border matching the element color (${pokemonType.colorHex}).
                2. The card must have a main illustration box containing the stylized character.
                3. The card rarity is $rarity. If rarity is LEGENDARY or EPIC, make it a Full-Art holographic "ex" card with a glowing radial gradient and diagonal light beams, and append " ex" to the name. Otherwise, draw a classic yellow/gold border card.
                
                Text and Stats Requirements (Language must be Korean):
                1. Title: Render the name "$objectLabel" in a clean, bold, stylized font at the top-left of the card.
                2. HP: Render "HP $hpValue" in a bold red font at the top-right next to the element symbol.
                3. Below the main illustration, render two move details:
                   - Move 1 name, description, and damage (e.g. 50).
                   - Move 2 name, description, and damage (e.g. 120).
                   - The moves, energy symbols, and description text must be in clean, legible Korean.
                4. Bottom Footer: Render Weakness, Resistance, and Retreat stats (using Korean terms: 약점, 저항력, 후퇴).
                5. Include copyright text "© 2026 LifeDex" at the very bottom.
                
                The entire card must be visual, unified, and look like an authentic physical Pokémon trading card. Do not output anything other than the card image itself.
            """.trimIndent()

            Log.d(TAG, "Generating Pokémon card via Gemini Image API for ${objectLabel}...")
            val generated = geminiClient.generateImageWithFallback(prompt, stickerBitmap)
            if (generated != null) {
                Log.d(TAG, "Successfully generated card via Gemini Image API for ${objectLabel}!")
                generated
            } else {
                Log.w(TAG, "Gemini Image generation returned null. Falling back to local Canvas engine.")
                CardGenerator.generatePokemonCardImage(stickerBitmap, objectLabel, rarity, level)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Image generation failed: ${e.message}. Falling back to local Canvas engine.", e)
            CardGenerator.generatePokemonCardImage(stickerBitmap, objectLabel, rarity, level)
        }
    }

    /**
     * Runs scanning process: starts visual sweep, triggers segmentation, generates rarity
     */
    fun startCoreDeviceScan(uri: Uri, userProposedTitle: String = "") {
        capturedImageUri.value = uri
        titleInput.value = userProposedTitle.ifBlank { "분석 중..." }
        isScanning.value = true
        scanProgress.value = 0f
        processedBitmap.value = null
        isNewCardReady.value = false
        tempStickers.value = emptyList()
        selectedStickerIndex.value = 0
        processingStep.value = 0
        processingStepLabel.value = ""
        cardImageBitmap.value = null
        showCardTab.value = false

        viewModelScope.launch {
            // ═══════════════════════════════════════════
            // STEP 1: Subject Segmentation (객체 분리)
            // ═══════════════════════════════════════════
            processingStep.value = 1
            processingStepLabel.value = "객체 분리 중..."
            scanProgress.value = 0.05f

            val segmentationResult = withContext(Dispatchers.IO) {
                try {
                    nukiService.processSubjectImages(uri)
                } catch (e: Exception) {
                    Log.e(TAG, "ML Kit segmentation failed: ${e.message}")
                    null
                }
            }
            scanProgress.value = 0.25f

            if (segmentationResult != null && segmentationResult.subjects.isNotEmpty()) {
                // We have ML Kit subjects, enter Step 1.5 (Selection)
                Log.d(TAG, "ML Kit found ${segmentationResult.subjects.size} subjects. Awaiting selection.")
                detectedSubjects.value = segmentationResult.subjects
                combinedMaskImage.value = segmentationResult.combinedMaskOverlay
                isAwaitingSubjectSelection.value = true
                processingStepLabel.value = "객체 선택 대기 중..."
                return@launch // Pause here until user selects
            }
            
            // Fallback: If ML Kit failed or found nothing, proceed automatically with Gemini
            Log.d(TAG, "ML Kit returned no subjects. Using Gemini fallback.")
            continueScanWithSubject(null)
        }
    }

    fun selectSubjectAndContinue(index: Int) {
        val subjects = detectedSubjects.value
        if (index in subjects.indices) {
            isAwaitingSubjectSelection.value = false
            combinedMaskImage.value = null // clear memory
            
            val selectedSubject = subjects[index]
            viewModelScope.launch {
                continueScanWithSubject(selectedSubject)
            }
        }
    }

    private suspend fun continueScanWithSubject(selectedSubject: ProcessedSubject?) {
        val uri = capturedImageUri.value ?: return
        
        // ═══════════════════════════════════════════
        // STEP 2: Sticker Creation (배경 제거 & 스티커)
        // ═══════════════════════════════════════════
        processingStep.value = 2
        processingStepLabel.value = "스티커 생성 중..."
        
        val rawStickers: List<TempSticker> = withContext(Dispatchers.IO) {
            if (selectedSubject != null) {
                listOf(
                    TempSticker(
                        bitmap = selectedSubject.stickerBitmap,
                        title = "스티커 #1",
                        rarity = determineRandomRarity(),
                        level = Random.nextInt(10, 100),
                        suggestions = listOf("스티커 #1", "Specimen", "Gotcha")
                    )
                )
            } else {
                // Fallback to Gemini
                val geminiStickers = detectObjectsWithGemini(uri)
                if (geminiStickers.isNotEmpty()) geminiStickers else emptyList()
            }
        }
        
        scanProgress.value = 0.50f
        
        tempStickers.value = rawStickers
        if (rawStickers.isNotEmpty()) {
            withContext(Dispatchers.Main) { selectSticker(0) }
        }

        // ═══════════════════════════════════════════
        // STEP 3: Object Recognition (객체 인식)
        // ═══════════════════════════════════════════
        processingStep.value = 3
        processingStepLabel.value = "객체 인식 중..."
        
        val updatedStickers = withContext(Dispatchers.IO) {
            rawStickers.mapIndexed { idx, sticker ->
                val bitmapForAnalysis = if (selectedSubject != null && idx == 0) {
                    selectedSubject.croppedContextBitmap ?: sticker.bitmap
                } else {
                    sticker.bitmap
                }
                val geminiResult = analyzeImageWithGemini(bitmapForAnalysis)
                val labels = geminiResult ?: listOf("스티커 #${idx + 1}", "Specimen", "LifeDex")
                sticker.copy(title = labels.first(), suggestions = labels)
            }
        }
        scanProgress.value = 0.75f
        
        tempStickers.value = updatedStickers
        if (updatedStickers.isNotEmpty()) {
            withContext(Dispatchers.Main) { selectSticker(0) }
        } else {
            titleInput.value = titleInput.value.ifBlank { "미확인 객체" }
            suggestions.value = listOf("미래 유물", "사이버 컵", "기계 부품")
        }

        // Generate rarity and level
        determineCardRarity()
        cardLevel.value = Random.nextInt(10, 100)
        
        isScanning.value = false
        isNewCardReady.value = true

        // Clean up remaining cropped bitmaps from ML Kit
        detectedSubjects.value.forEach { subject ->
            if (subject != selectedSubject) subject.croppedContextBitmap?.recycle()
        }
        detectedSubjects.value = emptyList()

        // ═══════════════════════════════════════════
        // STEP 4: Pokémon Card Generation (카드 생성)
        // ═══════════════════════════════════════════
        processingStep.value = 4
        processingStepLabel.value = "포켓몬 카드 생성 중..."

        if (updatedStickers.isNotEmpty()) {
            val firstSticker = updatedStickers[0]
            val cardBitmap = withContext(Dispatchers.IO) {
                generatePokemonCardImage(
                    stickerBitmap = firstSticker.bitmap,
                    objectLabel = firstSticker.title,
                    rarity = firstSticker.rarity,
                    level = firstSticker.level
                )
            }
            if (cardBitmap != null) {
                val finalStickers = updatedStickers.toMutableList()
                finalStickers[0] = finalStickers[0].copy(cardBitmap = cardBitmap)
                tempStickers.value = finalStickers
                if (selectedStickerIndex.value == 0) {
                    cardImageBitmap.value = cardBitmap
                }
            }
        }

        processingStep.value = 0
        processingStepLabel.value = ""
        scanProgress.value = 1f
    }

    private fun determineCardRarity() {
        val roll = Random.nextInt(1, 101)
        rarityResult.value = when {
            roll <= 2 -> "LEGENDARY"  // 2%
            roll <= 10 -> "EPIC"     // 8%
            roll <= 40 -> "RARE"     // 30%
            else -> "COMMON"         // 60%
        }
    }

    /**
     * Persists the scanned card to Room database. Saves file to internal storage.
     */
    fun commitGeneratedCard() {
        val idx = selectedStickerIndex.value
        val list = tempStickers.value.toMutableList()
        if (idx !in list.indices) return
        
        val currentSticker = list[idx]
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Save transparent png file on device filesDir
                val savedPath = nukiService.saveNukiToFile(currentSticker.bitmap)
                
                // 2. Save card image if available
                val cardPath = currentSticker.cardBitmap?.let { 
                    nukiService.saveCardImageToFile(it)
                }
                
                // 3. Create the Room Entity
                val card = GotchaCard(
                    title = currentSticker.title.trim().ifBlank { "Gotcha Object" },
                    imagePath = savedPath,
                    cardImagePath = cardPath,
                    latitude = latitude.value,
                    longitude = longitude.value,
                    rarity = currentSticker.rarity,
                    level = currentSticker.level
                )

                // 4. Store
                repository.insertCard(card)
                Log.d(TAG, "Saved new gotcha card: $card")

                // Reset state & jump back to dex view
                withContext(Dispatchers.Main) {
                    list.removeAt(idx)
                    tempStickers.value = list
                    if (list.isNotEmpty()) {
                        selectSticker(0)
                    } else {
                        resetCaptureStates()
                        activeTab.value = "DEX"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save card: ${e.message}")
            }
        }
    }

    fun discardCurrentSticker() {
        val idx = selectedStickerIndex.value
        val list = tempStickers.value.toMutableList()
        if (idx !in list.indices) return
        
        list.removeAt(idx)
        tempStickers.value = list
        if (list.isNotEmpty()) {
            selectSticker(0)
        } else {
            resetCaptureStates()
            activeTab.value = "DEX"
        }
    }

    fun resetCaptureStates() {
        capturedImageUri.value = null
        processedBitmap.value = null
        isNewCardReady.value = false
        titleInput.value = ""
        scanProgress.value = 0f
        suggestions.value = listOf("미래 유물", "사이버 컵", "기계 부품")
        tempStickers.value = emptyList()
        selectedStickerIndex.value = 0
        processingStep.value = 0
        processingStepLabel.value = ""
        cardImageBitmap.value = null
        showCardTab.value = false
    }

    fun deleteCard(cardId: Int, imagePath: String, cardImagePath: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Delete sticker image file
                val file = File(imagePath)
                if (file.exists()) {
                    file.delete()
                }
                // Delete card image file if exists
                cardImagePath?.let {
                    val cardFile = File(it)
                    if (cardFile.exists()) {
                        cardFile.delete()
                    }
                }
                repository.deleteCard(cardId)
                
                if (selectedDetailCard.value?.id == cardId) {
                    selectedDetailCard.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearAllCollection() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
            // Clean up filesDir
            try {
                val files = getApplication<Application>().filesDir.listFiles()
                files?.forEach { file ->
                    if (file.name.contains("gotcha_") && file.name.endsWith(".png")) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

data class Hotspot(val name: String, val lat: Double, val lng: Double)

data class TempSticker(
    val bitmap: Bitmap,
    var title: String,
    val rarity: String,
    val level: Int,
    val suggestions: List<String>,
    var cardBitmap: Bitmap? = null  // Generated Pokemon card image
)
