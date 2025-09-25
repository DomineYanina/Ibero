package com.example.ibero.repository

import com.example.ibero.data.Inspection
import com.example.ibero.data.InspectionDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class InspectionRepository(private val inspectionDao: InspectionDao) {

    // Obtiene todas las inspecciones como un Flow para observar cambios en tiempo real
    val allInspections: Flow<List<Inspection>> = inspectionDao.getAllInspections()

    // Obtiene las inspecciones no sincronizadas como un Flow
    val unsyncedInspections: Flow<List<Inspection>> = inspectionDao.getUnsyncedInspections()

    /**
     * Inserta una nueva inspección en la base de datos.
     * @param inspection El objeto Inspection a insertar.
     */

    suspend fun getUnsyncedInspectionsOnce(): List<Inspection> {
        return inspectionDao.getUnsyncedInspections().first() // Obtiene el primer valor y cancela la observación del Flow
    }

    suspend fun insert(inspection: Inspection) {
        inspectionDao.insertInspection(inspection)
    }

    /**
     * Actualiza una inspección existente en la base de datos.
     * @param inspection El objeto Inspection a actualizar.
     */
    suspend fun update(inspection: Inspection) {
        inspectionDao.updateInspection(inspection)
    }

    /**
     * Elimina una inspección por su ID.
     * @param inspectionId El ID de la inspección a eliminar.
     */
    suspend fun delete(inspectionId: Long) {
        inspectionDao.deleteInspection(inspectionId)
    }

    /**
     * Elimina todas las inspecciones que ya han sido sincronizadas.
     */
    suspend fun deleteSynced() {
        inspectionDao.deleteSyncedInspections()
    }

}
