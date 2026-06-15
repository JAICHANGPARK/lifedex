package com.lifedex.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [GotchaCard::class, GotchaDeck::class, DeckCardCrossRef::class], version = 3, exportSchema = false)
abstract class GotchaDatabase : RoomDatabase() {
    abstract fun gotchaDao(): GotchaDao

    companion object {
        @Volatile
        private var INSTANCE: GotchaDatabase? = null

        fun getDatabase(context: Context): GotchaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GotchaDatabase::class.java,
                    "gotcha_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
