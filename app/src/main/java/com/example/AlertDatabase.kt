package com.example

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "alert_thresholds")
data class AlertThreshold(
    @PrimaryKey val id: String, // e.g., "battery_level", "cpu_temp", "avail_ram"
    val metricName: String,     // e.g., "Battery Percentage", "CPU Temperature", "Available Memory"
    val isEnabled: Boolean = false,
    val thresholdValue: Float = 0f,
    val comparisonType: String = "LESS_THAN", // either "LESS_THAN" or "GREATER_THAN"
    val unit: String = ""       // e.g., "%", "°C", "GB"
)

@Dao
interface AlertThresholdDao {
    @Query("SELECT * FROM alert_thresholds")
    fun getAllThresholdsFlow(): Flow<List<AlertThreshold>>

    @Query("SELECT * FROM alert_thresholds")
    suspend fun getAllThresholds(): List<AlertThreshold>

    @Query("SELECT * FROM alert_thresholds WHERE id = :id LIMIT 1")
    suspend fun getThresholdById(id: String): AlertThreshold?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(threshold: AlertThreshold)

    @Update
    suspend fun update(threshold: AlertThreshold)

    @Delete
    suspend fun delete(threshold: AlertThreshold)
}

@Database(entities = [AlertThreshold::class], version = 1, exportSchema = false)
abstract class AlertRoomDatabase : RoomDatabase() {
    abstract fun alertThresholdDao(): AlertThresholdDao

    companion object {
        @Volatile
        private var INSTANCE: AlertRoomDatabase? = null

        fun getDatabase(context: Context): AlertRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlertRoomDatabase::class.java,
                    "alert_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class AlertRepository(private val alertThresholdDao: AlertThresholdDao) {
    val allThresholds: Flow<List<AlertThreshold>> = alertThresholdDao.getAllThresholdsFlow()

    suspend fun getAllThresholdsList(): List<AlertThreshold> = alertThresholdDao.getAllThresholds()

    suspend fun getThresholdById(id: String): AlertThreshold? = alertThresholdDao.getThresholdById(id)

    suspend fun saveThreshold(threshold: AlertThreshold) {
        alertThresholdDao.insertOrUpdate(threshold)
    }
}
