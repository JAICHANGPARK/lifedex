package com.lifedex.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GotchaDao {
    @Query("SELECT * FROM gotcha_cards ORDER BY timestamp DESC")
    fun getAllCards(): Flow<List<GotchaCard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: GotchaCard): Long

    @Query("DELETE FROM gotcha_cards WHERE id = :id")
    suspend fun deleteCardById(id: Int)

    @Query("DELETE FROM gotcha_cards")
    suspend fun clearAllCards()

    @Query("SELECT * FROM gotcha_decks ORDER BY timestamp DESC")
    fun getAllDecks(): Flow<List<GotchaDeck>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: GotchaDeck): Long

    @Query("DELETE FROM gotcha_decks WHERE id = :id")
    suspend fun deleteDeckById(id: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDeckCardCrossRef(ref: DeckCardCrossRef)

    @Query("DELETE FROM deck_card_cross_ref WHERE deckId = :deckId AND cardId = :cardId")
    suspend fun deleteDeckCardCrossRef(deckId: Int, cardId: Int)

    @Query("""
        SELECT gotcha_cards.* FROM gotcha_cards 
        INNER JOIN deck_card_cross_ref ON gotcha_cards.id = deck_card_cross_ref.cardId 
        WHERE deck_card_cross_ref.deckId = :deckId
        ORDER BY gotcha_cards.timestamp DESC
    """)
    fun getCardsForDeck(deckId: Int): Flow<List<GotchaCard>>

    @Query("DELETE FROM deck_card_cross_ref WHERE deckId = :deckId")
    suspend fun deleteCrossRefsForDeck(deckId: Int)

    @Query("DELETE FROM deck_card_cross_ref WHERE cardId = :cardId")
    suspend fun deleteCrossRefsForCard(cardId: Int)
}
