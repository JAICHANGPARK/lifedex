package com.lifedex.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lifedex.data.GotchaCard
import com.lifedex.ui.GotchaViewModel
import com.lifedex.ui.MapScreen
import com.lifedex.ui.components.RarityHelpers
import com.lifedex.ui.theme.CardSlate
import com.lifedex.ui.theme.ColorRarityCommon
import com.lifedex.ui.theme.ColorRarityEpic
import com.lifedex.ui.theme.ColorRarityLegendary
import com.lifedex.ui.theme.ColorRarityRare
import com.lifedex.ui.theme.CrimsonAlert
import com.lifedex.ui.theme.NeonCyan
import java.util.Locale

@Composable
fun DexListingScreen(
    viewModel: GotchaViewModel,
    cards: List<GotchaCard>,
    onCardSelect: (GotchaCard) -> Unit,
    onClearAll: () -> Unit
) {
    var selectedRarityFilter by remember { mutableStateOf("ALL") }
    var showConfirmDeleteAll by remember { mutableStateOf(false) }
    var isMapViewActive by remember { mutableStateOf(false) }

    val filteredCards = remember(cards, selectedRarityFilter) {
        if (selectedRarityFilter == "ALL") cards
        else cards.filter { it.rarity.equals(selectedRarityFilter, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Stats bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "COLLECTION SLOTS",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${cards.size} / 50 SPECIES CAPTURED",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Map / Grid View toggle button
                IconButton(
                    onClick = { isMapViewActive = !isMapViewActive },
                    modifier = Modifier.testTag("toggle_map_grid_btn")
                ) {
                    Icon(
                        imageVector = if (isMapViewActive) Icons.Default.GridView else Icons.Default.Map,
                        contentDescription = "Toggle View Mode",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (cards.isNotEmpty()) {
                    IconButton(
                        onClick = { showConfirmDeleteAll = true },
                        modifier = Modifier.testTag("clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Format Registry",
                            tint = CrimsonAlert
                        )
                    }
                }
            }
        }

        // Rarity Filter Pill-Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("ALL", "COMMON", "RARE", "EPIC", "LEGENDARY")
            filters.forEach { filter ->
                val isSelected = selectedRarityFilter == filter
                val bColor = when (filter) {
                    "COMMON" -> ColorRarityCommon
                    "RARE" -> ColorRarityRare
                    "EPIC" -> ColorRarityEpic
                    "LEGENDARY" -> ColorRarityLegendary
                    else -> MaterialTheme.colorScheme.primary
                }

                androidx.compose.material3.FilterChip(
                    selected = isSelected,
                    onClick = { selectedRarityFilter = filter },
                    label = {
                        Text(
                            text = filter,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = bColor,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = if (isSelected) null else androidx.compose.material3.FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        borderWidth = 1.dp
                    )
                )
            }
        }

        // Dynamic Card Grid or Inline Map View
        if (isMapViewActive) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                MapScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            if (filteredCards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "No items",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(72.dp)
                                .padding(bottom = 16.dp)
                        )
                        Text(
                            text = "EMPTY DECK REGISTRY",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedRarityFilter == "ALL")
                                "No lifeform objects captured. Capture live photos to extract stickers!"
                            else "No captured objects found in $selectedRarityFilter rarity.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 260.dp)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 60.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cards_grid")
                ) {
                    items(filteredCards, key = { it.id }) { card ->
                        GotchaDexItemView(card = card, onClick = { onCardSelect(card) })
                    }
                }
            }
        }
    }

    // Confirm Delete Dialog
    if (showConfirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteAll = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    "PURGE ENTIRE STORAGE?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonAlert,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Text(
                    "Warning: This clears all collected digital cards and frees app storage irrevocably.",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        showConfirmDeleteAll = false
                    }
                ) {
                    Text("FORMAT ALL", color = CrimsonAlert, style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteAll = false }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

@Composable
fun GotchaDexItemView(
    card: GotchaCard,
    onClick: () -> Unit
) {
    val rarityColor = RarityHelpers.getRarityColor(card.rarity)
    val customBgColor = RarityHelpers.getRarityBgColor(card.rarity)
    val customTextColor = RarityHelpers.getRarityTextColor(card.rarity)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
            .clickable { onClick() }
            .testTag("card_element_${card.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card visual ID block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(customBgColor, shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = card.rarity.uppercase(),
                        color = customTextColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    )
                }
                Text(
                    text = "LV.${card.level}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sticker Image viewport with beautiful diagonal grid scanning visual backing
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                rarityColor.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Subtle scan decor lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val spacing = 16f
                    for (x in 0..width.toInt() step spacing.toInt()) {
                        for (y in 0..height.toInt() step spacing.toInt()) {
                            drawCircle(
                                color = rarityColor.copy(alpha = 0.15f),
                                radius = 1.3f,
                                center = androidx.compose.ui.geometry.Offset(x.toFloat(), y.toFloat())
                            )
                        }
                    }
                }

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(card.imagePath)
                        .crossfade(true)
                        .build(),
                    contentDescription = card.title,
                    modifier = Modifier
                        .size(92.dp)
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Text Label Box
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = card.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${String.format(Locale.US, "%.3f", card.latitude)}°N ${String.format(Locale.US, "%.3f", card.longitude)}°W",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
