package com.example.ibero.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TejeduriaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tejedurias: List<Tejeduria>)

    @Query("SELECT nombre FROM tejedurias")
    fun getAllTejedurias(): Flow<List<String>>

    @Query("DELETE FROM tejedurias")
    suspend fun deleteAll()
}