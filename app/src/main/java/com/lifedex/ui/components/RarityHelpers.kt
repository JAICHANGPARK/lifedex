package com.lifedex.ui.components

import androidx.compose.ui.graphics.Color
import com.lifedex.ui.theme.ColorRarityCommon
import com.lifedex.ui.theme.ColorRarityRare
import com.lifedex.ui.theme.ColorRarityEpic
import com.lifedex.ui.theme.ColorRarityLegendary
import com.lifedex.ui.theme.NeonCyan

object RarityHelpers {
    fun getRarityColor(rarity: String): Color {
        return when (rarity.uppercase()) {
            "COMMON" -> ColorRarityCommon
            "RARE" -> ColorRarityRare
            "EPIC" -> ColorRarityEpic
            "LEGENDARY" -> ColorRarityLegendary
            else -> NeonCyan
        }
    }

    fun getRarityTextColor(rarity: String): Color {
        return when (rarity.uppercase()) {
            "COMMON" -> Color(0xFF475569) // slate-600
            "RARE" -> Color(0xFF1D4ED8)   // blue-700
            "EPIC" -> Color(0xFF6D28D9)   // violet-700
            "LEGENDARY" -> Color(0xFFB45309) // amber-700
            else -> Color(0xFF0F172A)
        }
    }

    fun getRarityBgColor(rarity: String): Color {
        return when (rarity.uppercase()) {
            "COMMON" -> Color(0xFFF1F5F9) // slate-100
            "RARE" -> Color(0xFFDBEAFE)   // blue-100
            "EPIC" -> Color(0xFFEDE9FE)   // violet-100
            "LEGENDARY" -> Color(0xFFFEF3C7) // amber-100
            else -> Color(0xFFF1F5F9)
        }
    }
}
