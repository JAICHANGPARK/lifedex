package com.lifedex.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lifedex.data.GotchaCard
import com.lifedex.data.GotchaDeck
import com.lifedex.ui.GotchaViewModel
import com.lifedex.ui.components.RarityHelpers
import com.lifedex.ui.theme.NeonCyan
import com.lifedex.ui.theme.NeonMagenta
import com.lifedex.ui.theme.CrimsonAlert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecksScreen(
    viewModel: GotchaViewModel,
    decks: List<GotchaDeck>,
    allCards: List<GotchaCard>,
    onCardSelect: (GotchaCard) -> Unit
) {
    val context = LocalContext.current
    var selectedDeck by remember { mutableStateOf<GotchaDeck?>(null) }
    var showCreateDeckDialog by remember { mutableStateOf(false) }

    if (selectedDeck != null) {
        val deck = selectedDeck!!
        // Fetch cards inside this specific deck
        val deckCardsStateFlow = remember(deck.id) { viewModel.getCardsForDeck(deck.id) }
        val cardsInDeck by deckCardsStateFlow.collectAsStateWithLifecycle()
        var showAddCardsDialog by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { selectedDeck = null },
                    modifier = Modifier.testTag("deck_detail_back")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Decks List",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DECK DETAILS",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = deck.name.uppercase(),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(
                        onClick = { showAddCardsDialog = true },
                        modifier = Modifier.testTag("deck_add_cards")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Cards to Deck",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.deleteDeck(deck.id)
                            Toast.makeText(context, "Deck deleted!", Toast.LENGTH_SHORT).show()
                            selectedDeck = null
                        },
                        modifier = Modifier.testTag("deck_delete")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Deck",
                            tint = CrimsonAlert
                        )
                    }
                }
            }

            if (deck.description.isNotBlank()) {
                Text(
                    text = deck.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )
            }

            if (cardsInDeck.isEmpty()) {
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
                            imageVector = Icons.Default.Style,
                            contentDescription = "No cards in deck",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(72.dp)
                                .padding(bottom = 16.dp)
                        )
                        Text(
                            text = "EMPTY DECK",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the '+' icon above to add your saved cards to this deck.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 240.dp)
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
                        .testTag("deck_cards_grid")
                ) {
                    items(cardsInDeck, key = { it.id }) { card ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // Render standard card view
                            com.lifedex.ui.screens.GotchaGalleryCardView(
                                card = card,
                                onClick = { onCardSelect(card) }
                            )

                            // Overlay button to remove card from deck
                            IconButton(
                                onClick = {
                                    viewModel.removeCardFromDeck(deck.id, card.id)
                                    Toast.makeText(context, "Removed from deck!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .size(28.dp)
                                    .testTag("remove_card_${card.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RemoveCircleOutline,
                                    contentDescription = "Remove from deck",
                                    tint = CrimsonAlert,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Dialog to add multiple cards to deck
            if (showAddCardsDialog) {
                var selectedCardsToLink by remember { mutableStateOf(setOf<Int>()) }
                val cardsNotInDeck = remember(allCards, cardsInDeck) {
                    val inDeckIds = cardsInDeck.map { it.id }.toSet()
                    allCards.filter { it.id !in inDeckIds }
                }

                AlertDialog(
                    onDismissRequest = { showAddCardsDialog = false },
                    title = {
                        Text(
                            text = "SELECT CARDS TO ADD",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        if (cardsNotInDeck.isEmpty()) {
                            Text(
                                text = "All your captured cards are already added to this deck, or you haven't captured any cards yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(cardsNotInDeck) { card ->
                                    val isSelected = card.id in selectedCardsToLink
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(
                                                    alpha = 0.4f
                                                )
                                                else Color.Transparent
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outlineVariant,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                selectedCardsToLink = if (isSelected) {
                                                    selectedCardsToLink - card.id
                                                } else {
                                                    selectedCardsToLink + card.id
                                                }
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(card.imagePath)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = card.title,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surface),
                                            contentScale = ContentScale.Fit
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = card.title,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "LV.${card.level} • ${card.rarity}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = RarityHelpers.getRarityColor(card.rarity)
                                            )
                                        }

                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                selectedCardsToLink = if (checked == true) {
                                                    selectedCardsToLink + card.id
                                                } else {
                                                    selectedCardsToLink - card.id
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (selectedCardsToLink.isNotEmpty()) {
                                    viewModel.addCardsToDeck(deck.id, selectedCardsToLink.toList())
                                    Toast.makeText(context, "Cards added to deck!", Toast.LENGTH_SHORT).show()
                                }
                                showAddCardsDialog = false
                            },
                            enabled = selectedCardsToLink.isNotEmpty()
                        ) {
                            Text("ADD", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddCardsDialog = false }) {
                            Text("CANCEL")
                        }
                    }
                )
            }
        }
    } else {
        // List of all decks
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DECK REGISTRY",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "MY CARD DECKS",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }

                Button(
                    onClick = { showCreateDeckDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_create_deck")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CREATE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }

            if (decks.isEmpty()) {
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
                            imageVector = Icons.Outlined.Style,
                            contentDescription = "No decks",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(72.dp)
                                .padding(bottom = 16.dp)
                        )
                        Text(
                            text = "NO DECKS FOUND",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Create a custom deck and add your saved creature cards to represent different themes or categories!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 280.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 60.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("decks_list")
                ) {
                    items(decks, key = { it.id }) { deck ->
                        // Gather cards in this deck to show size
                        val deckCardsFlow = remember(deck.id) { viewModel.getCardsForDeck(deck.id) }
                        val cardsInThisDeck by deckCardsFlow.collectAsStateWithLifecycle()

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDeck = deck }
                                .testTag("deck_item_${deck.id}"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Deck Icon Visual Decor
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            Brush.linearGradient(listOf(NeonCyan, NeonMagenta)),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Style,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = deck.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (deck.description.isNotBlank()) {
                                        Text(
                                            text = deck.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${cardsInThisDeck.size} CARDS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Create Deck Dialog
            if (showCreateDeckDialog) {
                var nameText by remember { mutableStateOf("") }
                var descText by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showCreateDeckDialog = false },
                    title = {
                        Text(
                            text = "CREATE NEW DECK",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = nameText,
                                onValueChange = { nameText = it },
                                label = { Text("Deck Name") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_deck_name"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = descText,
                                onValueChange = { descText = it },
                                label = { Text("Description (Optional)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_deck_desc"),
                                maxLines = 3,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (nameText.isNotBlank()) {
                                    viewModel.createDeck(nameText.trim(), descText.trim())
                                    Toast.makeText(context, "Deck created!", Toast.LENGTH_SHORT).show()
                                    showCreateDeckDialog = false
                                }
                            },
                            enabled = nameText.isNotBlank()
                        ) {
                            Text("CREATE", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateDeckDialog = false }) {
                            Text("CANCEL")
                        }
                    }
                )
            }
        }
    }
}
