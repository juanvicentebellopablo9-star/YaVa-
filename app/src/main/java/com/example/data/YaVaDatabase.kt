package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        OrderEntity::class,
        DriverEntity::class,
        UserEntity::class,
        CompanyConfigEntity::class,
        LegalConsentEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class YaVaDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun driverDao(): DriverDao
    abstract fun userDao(): UserDao
    abstract fun companyConfigDao(): CompanyConfigDao
    abstract fun legalConsentDao(): LegalConsentDao

    companion object {
        @Volatile
        private var INSTANCE: YaVaDatabase? = null

        fun getDatabase(context: Context): YaVaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    YaVaDatabase::class.java,
                    "yava_logistics_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
