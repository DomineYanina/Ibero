package com.example.ibero

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.ibero.data.AppDatabase
import com.example.ibero.data.Inspection
import com.example.ibero.data.network.AddInspectionResponse // AÑADIDO: Nuevo modelo de respuesta
import com.example.ibero.data.network.GoogleSheetsApi
import com.example.ibero.repository.InspectionRepository
import kotlinx.coroutines.Dispatchers
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

    // LiveData para el estado de carga
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    // Mutex para asegurar que solo una sincronización se ejecute a la vez
    private val syncMutex = Mutex()

    init {
        val inspectionDao = AppDatabase.getDatabase(application).inspectionDao()
        repository = InspectionRepository(inspectionDao)
        allInspections = repository.allInspections.asLiveData()

        // Observar el estado de la red para iniciar sincronización
        viewModelScope.launch {
            connectivityObserver.observe()
            connectivityObserver.connectionStatus.collectLatest { status ->
                if (status is ConnectionStatus.Available) {
                    Log.d("SyncDebug", "Conexión a Internet detectada. Iniciando sincronización de registros pendientes...")
                    performSync()
                }
            }
        }

        // Observar la lista de inspecciones no sincronizadas para iniciar sincronización
        viewModelScope.launch {
            repository.unsyncedInspections
                .map { it.isNotEmpty() }
                .distinctUntilChanged()
                .collectLatest { hasUnsynced ->
                    if (hasUnsynced) {
                        Log.d("SyncDebug", "Nuevas inspecciones pendientes detectadas. Iniciando sincronización...")
                        performSync()
                    }
                }
        }
    }

    fun insertInspection(inspection: Inspection) = viewModelScope.launch {
        repository.insert(inspection)
        addInspectionToSessionList(inspection)
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
     * Sincroniza las inspecciones no enviadas con Google Sheets.
     * Esta función solo se encarga de reintentar las subidas fallidas.
     */
    fun performSync() {
        viewModelScope.launch(Dispatchers.IO) {
            // Establecer el estado de carga al iniciar
            withContext(Dispatchers.Main) {
                _isLoading.value = true
            }

            syncMutex.withLock {
                if (!connectivityObserver.hasInternet()) {
                    withContext(Dispatchers.Main) {
                        _syncMessage.value = "No hay conexión a Internet. Los registros se guardan localmente."
                    }
                    return@withLock
                }

                val unsyncedInspections = repository.getUnsyncedInspectionsOnce()

                if (unsyncedInspections.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _syncMessage.value = "No hay inspecciones pendientes de sincronizar."
                    }
                    // Al final de la función, restablecer el estado de carga a false
                    withContext(Dispatchers.Main) {
                        _isLoading.value = false
                    }
                    return@withLock
                }

                Log.d("SyncDebug", "Iniciando sincronización. Inspecciones pendientes: ${unsyncedInspections.size}")

                var successfulSyncs = 0
                var failedSyncs = 0

                for (inspection in unsyncedInspections) {
                    try {
                        val response: AddInspectionResponse = GoogleSheetsApi.service.addInspection(
                            action = "addInspection",
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

                        if (response.status == "SUCCESS" && response.data.uniqueId != null) {
                            val newUniqueId = response.data.uniqueId
                            // Crear un nuevo objeto con el ID del servidor y la bandera de sincronización
                            val updatedInspection = inspection.copy(uniqueId = newUniqueId, isSynced = true, imageUrls = emptyList())
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
            }
            // Al final de la función, restablecer el estado de carga a false
            withContext(Dispatchers.Main) {
                _isLoading.value = false
            }
        }
    }

    /**
     * Inserta una inspección y la sube inmediatamente a Google Sheets.
     * Si la subida es exitosa, actualiza el registro localmente.
     * Si falla, simplemente lo guarda localmente para su futura sincronización.
     */
    suspend fun finalizeAndSync(inspection: Inspection): Boolean {
        withContext(Dispatchers.Main) {
            _isLoading.value = true
        }

        try {
            val response: AddInspectionResponse = GoogleSheetsApi.service.addInspection(
                action = "addInspection",
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

            if (response.status == "SUCCESS" && response.data.uniqueId != null) {
                val newUniqueId = response.data.uniqueId
                // Se actualiza el registro localmente con el ID y el estado de sincronización.
                val updatedInspection = inspection.copy(uniqueId = newUniqueId, isSynced = true, imageUrls = emptyList())
                repository.insert(updatedInspection) // Reemplaza la inserción original
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
                return true // Éxito
            } else {
                // Si falla la API, el registro se mantiene en la base de datos local
                // con isSynced = false para que se reintente en el próximo performSync.
                repository.insert(inspection)
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
                return false // Fallo
            }
        } catch (e: Exception) {
            // Si hay un error de conexión, se inserta el registro en la base de datos local
            repository.insert(inspection)
            withContext(Dispatchers.Main) {
                _isLoading.value = false
            }
            Log.e("ViewModel", "Error al finalizar y sincronizar", e)
            return false // Fallo
        }
    }
}