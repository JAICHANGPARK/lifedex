package com.lifedex.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign.Companion.Center as CenterText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lifedex.service.ProcessedSubject
import com.lifedex.ui.TempSticker
import com.lifedex.ui.components.RarityHelpers.getRarityBgColor
import com.lifedex.ui.components.RarityHelpers.getRarityColor
import com.lifedex.ui.components.RarityHelpers.getRarityTextColor
import com.lifedex.ui.theme.CrimsonAlert
import com.lifedex.ui.theme.CyberYellow
import com.lifedex.ui.theme.NeonCyan
import com.lifedex.ui.theme.NeonMagenta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerWorkspaceScreen(
    isScanning: Boolean,
    scanProgress: Float,
    processedBitmap: Bitmap?,
    capturedImageUri: Uri?,
    rarity: String,
    level: Int,
    title: String,
    suggestions: List<String>,
    isNewCardReady: Boolean,
    tempStickers: List<TempSticker>,
    selectedStickerIndex: Int,
    processingStep: Int,
    processingStepLabel: String,
    cardImageBitmap: Bitmap?,
    showCardTab: Boolean,
    isAwaitingSubjectSelection: Boolean,
    combinedMaskImage: Bitmap?,
    detectedSubjects: List<ProcessedSubject>,
    onSubjectSelect: (Int) -> Unit,
    onStickerSelect: (Int) -> Unit,
    onTitleChange: (String) -> Unit,
    onToggleCardView: () -> Unit,
    onPhotoPick: () -> Unit,
    onCameraClick: () -> Unit,
    onConfirmSave: () -> Unit,
    onDiscardCurrent: () -> Unit,
    onDiscardAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High-tech capsule displaying scanner state & cancel all button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SUBJECT STICKER GENERATOR",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
            
            if (capturedImageUri != null) {
                TextButton(
                    onClick = onDiscardAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = CrimsonAlert)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel All", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CANCEL ALL", fontSize = 11.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ═══════════════════════════════════════════
        // 4-Step Processing Indicator
        // ═══════════════════════════════════════════
        if (capturedImageUri != null && (isScanning || processingStep > 0)) {
            val stepLabels = listOf("객체 분리", "스티커 생성", "객체 인식", "카드 생성")
            val stepIcons = listOf(
                Icons.Outlined.ContentCut,
                Icons.Outlined.AutoAwesome,
                Icons.Outlined.ImageSearch,
                Icons.Outlined.Style
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        stepLabels.forEachIndexed { index, label ->
                            val stepNum = index + 1
                            val isActive = processingStep == stepNum
                            val isComplete = processingStep > stepNum || processingStep == 0 && isNewCardReady
                            val stepColor = when {
                                isActive -> MaterialTheme.colorScheme.primary
                                isComplete -> Color(0xFF22C55E) // green-500
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            if (isActive) stepColor.copy(alpha = 0.2f)
                                            else if (isComplete) stepColor.copy(alpha = 0.15f)
                                            else Color.Transparent,
                                            CircleShape
                                        )
                                        .border(
                                            1.5.dp,
                                            stepColor,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isComplete) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = stepColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Icon(
                                            stepIcons[index],
                                            contentDescription = null,
                                            tint = stepColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    color = stepColor,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = CenterText
                                )
                            }
                        }
                    }

                    if (processingStepLabel.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { scanProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = processingStepLabel,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (capturedImageUri == null) {
            // INITIAL EMPTY WORKSPACE state: prompt photo picker
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(24.dp),
                        clip = false,
                        ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = "Load object",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(64.dp)
                            .padding(bottom = 16.dp)
                    )

                    Text(
                        text = "LOAD TARGET PHOTO TO EXTRACT STICKER",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "On-device AI background remover isolates subjects, calculates levels, and registers coordinates instantly.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = CenterText,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = onPhotoPick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(6.dp))
                            Text("GALLERY", color = MaterialTheme.colorScheme.onPrimary, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onCameraClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = MaterialTheme.colorScheme.onSecondary)
                            Spacer(Modifier.width(6.dp))
                            Text("CAMERA", color = MaterialTheme.colorScheme.onSecondary, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (isAwaitingSubjectSelection && combinedMaskImage != null) {
            // STEP 1.5: SUBJECT SELECTION UI
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(24.dp),
                        clip = false,
                        ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "원하는 객체를 선택하세요",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Display combined mask image
                    Image(
                        bitmap = combinedMaskImage.asImageBitmap(),
                        contentDescription = "Masked Subjects",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Subject thumbnails row
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(detectedSubjects.size) { index ->
                            val subject = detectedSubjects[index]
                            val maskColor = Color(subject.maskColor)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { onSubjectSelect(index) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(3.dp, maskColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = subject.stickerBitmap.asImageBitmap(),
                                        contentDescription = "Subject $index",
                                        modifier = Modifier.fillMaxSize().padding(4.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "객체 ${index + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // PHOTO HAS BEEN ACQUIRED: Display Scanner Frame
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(24.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (showCardTab && cardImageBitmap != null) {
                    // Show Creature Card Image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(
                                        CyberYellow.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = cardImageBitmap.asImageBitmap(),
                            contentDescription = "Creature Card",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else if (processedBitmap == null) {
                    // Display original background loaded image while scanning runs
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(capturedImageUri)
                            .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                            .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                            .build(),
                        contentDescription = "Target",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Display Extracted Transparent PNG Sticker in front of sci-fi circle
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(
                                        getRarityColor(rarity).copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = processedBitmap.asImageBitmap(),
                            contentDescription = "Segmented stickerResult",
                            modifier = Modifier
                                .size(240.dp)
                                .padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Interactive Sweeper Beam Overlay
                if (isScanning) {
                    val sweepTransition = rememberInfiniteTransition(label = "Radar lines")
                    val positionFactor by sweepTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ), label = "Scanline sweep"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.04f)
                            .align(Alignment.TopCenter)
                            .graphicsLayer(translationY = 310 * positionFactor)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.primary,
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ═══════════════════════════════════════════
            // Sticker / Card Tab Toggle
            // ═══════════════════════════════════════════
            if (isNewCardReady && cardImageBitmap != null) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    val stickerSelected = !showCardTab
                    val cardSelected = showCardTab

                    SegmentedButton(
                        selected = stickerSelected,
                        onClick = { if (!stickerSelected) onToggleCardView() },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = stickerSelected) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    ) {
                        Text(
                            "스티커",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    SegmentedButton(
                        selected = cardSelected,
                        onClick = { if (!cardSelected) onToggleCardView() },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = cardSelected) {
                                Icon(
                                    Icons.Default.Style,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    ) {
                        Text(
                            "도감 카드",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            } else if (isNewCardReady && processingStep == 4) {
                // Card is being generated
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "도감 카드 생성 중...",
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Carousel selector for multiple stickers
            if (isNewCardReady && tempStickers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tempStickers.forEachIndexed { index, sticker ->
                        val isSelected = index == selectedStickerIndex
                        
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(68.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh) // Theme surface
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.5.dp, Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)), RoundedCornerShape(16.dp))
                                    } else {
                                        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                                    }
                                )
                                .graphicsLayer {
                                    scaleX = if (isSelected) 1.08f else 1.0f
                                    scaleY = if (isSelected) 1.08f else 1.0f
                                }
                                .clickable { onStickerSelect(index) }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val dotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val dotSpacing = 8f
                                for (x in 0..size.width.toInt() step dotSpacing.toInt()) {
                                    for (y in 0..size.height.toInt() step dotSpacing.toInt()) {
                                        drawCircle(
                                            color = dotColor,
                                            radius = 0.8f,
                                            center = androidx.compose.ui.geometry.Offset(x.toFloat(), y.toFloat())
                                        )
                                    }
                                }
                            }
                            
                            Image(
                                bitmap = sticker.bitmap.asImageBitmap(),
                                contentDescription = "Sticker $index Thumbnail",
                                modifier = Modifier.size(54.dp),
                                contentScale = ContentScale.Fit
                            )

                            // Card badge indicator
                            if (sticker.cardBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(16.dp)
                                        .background(CyberYellow, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Style,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Finished generating card parameters, user can customize and submit!
            if (isNewCardReady) {
                val customBgColor = getRarityBgColor(rarity)
                val customTextColor = getRarityTextColor(rarity)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(24.dp),
                            clip = false,
                            ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)), RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(customBgColor, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = rarity.uppercase(),
                                    color = customTextColor,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            if (tempStickers.size > 1) {
                                Text(
                                    text = "STICKER ${selectedStickerIndex + 1} OF ${tempStickers.size}",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            } else {
                                Text(
                                    text = "LEVEL $level",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom Name Form Field - Light neutral styling
                        OutlinedTextField(
                            value = title,
                            onValueChange = onTitleChange,
                            label = { Text("IDENTIFY OBJECT SPECIMEN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("title_textfield")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Suggest short names row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            suggestions.forEach { suggest ->
                                androidx.compose.material3.SuggestionChip(
                                    onClick = { onTitleChange(suggest) },
                                    label = { Text(suggest, style = MaterialTheme.typography.labelMedium) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = androidx.compose.material3.SuggestionChipDefaults.suggestionChipBorder(
                                        enabled = true,
                                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                                        borderWidth = 1.dp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Submit Save Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDiscardCurrent,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CrimsonAlert),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonAlert),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("RECYCLE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onConfirmSave,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("save_sticker_btn")
                            ) {
                                Text(
                                    "COMMIT CORE CARD",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
