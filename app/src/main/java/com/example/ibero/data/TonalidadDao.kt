package com.example.ibero.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TonalidadDao {
    @Insert
    suspend fun insertTonalidad(tonalidad: Tonalidad)

    @Update
    suspend fun updateTonalidad(tonalidad: Tonalidad)

    @Query("SELECT * FROM tonalidades WHERE isSynced = 0")
    fun getUnsyncedTonalidades(): Flow<List<Tonalidad>>

    @Query("SELECT COUNT(*) FROM tonalidades WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int
}