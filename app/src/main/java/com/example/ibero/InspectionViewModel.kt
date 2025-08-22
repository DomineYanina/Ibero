package com.example.ibero

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.ibero.data.AppDatabase
import com.example.ibero.data.Inspection
import com.example.ibero.data.network.AddInspectionResponse
import com.example.ibero.data.network.GoogleSheetsApi2
import com.example.ibero.repository.InspectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Date
import kotlinx.coroutines.delay
import java.util.UUID // Importación necesaria

class InspectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: InspectionRepository
    private val connectivityObserver = NetworkConnectivityObserver(application)
    val allInspections: LiveData<List<Inspection>>

    private val _syncMessage = MutableLiveData<String?>()
    val syncMessage: LiveData<String?> get() = _syncMessage

    private val _currentSessionInspections = MutableLiveData<MutableList<Inspection>>(mutableListOf())
    val currentSessionInspections: LiveData<MutableList<Inspection>> get() = _currentSessionInspections

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val syncMutex = Mutex()
    private var sessionData: Inspection? = null

    init {
        val inspectionDao = AppDatabase.getDatabase(application).inspectionDao()
        repository = InspectionRepository(inspectionDao)
        allInspections = repository.allInspections.asLiveData()

        viewModelScope.launch {
            connectivityObserver.observe()
            connectivityObserver.connectionStatus.collectLatest { status ->
                if (status is ConnectionStatus.Available) {
                    Log.d("SyncDebug", "Conexión a Internet detectada. Iniciando sincronización de registros pendientes...")
                    performSync()
                }
            }
        }

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

    fun initSessionData(
        usuario: String,
        hojaDeRuta: String,
        fecha: Date,
        tejeduria: String,
        telar: String,
        tintoreria: String,
        articulo: String,
        color: String,
        rolloDeUrdido: String,
        orden: String,
        cadena: String,
        anchoDeRollo: String,
        esmerilado: String,
        ignifugo: String,
        impermeable: String,
        otro: String
    ) {
        try {
            sessionData = Inspection(
                usuario = usuario,
                fecha = fecha,
                hojaDeRuta = hojaDeRuta,
                tejeduria = tejeduria,
                telar = telar.toIntOrNull() ?: 0,
                tintoreria = tintoreria.toIntOrNull() ?: 0,
                articulo = articulo,
                color = color.toIntOrNull() ?: 0,
                rolloDeUrdido = rolloDeUrdido.toIntOrNull() ?: 0,
                orden = orden,
                cadena = cadena.toIntOrNull() ?: 0,
                anchoDeRollo = anchoDeRollo.toIntOrNull() ?: 0,
                esmerilado = esmerilado,
                ignifugo = ignifugo,
                impermeable = impermeable,
                otro = otro,
                tipoCalidad = "",
                tipoDeFalla = null,
                metrosDeTela = 0.0,
                uniqueId = UUID.randomUUID().toString() // <<-- CAMBIO AQUI: Generar un UUID único
            )
        } catch (e: NumberFormatException) {
            Log.e("ViewModel", "Error al convertir un campo de String a Int. Revisar los datos de entrada.", e)
            throw e
        }
    }

    fun getCurrentSessionData(): Inspection {
        return sessionData ?: throw IllegalStateException("Session data not initialized.")
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
        val existingIndex = currentList.indexOfFirst { it.uniqueId == inspection.uniqueId }
        if (existingIndex != -1) {
            currentList[existingIndex] = inspection
        } else {
            currentList.add(0, inspection)
        }
        _currentSessionInspections.value = currentList
    }

    fun clearCurrentSessionList() {
        _currentSessionInspections.value = mutableListOf()
    }

    fun performSync() {
        viewModelScope.launch(Dispatchers.IO) {
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
                        val response: AddInspectionResponse = GoogleSheetsApi2.service.addInspection(
                            action = "addInspection",
                            usuario = inspection.usuario,
                            fecha = inspection.fecha.time.toString(),
                            hojaDeRuta = inspection.hojaDeRuta,
                            tejeduria = inspection.tejeduria,
                            telar = inspection.telar.toString(),
                            tintoreria = inspection.tintoreria.toString(),
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
                            imageUrls = inspection.imageUrls.joinToString(","),
                            uniqueId = inspection.uniqueId // <<-- AÑADE ESTA LINEA
                        )

                        if (response.status == "SUCCESS" && response.data.uniqueId != null) {
                            val newUniqueId = response.data.uniqueId
                            // Clave: Actualizar el registro local con el ID devuelto por el servidor
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
            withContext(Dispatchers.Main) {
                _isLoading.value = false
            }
        }
    }

    suspend fun finalizeAndSync(inspection: Inspection): Boolean {
        withContext(Dispatchers.Main) {
            _isLoading.value = true
        }

        try {
            val response: AddInspectionResponse = GoogleSheetsApi2.service.addInspection(
                action = "addInspection",
                usuario = inspection.usuario,
                fecha = inspection.fecha.time.toString(),
                hojaDeRuta = inspection.hojaDeRuta,
                tejeduria = inspection.tejeduria,
                telar = inspection.telar.toString(),
                tintoreria = inspection.tintoreria.toString(),
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
                imageUrls = inspection.imageUrls.joinToString(","),
                uniqueId = inspection.uniqueId // <<-- AÑADE ESTA LINEA
            )

            if (response.status == "SUCCESS" && response.data.uniqueId != null) {
                val newUniqueId = response.data.uniqueId
                val updatedInspection = inspection.copy(uniqueId = newUniqueId, isSynced = true, imageUrls = emptyList())
                // Clave: Insertar el registro con el ID devuelto por el servidor
                repository.insert(updatedInspection)
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
                return true
            } else {
                repository.insert(inspection)
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
                return false
            }
        } catch (e: Exception) {
            repository.insert(inspection)
            withContext(Dispatchers.Main) {
                _isLoading.value = false
            }
            Log.e("ViewModel", "Error al finalizar y sincronizar", e)
            return false
        }
    }

    // NUEVO MÉTODO: Actualiza el registro local y lo sincroniza con Google Sheets
    suspend fun updateInspectionAndSync(inspection: Inspection) {
        Log.d("UpdateDebug", "Llamada a updateInspectionAndSync en el ViewModel. UniqueId: ${inspection.uniqueId}")

        // 1. Actualiza el registro en la base de datos local
        try {
            repository.update(inspection)
            Log.d("UpdateDebug", "Registro local actualizado exitosamente.")
        } catch (e: Exception) {
            Log.e("UpdateError", "Error al actualizar el registro local: ${e.message}", e)
        }

        // 2. Notifica a la UI sobre el cambio en la lista de la sesión
        addInspectionToSessionList(inspection)
        Log.d("UpdateDebug", "Lista de la sesión actualizada.")

        // 3. Intenta sincronizar el cambio con la nube
        val success = syncOneInspection(inspection)
        Log.d("UpdateDebug", "Intento de sincronización con la nube completado. Éxito: $success")
    }

    // NUEVO MÉTODO: Actualiza el registro local, lo sincroniza con la nube y finaliza
    suspend fun updateInspectionAndFinalize(inspection: Inspection): Boolean {
        // 1. Actualiza el registro en la base de datos local
        repository.update(inspection)
        // 2. Sincroniza y retorna el resultado
        return syncOneInspection(inspection)
    }

    // NUEVO MÉTODO: Sincroniza un solo registro
    private suspend fun syncOneInspection(inspection: Inspection): Boolean {
        withContext(Dispatchers.Main) {
            _isLoading.value = true
            Log.d("UpdateDebug", "Indicador de carga activado.")
        }

        if (!connectivityObserver.hasInternet()) {
            withContext(Dispatchers.Main) {
                _syncMessage.value = "No hay conexión a Internet. El registro se guardó localmente."
                _isLoading.value = false
            }
            Log.e("UpdateError", "No hay conexión a Internet.")
            return false
        }

        // NUEVA LÓGICA: Lógica de reintento
        val maxRetries = 3
        var currentRetry = 0
        var success = false

        while (currentRetry < maxRetries && !success) {
            try {
                Log.d("UpdateDebug", "Iniciando llamada a la API para updateInspection (Intento ${currentRetry + 1}).")
                val response = GoogleSheetsApi2.service.updateInspection(
                    uniqueId = inspection.uniqueId,
                    tipoCalidad = inspection.tipoCalidad,
                    tipoDeFalla = inspection.tipoDeFalla,
                    metrosDeTela = inspection.metrosDeTela
                )
                Log.d("UpdateDebug", "Respuesta de la API recibida. Status: ${response.status}, Message: ${response.message}")

                if (response.status == "SUCCESS") {
                    val updatedInspection = inspection.copy(isSynced = true)
                    repository.update(updatedInspection)
                    withContext(Dispatchers.Main) {
                        _syncMessage.value = "Registro actualizado exitosamente en la nube."
                    }
                    Log.d("UpdateDebug", "Actualización exitosa en la nube y en la base de datos local.")
                    success = true
                } else {
                    withContext(Dispatchers.Main) {
                        _syncMessage.value = "Fallo al actualizar el registro en la nube: ${response.message}"
                    }
                    Log.e("UpdateError", "Fallo en la API. Status: ${response.status}, Message: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("UpdateError", "Error al sincronizar una inspección: ${e.message}", e)
                if (e is retrofit2.HttpException && e.code() == 429) {
                    // Si es un error 429, esperamos un poco antes de reintentar
                    val waitTime = (2000 * (currentRetry + 1)).toLong() // Espera 2, 4, 6 segundos, etc.
                    Log.w("UpdateWarning", "Recibido error 429. Esperando ${waitTime / 1000} segundos antes de reintentar...")
                    withContext(Dispatchers.Main) {
                        _syncMessage.value = "Demasiadas peticiones. Reintentando en ${waitTime / 1000} segundos..."
                    }
                    delay(waitTime)
                } else {
                    // Para cualquier otro error, no reintentamos
                    withContext(Dispatchers.Main) {
                        _syncMessage.value = "Error al actualizar. Se guardó localmente. ${e.message}"
                    }
                    break // Salimos del bucle de reintento
                }
            } finally {
                currentRetry++
            }
        }

        // Si después de todos los reintentos no hubo éxito, limpiamos el estado de carga
        if (!success) {
            withContext(Dispatchers.Main) {
                _isLoading.value = false
                Log.d("UpdateDebug", "Indicador de carga desactivado. Finalizado.")
            }
        }

        return success
    }
}