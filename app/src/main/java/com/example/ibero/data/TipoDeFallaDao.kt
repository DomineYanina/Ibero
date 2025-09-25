package com.example.ibero.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TipoDeFallaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tiposDeFalla: List<TipoDeFalla>)

    @Query("SELECT nombre FROM tipos_de_falla")
    fun getAllTiposDeFallas(): Flow<List<String>>

    @Query("DELETE FROM tipos_de_falla")
    suspend fun deleteAll()
}