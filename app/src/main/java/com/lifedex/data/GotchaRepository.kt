package com.lifedex.data

import kotlinx.coroutines.flow.Flow

class GotchaRepository(private val gotchaDao: GotchaDao) {
    val allCards: Flow<List<GotchaCard>> = gotchaDao.getAllCards()

    suspend fun insertCard(card: GotchaCard): Long {
        return gotchaDao.insertCard(card)
    }

    suspend fun deleteCard(id: Int) {
        gotchaDao.deleteCrossRefsForCard(id)
        gotchaDao.deleteCardById(id)
    }

    suspend fun clearAll() {
        gotchaDao.clearAllCards()
    }

    val allDecks: Flow<List<GotchaDeck>> = gotchaDao.getAllDecks()

    fun getCardsForDeck(deckId: Int): Flow<List<GotchaCard>> {
        return gotchaDao.getCardsForDeck(deckId)
    }

    suspend fun insertDeck(deck: GotchaDeck): Long {
        return gotchaDao.insertDeck(deck)
    }

    suspend fun deleteDeck(deckId: Int) {
        gotchaDao.deleteCrossRefsForDeck(deckId)
        gotchaDao.deleteDeckById(deckId)
    }

    suspend fun addCardToDeck(deckId: Int, cardId: Int) {
        gotchaDao.insertDeckCardCrossRef(DeckCardCrossRef(deckId, cardId))
    }

    suspend fun removeCardFromDeck(deckId: Int, cardId: Int) {
        gotchaDao.deleteDeckCardCrossRef(deckId, cardId)
    }
}
