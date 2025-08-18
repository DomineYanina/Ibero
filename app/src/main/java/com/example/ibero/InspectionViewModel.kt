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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class InspectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: InspectionRepository
    private val connectivityObserver = NetworkConnectivityObserver(application)
    val allInspections: LiveData<List<Inspection>>

    private val _syncMessage = MutableLiveData<String?>()
    val syncMessage: LiveData<String?> get() = _syncMessage

    private val _currentSessionInspections = MutableLiveData<MutableList<Inspection>>(mutableListOf())
    val currentSessionInspections: LiveData<MutableList<Inspection>> get() = _currentSessionInspections

    // Mutex para asegurar que solo una sincronización se ejecute a la vez
    private val syncMutex = Mutex()

    init {
        val inspectionDao = AppDatabase.getDatabase(application).inspectionDao()
        repository = InspectionRepository(inspectionDao)
        allInspections = repository.allInspections.asLiveData()

        // 1. Observar el estado de la red para iniciar sincronización
        viewModelScope.launch {
            connectivityObserver.observe()
            connectivityObserver.connectionStatus.collectLatest { status ->
                if (status is ConnectionStatus.Available) {
                    Log.d("SyncDebug", "Conexión a Internet detectada. Iniciando sincronización de registros pendientes...")
                    performSync() // Llama a performSync si hay conexión
                }
            }
        }

        // 2. Observar la lista de inspecciones no sincronizadas para iniciar sincronización
        // Usamos distinctUntilChanged para evitar procesar la misma lista si no hay cambios.
        // Mapeamos a un valor unitario para solo reaccionar a la existencia de elementos.
        viewModelScope.launch {
            repository.unsyncedInspections
                .map { it.isNotEmpty() } // Mapear a true si hay elementos, false si no
                .distinctUntilChanged()  // Solo emitir si el estado de tener pendientes cambia (de true a false o viceversa)
                .collect { hasUnsynced ->
                    if (hasUnsynced) {
                        Log.d("SyncDebug", "Nuevas inspecciones pendientes detectadas. Iniciando sincronización...")
                        performSync() // Llama a performSync si hay pendientes
                    }
                }
        }
    }

    /**
     * Inserta una nueva inspección en la base de datos local y la agrega a la lista de la sesión actual.
     * Ya no llama a performSync() directamente aquí.
     */
    fun insertInspection(inspection: Inspection) = viewModelScope.launch {
        repository.insert(inspection)
        addInspectionToSessionList(inspection)
        // La sincronización se disparará automáticamente por la observación del Flow 'unsyncedInspections'
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

    fun addInspectionToSessionList(inspection: Inspection) {
        val currentList = _currentSessionInspections.value ?: mutableListOf()
        currentList.add(0, inspection)
        _currentSessionInspections.value = currentList
    }

    fun clearCurrentSessionList() {
        _currentSessionInspections.value = mutableListOf()
    }

    /**
     * Sincroniza las inspecciones no enviadas con Google Sheets usando Apps Script.
     * Asegura que solo una sincronización se ejecute a la vez.
     */
    fun performSync() {
        viewModelScope.launch(Dispatchers.IO) {
            // Adquiere el bloqueo del mutex para asegurar exclusividad
            syncMutex.withLock {
                // Verificar la conexión a Internet antes de intentar la sincronización
                if (!connectivityObserver.hasInternet()) {
                    withContext(Dispatchers.Main) {
                        _syncMessage.value = "No hay conexión a Internet. Los registros se guardan localmente."
                    }
                    return@withLock // Sale si no hay conexión
                }

                // Obtener las inspecciones pendientes directamente de la base de datos UNA VEZ
                // En lugar de recolectar un Flow, obtenemos la lista actual.
                val unsyncedInspections = repository.getUnsyncedInspectionsOnce()

                Log.d("SyncDebug", "Iniciando sincronización. Inspecciones pendientes: ${unsyncedInspections.size}")

                if (unsyncedInspections.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _syncMessage.value = "No hay inspecciones pendientes de sincronizar."
                    }
                    return@withLock // Sale si no hay pendientes
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
                            // Actualizar el estado de sincronización.
                            // Esto se hace en el mismo hilo IO ya que update es suspend fun.
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
                            _syncMessage.value = "Error de conexión o API. Se guardó localmente. ${e.message}"
                        }
                        Log.e("SyncDebug", "Error al sincronizar datos para ${inspection.articulo}", e)
                        failedSyncs++
                    }
                }

                withContext(Dispatchers.Main) {
                    _syncMessage.value = "Sincronización completada: $successfulSyncs exitosas, $failedSyncs fallidas."
                }
            } // El bloqueo del mutex se libera automáticamente aquí
        }
    }
}