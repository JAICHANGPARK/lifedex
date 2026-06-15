package com.lifedex

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifedex.ui.GotchaViewModel
import com.lifedex.ui.components.GotchaBottomNavbar
import com.lifedex.ui.components.GotchaBrandingHeader
import com.lifedex.ui.components.GotchaCardDetailsDialog
import com.lifedex.ui.components.GeminiDiagnosisDialog
import com.lifedex.ui.components.GeographicZoneDialog
import com.lifedex.ui.screens.DexListingScreen
import com.lifedex.ui.screens.ScannerWorkspaceScreen
import com.lifedex.ui.theme.CyberSlate
import com.lifedex.ui.theme.MyApplicationTheme
import com.lifedex.ui.theme.CardSlate
import com.lifedex.ui.theme.NeonCyan
import com.lifedex.ui.theme.NeonMagenta
import com.lifedex.ui.theme.CrimsonAlert
import com.lifedex.utils.BitmapUtils
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                GotchaAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GotchaAppScreen(
    modifier: Modifier = Modifier,
    viewModel: GotchaViewModel = viewModel()
) {
    val context = LocalContext.current
    val cardsList by viewModel.cards.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val activeTab = uiState.activeTab
    val isScanning = uiState.isScanning
    val scanProgress = uiState.scanProgress
    val processedBitmap = uiState.processedBitmap
    val capturedImageUri = uiState.capturedImageUri
    val latVal = uiState.latitude
    val lonVal = uiState.longitude
    val gpsStatus = uiState.locationStatus
    val rarityResult = uiState.rarityResult
    val cardLevel = uiState.cardLevel
    val titleInput = uiState.titleInput
    val isNewCardReady = uiState.isNewCardReady
    val suggestions = uiState.suggestions
    val selectedDetailCard = uiState.selectedDetailCard
    val tempStickers = uiState.tempStickers
    val selectedStickerIndex = uiState.selectedStickerIndex
    val apiErrorDetail = uiState.apiErrorDetail
    val processingStep = uiState.processingStep
    val processingStepLabel = uiState.processingStepLabel
    val cardImageBitmap = uiState.cardImageBitmap
    val showCardTab = uiState.showCardTab

    var showSpotSelector by remember { mutableStateOf(false) }

    // Launchers for image selectors
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.startCoreDeviceScan(uri)
                viewModel.updateActiveTab("SCAN")
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null) {
                // Save thumbnail bitmap to local cache to create a shareable URI
                val uri = BitmapUtils.saveBitmapToCache(context, bitmap)
                if (uri != null) {
                    viewModel.startCoreDeviceScan(uri)
                    viewModel.updateActiveTab("SCAN")
                }
            } else {
                Toast.makeText(context, "Camera capture cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                cameraLauncher.launch(null)
            } else {
                Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val launchCameraWithPermission = {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                          permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            if (granted) {
                viewModel.fetchRealLocation()
                Toast.makeText(context, "GPS Tracking Engaged", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Permission Denied. Manual GPS Fallback Activated.", Toast.LENGTH_LONG).show()
            }
        }
    )

    // Run first localization lock automatically
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            GotchaBottomNavbar(
                activeTab = activeTab,
                onTabChange = { tab ->
                    if (tab == "SCAN" && capturedImageUri == null) {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    } else {
                        viewModel.updateActiveTab(tab)
                    }
                }
            )
        },
        floatingActionButton = {
            if (activeTab == "DEX") {
                FloatingActionButton(
                    onClick = { launchCameraWithPermission() },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(bottom = 16.dp, end = 8.dp)
                        .testTag("fab_quick_camera")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Quick scan capture",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Main Top Branding Header
                GotchaBrandingHeader(
                    gpsIndicator = "$gpsStatus [${String.format(Locale.US, "%.4f", latVal)}°, ${String.format(Locale.US, "%.4f", lonVal)}°]",
                    onSelectHotspot = { showSpotSelector = true }
                )

                // Switch Screen based on Tab selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (activeTab == "DEX") {
                        DexListingScreen(
                            viewModel = viewModel,
                            cards = cardsList,
                            onCardSelect = { viewModel.selectDetailCard(it) },
                            onClearAll = { viewModel.clearAllCollection() }
                        )
                    } else {
                        ScannerWorkspaceScreen(
                            isScanning = isScanning,
                            scanProgress = scanProgress,
                            processedBitmap = processedBitmap,
                            capturedImageUri = capturedImageUri,
                            rarity = rarityResult,
                            level = cardLevel,
                            title = titleInput,
                            suggestions = suggestions,
                            isNewCardReady = isNewCardReady,
                            tempStickers = tempStickers,
                    selectedStickerIndex = selectedStickerIndex,
                            processingStep = processingStep,
                            processingStepLabel = processingStepLabel,
                            cardImageBitmap = cardImageBitmap,
                            showCardTab = showCardTab,
                            isAwaitingSubjectSelection = uiState.isAwaitingSubjectSelection,
                            combinedMaskImage = uiState.combinedMaskImage,
                            detectedSubjects = uiState.detectedSubjects,
                            onSubjectSelect = { viewModel.selectSubjectAndContinue(it) },
                            onStickerSelect = { viewModel.selectSticker(it) },
                            onTitleChange = { viewModel.updateCurrentStickerTitle(it) },
                            onToggleCardView = { viewModel.toggleCardView() },
                            onPhotoPick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onCameraClick = {
                                launchCameraWithPermission()
                            },
                            onConfirmSave = {
                                viewModel.commitGeneratedCard()
                                Toast.makeText(context, "Card Collected in Storage!", Toast.LENGTH_SHORT).show()
                            },
                            onDiscardCurrent = {
                                viewModel.discardCurrentSticker()
                                Toast.makeText(context, "Sticker Recycled!", Toast.LENGTH_SHORT).show()
                            },
                            onDiscardAll = {
                                viewModel.resetCaptureStates()
                            }
                        )
                    }
                }
            }

            // Details floating view
            selectedDetailCard?.let { card ->
                GotchaCardDetailsDialog(
                    card = card,
                    onDismiss = { viewModel.selectDetailCard(null) },
                    onDelete = {
                        viewModel.deleteCard(card.id, card.imagePath, card.cardImagePath)
                        Toast.makeText(context, "Record Purged!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Gemini API Diagnostic Error popup
            apiErrorDetail?.let { errorText ->
                GeminiDiagnosisDialog(
                    errorText = errorText,
                    onDismiss = { viewModel.dismissApiError() }
                )
            }

            // Hotspot selector bottom sheet/panel
            if (showSpotSelector) {
                GeographicZoneDialog(
                    hotspots = viewModel.geographicHotspots,
                    onSelectHotspot = { spot ->
                        viewModel.setHotspot(spot)
                        showSpotSelector = false
                    },
                    onEngageGps = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                        showSpotSelector = false
                    },
                    onDismiss = { showSpotSelector = false }
                )
            }
        }
    }
}
