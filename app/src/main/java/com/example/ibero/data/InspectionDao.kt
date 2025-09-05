package com.example.ibero.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.ibero.data.Inspection
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionDao {
    // Inserta una nueva inspección en la base de datos
    @Insert
    suspend fun insertInspection(inspection: Inspection)

    // Actualiza una inspección existente en la base de datos
    @Update
    suspend fun updateInspection(inspection: Inspection)

    // Obtiene todas las inspecciones de la base de datos
    // Flow permite observar cambios en tiempo real en la base de datos
    @Query("SELECT * FROM inspections ORDER BY fecha DESC, hojaDeRuta DESC")
    fun getAllInspections(): Flow<List<Inspection>>

    // Obtiene todas las inspecciones que no han sido sincronizadas
    @Query("SELECT * FROM inspections WHERE isSynced = 0")
    fun getUnsyncedInspections(): Flow<List<Inspection>>

    // Elimina una inspección por su ID
    @Query("DELETE FROM inspections WHERE id = :inspectionId")
    suspend fun deleteInspection(inspectionId: Long)

    // Elimina todas las inspecciones sincronizadas (opcional, para limpiar la DB local)
    @Query("DELETE FROM inspections WHERE isSynced = 1")
    suspend fun deleteSyncedInspections()

    @Query("SELECT COUNT(*) FROM inspections WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int
}
