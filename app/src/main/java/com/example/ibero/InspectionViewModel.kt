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
                    performSync()
                }
            }
        }

        // 2. Observar la lista de inspecciones no sincronizadas para iniciar sincronización
        viewModelScope.launch {
            repository.unsyncedInspections
                .map { it.isNotEmpty() }
                .distinctUntilChanged()
                .collect { hasUnsynced ->
                    if (hasUnsynced) {
                        Log.d("SyncDebug", "Nuevas inspecciones pendientes detectadas. Iniciando sincronización...")
                        performSync()
                    }
                }
        }
    }

    /**
     * Inserta una nueva inspección en la base de datos local y la agrega a la lista de la sesión actual.
     */
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
     * Sincroniza las inspecciones no enviadas con Google Sheets usando Apps Script.
     * Asegura que solo una sincronización se ejecute a la vez.
     */
    fun performSync() {
        viewModelScope.launch(Dispatchers.IO) {
            syncMutex.withLock {
                if (!connectivityObserver.hasInternet()) {
                    withContext(Dispatchers.Main) {
                        _syncMessage.value = "No hay conexión a Internet. Los registros se guardan localmente."
                    }
                    return@withLock
                }

                val unsyncedInspections = repository.getUnsyncedInspectionsOnce()

                Log.d("SyncDebug", "Iniciando sincronización. Inspecciones pendientes: ${unsyncedInspections.size}")

                if (unsyncedInspections.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _syncMessage.value = "No hay inspecciones pendientes de sincronizar."
                    }
                    return@withLock
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
        }
    }

    /**
     * Inserta una inspección y la sincroniza inmediatamente con Google Sheets.
     * Esta función se utiliza para el botón "Finalizar".
     * @return True si la inserción y la sincronización fueron exitosas, false en caso contrario.
     */
    suspend fun insertAndSync(inspection: Inspection): Boolean {
        // La lógica se ejecuta dentro de un lock para evitar concurrencia
        return syncMutex.withLock {
            try {
                // 1. Insertar el registro localmente.
                repository.insert(inspection)

                // 2. Verificar la conexión a Internet. Si no hay, la subida no es posible.
                if (!connectivityObserver.hasInternet()) {
                    withContext(Dispatchers.Main) {
                        _syncMessage.value = "No hay conexión a Internet. El registro se guardó localmente."
                    }
                    return@withLock false
                }

                // 3. Subir el registro a Google Sheets.
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

                // 4. Si la subida fue exitosa, marcar el registro como sincronizado.
                if (response.status == "SUCCESS") {
                    val updatedInspection = inspection.copy(isSynced = true, imageUrls = emptyList())
                    repository.update(updatedInspection)
                    withContext(Dispatchers.Main) {
                        _syncMessage.value = "Registro subido con éxito."
                    }
                    return@withLock true
                } else {
                    withContext(Dispatchers.Main) {
                        _syncMessage.value = "Error al subir ${inspection.articulo}: ${response.message}. Guardado localmente."
                    }
                    return@withLock false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _syncMessage.value = "Error de conexión o API. Guardado localmente. ${e.message}"
                }
                Log.e("SyncDebug", "Error al sincronizar datos para ${inspection.articulo}", e)
                return@withLock false
            }
        }
    }
}