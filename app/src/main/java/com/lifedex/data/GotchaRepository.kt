package com.lifedex.data

import kotlinx.coroutines.flow.Flow

class GotchaRepository(private val gotchaDao: GotchaDao) {
    val allCards: Flow<List<GotchaCard>> = gotchaDao.getAllCards()

    suspend fun insertCard(card: GotchaCard): Long {
        return gotchaDao.insertCard(card)
    }

    suspend fun deleteCard(id: Int) {
        gotchaDao.deleteCardById(id)
    }

    suspend fun clearAll() {
        gotchaDao.clearAllCards()
    }
}
