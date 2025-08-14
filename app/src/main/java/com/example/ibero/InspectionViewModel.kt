package com.example.ibero

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.ibero.data.AppDatabase
import com.example.ibero.data.Inspection
import com.example.ibero.data.network.ApiResponse
import com.example.ibero.data.network.GoogleSheetsApi
import com.example.ibero.repository.InspectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InspectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: InspectionRepository
    val allInspections: LiveData<List<Inspection>>

    private val _syncMessage = MutableLiveData<String?>()
    val syncMessage: MutableLiveData<String?> get() = _syncMessage


    private val _isOnline = MutableLiveData<Boolean>()
    val isOnline: LiveData<Boolean> get() = _isOnline

    fun updateNetworkStatus(isAvailable: Boolean) {
        _isOnline.value = isAvailable
    }

    init {
        val inspectionDao = AppDatabase.getDatabase(application).inspectionDao()
        repository = InspectionRepository(inspectionDao)
        // Convierte el Flow a LiveData para usarlo en el ViewModel
        allInspections = repository.allInspections.asLiveData()
    }

    fun insertInspection(inspection: Inspection) = viewModelScope.launch {
        repository.insert(inspection)
    }

    fun updateInspection(inspection: Inspection) = viewModelScope.launch {
        repository.update(inspection)
    }

    fun deleteInspection(inspection: Inspection) = viewModelScope.launch {
        repository.delete(inspection.id)
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    /**
     * Sincroniza las inspecciones no enviadas con Google Sheets usando Apps Script.
     */
    fun performSync() {
        viewModelScope.launch(Dispatchers.IO) {
            val unsyncedInspections = repository.unsyncedInspections.firstOrNull() ?: emptyList()

            Log.d("SyncDebug", "Iniciando sincronización. Inspecciones pendientes: ${unsyncedInspections.size}")

            if (unsyncedInspections.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _syncMessage.value = "No hay inspecciones pendientes de sincronizar."
                }
                return@launch
            }

            var successfulSyncs = 0
            var failedSyncs = 0

            for (inspection in unsyncedInspections) {
                try {
                    val response: ApiResponse = GoogleSheetsApi.service.addInspection(
                        action = "addInspection",
                        uniqueId = inspection.uniqueId,
                        usuario = inspection.usuario,
                        fecha = inspection.fecha.time.toString(), // timestamp en ms
                        hojaDeRuta = inspection.hojaDeRuta,
                        tejeduria = inspection.tejeduria,
                        telar = inspection.telar,
                        tintoreria = inspection.tintoreria,
                        articulo = inspection.articulo,
                        tipoCalidad = inspection.tipoCalidad,
                        tipoDeFalla = inspection.tipoDeFalla, // puede ser null
                        anchoDeRollo = inspection.anchoDeRollo,
                        imageUrls = inspection.imageUrls.joinToString(",")
                    )

                    if (response.status == "SUCCESS") {
                        // Crea una copia de la inspección con isSynced = true
                        val updatedInspection = inspection.copy(isSynced = true, imageUrls = emptyList())
                        repository.update(updatedInspection)
                        successfulSyncs++
                    } else {
                        withContext(Dispatchers.Main) {
                            _syncMessage.value = "Error para ${inspection.articulo}: ${response.message}"
                        }
                        failedSyncs++
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _syncMessage.value = "Error de red para ${inspection.articulo}: ${e.message}"
                    }
                    failedSyncs++
                }
            }

            withContext(Dispatchers.Main) {
                _syncMessage.value =
                    "Sincronización completada: $successfulSyncs exitosas, $failedSyncs fallidas."
            }
        }
    }
}