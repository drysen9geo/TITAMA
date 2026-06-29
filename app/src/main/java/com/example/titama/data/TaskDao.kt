package com.example.titama.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM task_entries ORDER BY endTime DESC")
    fun getAllEntries(): Flow<List<TaskEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TaskEntry)

    @Update
    suspend fun updateEntry(entry: TaskEntry)

    @Delete
    suspend fun deleteEntry(entry: TaskEntry)

    @Query("SELECT * FROM quick_tasks")
    fun getAllQuickTasks(): Flow<List<QuickTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuickTask(task: QuickTask)

    @Delete
    suspend fun deleteQuickTask(task: QuickTask)
    
    @Query("DELETE FROM quick_tasks WHERE id = :id")
    suspend fun deleteQuickTaskById(id: String)

    @Query("SELECT * FROM user_settings WHERE id = 0")
    fun getSettings(): Flow<UserSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSettings(settings: UserSettings)
}
