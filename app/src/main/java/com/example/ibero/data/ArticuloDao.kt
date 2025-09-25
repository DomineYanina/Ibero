package com.example.ibero.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticuloDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articulos: List<Articulo>)

    @Query("SELECT nombre FROM articulos")
    fun getAllArticulos(): Flow<List<String>>

    @Query("DELETE FROM articulos")
    suspend fun deleteAll()
}