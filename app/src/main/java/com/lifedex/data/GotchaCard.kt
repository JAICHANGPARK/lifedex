package com.lifedex.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "gotcha_cards")
data class GotchaCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val imagePath: String, // Absolute path of the cropped PNG
    val cardImagePath: String? = null, // Absolute path of the generated Creature card image
    val latitude: Double,
    val longitude: Double,
    val rarity: String, // "COMMON", "RARE", "EPIC", "LEGENDARY"
    val level: Int,      // Generated gotcha level (e.g. 1 to 100)
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
