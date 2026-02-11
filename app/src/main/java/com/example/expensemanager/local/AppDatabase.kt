package com.example.expensemanager.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.expensemanager.local.daos.ExpenseDao
import com.example.expensemanager.local.daos.ExpenseTrackerDao
import com.example.expensemanager.local.daos.UserDao
import com.example.expensemanager.local.entities.ExpenseEntity
import com.example.expensemanager.local.entities.ExpenseTrackerEntity
import com.example.expensemanager.local.entities.UserEntity

@Database(entities = [ExpenseEntity::class,ExpenseTrackerEntity::class,UserEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseTrackerDao(): ExpenseTrackerDao
    abstract fun userDao(): UserDao
}