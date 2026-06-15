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
}
