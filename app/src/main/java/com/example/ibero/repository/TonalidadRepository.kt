package com.example.ibero.repository

import com.example.ibero.data.Tonalidad
import com.example.ibero.data.TonalidadDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TonalidadRepository(private val tonalidadDao: TonalidadDao) {

    val unsyncedTonalidades: Flow<List<Tonalidad>> = tonalidadDao.getUnsyncedTonalidades()

    suspend fun getUnsyncedTonalidadesOnce(): List<Tonalidad> {
        return tonalidadDao.getUnsyncedTonalidades().first()
    }

    suspend fun insert(tonalidad: Tonalidad) {
        tonalidadDao.insertTonalidad(tonalidad)
    }

    suspend fun update(tonalidad: Tonalidad) {
        tonalidadDao.updateTonalidad(tonalidad)
    }
}