package com.example.noisewatch.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentEntity): Long

    @Query("SELECT * FROM incidents WHERE id = :id")
    fun getIncidentById(id: Long): Flow<IncidentEntity?>

    @Query("SELECT * FROM incidents WHERE id = :id")
    suspend fun getIncidentByIdSync(id: Long): IncidentEntity?

    @Query("SELECT * FROM incidents ORDER BY createdAt DESC")
    fun getAllIncidents(): Flow<List<IncidentEntity>>

    @Query("DELETE FROM incidents")
    suspend fun deleteAllIncidents()

    @Query("DELETE FROM incidents WHERE id = :id")
    suspend fun deleteIncidentById(id: Long)
}
