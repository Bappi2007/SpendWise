package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.CategoryDao
import com.example.data.dao.ExpenseDao
import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseEntity

@Database(
    entities = [CategoryEntity::class, ExpenseEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SpendWiseDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: SpendWiseDatabase? = null

        fun getDatabase(context: Context): SpendWiseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpendWiseDatabase::class.java,
                    "spendwise_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
