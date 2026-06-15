package com.lifedex.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GotchaBottomNavbar(
    activeTab: String,
    onTabChange: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        NavigationBarItem(
            selected = activeTab == "DEX",
            onClick = { onTabChange("DEX") },
            icon = {
                Icon(
                    imageVector = if (activeTab == "DEX") Icons.Filled.GridView else Icons.Outlined.GridView,
                    contentDescription = "Collection Dex"
                )
            },
            label = { Text("Species Dex", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_dex")
        )

        NavigationBarItem(
            selected = activeTab == "CARDS",
            onClick = { onTabChange("CARDS") },
            icon = {
                Icon(
                    imageVector = if (activeTab == "CARDS") Icons.Filled.Style else Icons.Outlined.Style,
                    contentDescription = "Cards Gallery"
                )
            },
            label = { Text("My Cards", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_cards")
        )

        NavigationBarItem(
            selected = activeTab == "DECKS",
            onClick = { onTabChange("DECKS") },
            icon = {
                Icon(
                    imageVector = if (activeTab == "DECKS") Icons.Filled.Layers else Icons.Outlined.Layers,
                    contentDescription = "Card Decks"
                )
            },
            label = { Text("Card Decks", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_decks")
        )

        NavigationBarItem(
            selected = activeTab == "SCAN",
            onClick = { onTabChange("SCAN") },
            icon = {
                Icon(
                    imageVector = if (activeTab == "SCAN") Icons.Filled.QrCodeScanner else Icons.Outlined.QrCodeScanner,
                    contentDescription = "Scanner Screen"
                )
            },
            label = { Text("Gotcha Scan", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_scan")
        )
    }
}
