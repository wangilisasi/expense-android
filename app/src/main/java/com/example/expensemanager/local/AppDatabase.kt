package com.example.expensemanager.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expensemanager.local.daos.ExpenseDao
import com.example.expensemanager.local.daos.ExpenseTrackerDao
import com.example.expensemanager.local.daos.UserDao
import com.example.expensemanager.local.entities.ExpenseEntity
import com.example.expensemanager.local.entities.ExpenseTrackerEntity
import com.example.expensemanager.local.entities.UserEntity

@Database(
    entities = [ExpenseEntity::class, ExpenseTrackerEntity::class, UserEntity::class],
    version = 4
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseTrackerDao(): ExpenseTrackerDao
    abstract fun userDao(): UserDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expense_trackers ADD COLUMN serverId TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN occurredAt TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    UPDATE expenses
                    SET occurredAt = CASE
                        WHEN createdAt IS NOT NULL AND createdAt != '' THEN createdAt
                        ELSE date
                    END
                    WHERE occurredAt = ''
                    """
                )
            }
        }
    }
}
