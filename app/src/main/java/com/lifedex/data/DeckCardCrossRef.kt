package com.lifedex.data

import androidx.room.Entity

@Entity(tableName = "deck_card_cross_ref", primaryKeys = ["deckId", "cardId"])
data class DeckCardCrossRef(
    val deckId: Int,
    val cardId: Int
)
