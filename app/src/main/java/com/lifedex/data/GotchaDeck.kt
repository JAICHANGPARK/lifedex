package com.lifedex.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "gotcha_decks")
data class GotchaDeck(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
