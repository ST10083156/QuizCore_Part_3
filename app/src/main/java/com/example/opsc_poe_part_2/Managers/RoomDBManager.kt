package com.example.opsc_poe_part_2.Managers

import android.content.Context
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "user_scores")
data class UserScore(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val score: Int
)

@Dao
interface UserScoreDao {
    @Insert
    suspend fun insertUser(userScore: UserScore)

    @Query("UPDATE user_scores SET score = score + :newScore WHERE id = :userId")
    suspend fun addScoreToUser(userId: Int, newScore: Int)

    @Query("SELECT * FROM user_scores WHERE id = :userId")
    suspend fun getUserScore(userId: Int): UserScore?
}

@Database(entities = [UserScore::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userScoreDao(): UserScoreDao
}

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    // Function to get the database instance
    fun getDatabase(context: Context): AppDatabase {
        // Return existing instance or create a new one if null
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app_database" // Database name
            ).build()
            INSTANCE = instance
            instance
        }
    }
}

