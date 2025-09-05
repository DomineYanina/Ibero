package com.example.ibero.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TelarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(telares: List<Telar>)

    @Query("SELECT numero FROM telares")
    fun getAllTelares(): Flow<List<Int>>

    @Query("DELETE FROM telares")
    suspend fun deleteAll()
}