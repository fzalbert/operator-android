package ru.profikrol.operator.data.local.offline

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "task_cache")
data class TaskCacheEntity(
    @PrimaryKey val employeeId: String,
    val tasksJson: String,
    val updatedAt: Long,
)

@Entity(tableName = "shift_cache")
data class ShiftCacheEntity(
    @PrimaryKey val employeeId: String,
    val shiftJson: String,
    val updatedAt: Long,
)

@Entity(tableName = "offline_actions")
data class OfflineActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: String,
    val taskId: String,
    val type: String,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
    val lastError: String? = null,
)

@Dao
interface OfflineDao {
    @Query("SELECT * FROM shift_cache WHERE employeeId = :employeeId")
    suspend fun cachedShift(employeeId: String): ShiftCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveShift(cache: ShiftCacheEntity)

    @Query("SELECT * FROM task_cache WHERE employeeId = :employeeId")
    suspend fun cachedTasks(employeeId: String): TaskCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTasks(cache: TaskCacheEntity)

    @Query("DELETE FROM task_cache WHERE employeeId = :employeeId")
    suspend fun clearTasks(employeeId: String)

    @Query("SELECT * FROM offline_actions WHERE employeeId = :employeeId ORDER BY id")
    suspend fun pendingActions(employeeId: String): List<OfflineActionEntity>

    @Query("SELECT COUNT(*) FROM offline_actions WHERE employeeId = :employeeId")
    suspend fun pendingCount(employeeId: String): Int

    @Insert
    suspend fun enqueue(action: OfflineActionEntity): Long

    @Query("DELETE FROM offline_actions WHERE id = :id")
    suspend fun deleteAction(id: Long)

    @Query("UPDATE offline_actions SET attempts = attempts + 1, lastError = :error WHERE id = :id")
    suspend fun recordFailure(id: Long, error: String)
}

@Database(
    entities = [TaskCacheEntity::class, ShiftCacheEntity::class, OfflineActionEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class OfflineDatabase : RoomDatabase() {
    abstract fun offlineDao(): OfflineDao
}
