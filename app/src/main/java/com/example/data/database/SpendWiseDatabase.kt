package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CategoryDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.ExpenseSplitDao
import com.example.data.dao.FriendDao
import com.example.data.dao.FundDao
import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplitEntity
import com.example.data.model.FriendEntity
import com.example.data.model.FundEntity

@Database(
    entities = [
        CategoryEntity::class,
        ExpenseEntity::class,
        ExpenseSplitEntity::class,
        FriendEntity::class,
        FundEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SpendWiseDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseSplitDao(): ExpenseSplitDao
    abstract fun friendDao(): FriendDao
    abstract fun fundDao(): FundDao

    companion object {
        @Volatile
        private var INSTANCE: SpendWiseDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `expense_splits` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `expenseId` TEXT NOT NULL,
                        `personName` TEXT NOT NULL,
                        `allocatedAmount` REAL NOT NULL,
                        `percentage` REAL NOT NULL,
                        `isPaid` INTEGER NOT NULL,
                        `settledTimestamp` INTEGER,
                        FOREIGN KEY(`expenseId`) REFERENCES `expenses`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_expense_splits_expenseId` ON `expense_splits` (`expenseId`)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `friends` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `createdTimestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `funds` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `source` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `notes` TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): SpendWiseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpendWiseDatabase::class.java,
                    "spendwise_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
