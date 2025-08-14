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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InspectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: InspectionRepository
    private val connectivityObserver = NetworkConnectivityObserver(application)
    val allInspections: LiveData<List<Inspection>>

    // LiveData para los mensajes de sincronización
    private val _syncMessage = MutableLiveData<String?>()
    val syncMessage: LiveData<String?> get() = _syncMessage

    // LiveData para los registros de la sesión actual
    private val _currentSessionInspections = MutableLiveData<MutableList<Inspection>>(mutableListOf())
    val currentSessionInspections: LiveData<MutableList<Inspection>> get() = _currentSessionInspections

    init {
        val inspectionDao = AppDatabase.getDatabase(application).inspectionDao()
        repository = InspectionRepository(inspectionDao)
        // Se corrige el Type mismatch convirtiendo el Flow a LiveData
        allInspections = repository.allInspections.asLiveData()

        // Iniciar la observación del estado de la red
        viewModelScope.launch {
            connectivityObserver.observe()
            connectivityObserver.connectionStatus.collectLatest { status ->
                if (status is ConnectionStatus.Available) {
                    Log.d("SyncDebug", "Conexión a Internet detectada. Iniciando sincronización de registros pendientes...")
                    performSync()
                }
            }
        }
    }

    /**
     * Inserta una nueva inspección en la base de datos local y la agrega a la lista de la sesión actual.
     */
    fun insertInspection(inspection: Inspection) = viewModelScope.launch {
        // Guarda en la base de datos
        repository.insert(inspection)
        // Agrega a la lista de la sesión actual para mostrarla en la UI
        addInspectionToSessionList(inspection)
    }

    fun updateInspection(inspection: Inspection) = viewModelScope.launch {
        repository.update(inspection)
    }

    fun deleteInspection(inspection: Inspection) = viewModelScope.launch {
        repository.delete(inspection.id)
    }

    // Función para limpiar el mensaje de sincronización
    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    /**
     * Agrega un nuevo registro a la lista de la sesión actual para ser mostrado en el RecyclerView.
     */
    fun addInspectionToSessionList(inspection: Inspection) {
        val currentList = _currentSessionInspections.value ?: mutableListOf()
        currentList.add(0, inspection) // Agrega el nuevo registro al principio
        _currentSessionInspections.value = currentList
    }

    /**
     * Limpia la lista de registros de la sesión actual.
     */
    fun clearCurrentSessionList() {
        _currentSessionInspections.value = mutableListOf()
    }

    /**
     * Sincroniza las inspecciones no enviadas con Google Sheets usando Apps Script.
     */
    fun performSync() {
        viewModelScope.launch(Dispatchers.IO) {
            // Verificar la conexión a Internet antes de intentar la sincronización
            if (!connectivityObserver.hasInternet()) {
                withContext(Dispatchers.Main) {
                    _syncMessage.value = "No hay conexión a Internet. Los registros se guardan localmente."
                }
                return@launch
            }

            // Obtiene las inspecciones no sincronizadas
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
                        fecha = inspection.fecha.time.toString(),
                        hojaDeRuta = inspection.hojaDeRuta,
                        tejeduria = inspection.tejeduria,
                        telar = inspection.telar,
                        tintoreria = inspection.tintoreria,
                        articulo = inspection.articulo,
                        color = inspection.color,
                        rolloDeUrdido = inspection.rolloDeUrdido,
                        orden = inspection.orden,
                        cadena = inspection.cadena,
                        anchoDeRollo = inspection.anchoDeRollo,
                        esmerilado = inspection.esmerilado,
                        ignifugo = inspection.ignifugo,
                        impermeable = inspection.impermeable,
                        otro = inspection.otro,
                        tipoCalidad = inspection.tipoCalidad,
                        tipoDeFalla = inspection.tipoDeFalla,
                        metrosDeTela = inspection.metrosDeTela,
                        imageUrls = inspection.imageUrls.joinToString(",")
                    )

                    if (response.status == "SUCCESS") {
                        val updatedInspection = inspection.copy(isSynced = true, imageUrls = emptyList())
                        repository.update(updatedInspection)
                        successfulSyncs++
                    } else {
                        withContext(Dispatchers.Main) {
                            _syncMessage.value = "Error al subir ${inspection.articulo}: ${response.message}"
                        }
                        failedSyncs++
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _syncMessage.value = "Error de conexión. Se guardó localmente. ${e.message}"
                    }
                    Log.e("SyncDebug", "Error al sincronizar datos para ${inspection.articulo}", e)
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