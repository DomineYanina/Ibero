package com.example.ibero.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HojaDeRutaDao {
    @Query("SELECT * FROM hojas_de_ruta ORDER BY nombre ASC")
    fun getAllHojasDeRuta(): Flow<List<HojaDeRuta>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(hojasDeRuta: List<HojaDeRuta>)

    @Query("DELETE FROM hojas_de_ruta")
    suspend fun deleteAll()

    @Query("SELECT EXISTS(SELECT 1 FROM hojas_de_ruta WHERE nombre = :nombre)")
    suspend fun exists(nombre: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hojaDeRuta: HojaDeRuta)
}