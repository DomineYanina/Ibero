package com.example.ibero

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.ibero.data.Inspection
import com.example.ibero.repository.InspectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

// Interfaz para la API de Google Apps Script
interface GoogleAppsScriptService {
    @FormUrlEncoded
    @POST("exec")
    suspend fun uploadInspectionData(
        @Field("action") action: String = "addInspection",
        @Field("uniqueId") uniqueId: String,
        @Field("inspectionDate") inspectionDate: String,
        @Field("inspectionTime") inspectionTime: String,
        @Field("inspectorName") inspectorName: String,
        @Field("orderNumber") orderNumber: String,
        @Field("articleReference") articleReference: String,
        @Field("supplier") supplier: String,
        @Field("color") color: String,
        @Field("totalLotQuantity") totalLotQuantity: Int,
        @Field("sampleQuantity") sampleQuantity: Int,
        @Field("defectType") defectType: String,
        @Field("otherDefectDescription") otherDefectDescription: String?,
        @Field("defectiveItemsQuantity") defectiveItemsQuantity: Int,
        @Field("defectDescription") defectDescription: String,
        @Field("actionTaken") actionTaken: String,
        @Field("imageUrls") imageUrls: String
    ): ApiResponse

    @Multipart
    @POST("exec")
    suspend fun uploadImage(
        @Part("action") action: String = "uploadImage",
        @Part("uniqueId") uniqueId: String,
        @Part image: MultipartBody.Part
    ): ImageUploadResponse
}

data class ApiResponse(val status: String, val message: String)
data class ImageUploadResponse(val status: String, val message: String, val imageUrl: String?)

class InspectionViewModel(private val repository: InspectionRepository) : ViewModel() {

    val allInspections: LiveData<List<Inspection>> = repository.allInspections.asLiveData()

    private val unsyncedInspections = repository.unsyncedInspections.asLiveData()
    val unsyncedCount: LiveData<Int> = unsyncedInspections.map { it.size }

    private val _isNetworkAvailable = MutableLiveData<Boolean>(false) // Iniciar como false hasta que se confirme
    val isNetworkAvailable: LiveData<Boolean> = _isNetworkAvailable

    private val _syncMessage = MutableLiveData<String?>()
    val syncMessage: LiveData<String?> = _syncMessage

    private val GOOGLE_APPS_SCRIPT_WEB_APP_URL = "https://script.google.com/macros/s/AKfycbyUcLe6tu_U0ZjK9F_qu6CnSsXNyZB3C89OjTD5gLXu5h5XxOSZRLzwZqpj-QLGJWPUxA/exec/"

    private val googleAppsScriptService: GoogleAppsScriptService by lazy {
        Retrofit.Builder()
            .baseUrl(GOOGLE_APPS_SCRIPT_WEB_APP_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleAppsScriptService::class.java)
    }

    private var isSyncing = false // Bandera para controlar la sincronización en curso
    private var syncJob: Job? = null // Para mantener una referencia al job de sincronización si se necesita cancelarlo

    init {
        // Observar cambios en la red y en las inspecciones no sincronizadas para la sincronización automática
        _isNetworkAvailable.observeForever { available ->
            if (available && (unsyncedCount.value ?: 0) > 0 && !isSyncing) {
                triggerSync()
            }
        }
        unsyncedInspections.observeForever { inspections ->
            if (_isNetworkAvailable.value == true && inspections.isNotEmpty() && !isSyncing) {
                triggerSync()
            }
        }
    }

    fun insertInspection(inspection: Inspection) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(inspection)
        // Después de insertar, si la red está disponible y no se está sincronizando, intentar sincronizar
        withContext(Dispatchers.Main) { // Cambiar a Main para acceder a _isNetworkAvailable.value de forma segura
            if (_isNetworkAvailable.value == true && !isSyncing) {
                triggerSync()
            }
        }
    }

    fun updateNetworkStatus(available: Boolean) {
        val oldStatus = _isNetworkAvailable.value
        _isNetworkAvailable.value = available
        // Si la red acaba de estar disponible y antes no lo estaba, intentar sincronizar
        if (available && oldStatus == false && (unsyncedCount.value ?: 0) > 0 && !isSyncing) {
            triggerSync()
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    /**
     * Inicia el proceso de sincronización si no hay otra sincronización en curso.
     * Esta es la función que debe ser llamada desde la UI (botón de sincronizar).
     */
    fun requestManualSync() {
        if (!isNetworkAvailable.value!!) {
            _syncMessage.value = "No hay conexión de red para sincronizar."
            return
        }
        if (isSyncing) {
            _syncMessage.value = "La sincronización ya está en progreso."
            return
        }
        if ((unsyncedCount.value ?: 0) == 0) {
            _syncMessage.value = "No hay inspecciones pendientes de sincronizar."
            return
        }
        triggerSync()
    }

    /**
     * Función interna que lanza la corrutina de sincronización.
     */
    private fun triggerSync() {
        if (isSyncing) return // Ya se está sincronizando

        // Cancelar cualquier job de sincronización anterior si por alguna razón estuviera activo
        // aunque con la bandera isSyncing esto no debería ser estrictamente necesario,
        // es una salvaguarda adicional.
        syncJob?.cancel()
        syncJob = viewModelScope.launch(Dispatchers.IO) { // Usar Dispatchers.IO para operaciones de red/DB
            performSync()
        }
    }

    private suspend fun performSync() { // Marcada como suspend, llamada desde una corrutina
        if (isSyncing) {
            return // Doble verificación por si acaso
        }
        isSyncing = true

        try {
            // Obtener la lista una sola vez al inicio de la sincronización
            val inspectionsToSync = repository.unsyncedInspections.firstOrNull() ?: emptyList()

            if (inspectionsToSync.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _syncMessage.value = "No hay inspecciones pendientes de sincronizar."
                }
                return // Salir si no hay nada que sincronizar
            }

            withContext(Dispatchers.Main) {
                _syncMessage.value = "Sincronizando ${inspectionsToSync.size} inspecciones..."
            }

            var successfulSyncs = 0
            var failedSyncs = 0

            for (inspection in inspectionsToSync) {
                // Opcional: Volver a verificar el estado de 'isSynced' de la inspección actual
                // desde la base de datos antes de intentar subirla. Esto ayuda si múltiples workers
                // o un bug causaron que se procese dos veces.
                // val currentDbInspection = repository.getInspectionById(inspection.id).firstOrNull() // Necesitarías un método así
                // if (currentDbInspection?.isSynced == true) {
                //     successfulSyncs++ // Contar como éxito si ya está sincronizada
                //     continue
                // }

                var currentInspectionFailed = false
                val uploadedImageUrls = mutableListOf<String>()

                // Paso 1: Subir imágenes a Google Drive
                for (imagePath in inspection.imagePaths) {
                    val imageFile = File(imagePath)
                    if (imageFile.exists()) {
                        val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                        val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

                        try {
                            val imageUploadResponse = googleAppsScriptService.uploadImage(
                                uniqueId = inspection.uniqueId,
                                image = imagePart
                            )

                            if (imageUploadResponse.status == "SUCCESS" && imageUploadResponse.imageUrl != null) {
                                uploadedImageUrls.add(imageUploadResponse.imageUrl)
                                // Opcional: Considera eliminar la imagen local aquí solo si la sincronización
                                // completa de la inspección (incluyendo datos) es exitosa.
                                // Por ahora, la eliminaremos aquí y si la subida de datos falla,
                                // la imagen ya estará en Drive.
                                imageFile.delete()
                            } else {
                                withContext(Dispatchers.Main) {
                                    _syncMessage.value = "Error al subir imagen para ${inspection.articleReference}: ${imageUploadResponse.message}"
                                }
                                currentInspectionFailed = true
                                break // Salir del bucle de imágenes si una falla para esta inspección
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                _syncMessage.value = "Error de red/API al subir imagen para ${inspection.articleReference}: ${e.message}"
                            }
                            currentInspectionFailed = true
                            break // Salir del bucle de imágenes
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            _syncMessage.value = "La imagen local no existe: $imagePath para ${inspection.articleReference}"
                        }
                        // Decide si esto debe contar como un fallo de la inspección
                        // currentInspectionFailed = true; break;
                    }
                }

                if (currentInspectionFailed) {
                    failedSyncs++
                    continue // Ir a la siguiente inspección
                }

                // Paso 2: Enviar datos de la inspección a Google Sheets
                try {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val inspectionDateFormatted = dateFormat.format(inspection.inspectionDate)

                    val apiResponse = googleAppsScriptService.uploadInspectionData(
                        uniqueId = inspection.uniqueId,
                        inspectionDate = inspectionDateFormatted,
                        inspectionTime = inspection.inspectionTime,
                        inspectorName = inspection.inspectorName,
                        orderNumber = inspection.orderNumber,
                        articleReference = inspection.articleReference,
                        supplier = inspection.supplier,
                        color = inspection.color,
                        totalLotQuantity = inspection.totalLotQuantity,
                        sampleQuantity = inspection.sampleQuantity,
                        defectType = inspection.defectType,
                        otherDefectDescription = inspection.otherDefectDescription,
                        defectiveItemsQuantity = inspection.defectiveItemsQuantity,
                        defectDescription = inspection.defectDescription,
                        actionTaken = inspection.actionTaken,
                        imageUrls = uploadedImageUrls.joinToString(",")
                    )

                    if (apiResponse.status == "SUCCESS") {
                        val updatedInspection = inspection.copy(isSynced = true, imageUrls = uploadedImageUrls)
                        repository.update(updatedInspection)
                        successfulSyncs++
                    } else {
                        withContext(Dispatchers.Main) {
                            _syncMessage.value = "Error de sincronización para ${inspection.articleReference}: ${apiResponse.message}"
                        }
                        failedSyncs++
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _syncMessage.value = "Error de red/API al sincronizar datos para ${inspection.articleReference}: ${e.message}"
                    }
                    failedSyncs++
                }
            }

            withContext(Dispatchers.Main) {
                if (failedSyncs > 0) {
                    _syncMessage.value = "Sincronización completada con algunos errores. Éxitos: $successfulSyncs, Fallos: $failedSyncs."
                } else {
                    _syncMessage.value = "Sincronización completada. Todas las $successfulSyncs inspecciones sincronizadas."
                }
                // Si aún hay inspecciones pendientes y la red está disponible, podríamos intentar otra sincronización.
                // Sin embargo, el observador `unsyncedInspections` y `_isNetworkAvailable` deberían
                // manejar esto si `repository.update` causa una nueva emisión y aún quedan elementos.
                // Por seguridad, podrías añadir una llamada a triggerSync() aquí si fuera necesario y
                // si failedSyncs > 0 (porque los exitosos ya no estarán en la lista)
                // if (failedSyncs > 0 && _isNetworkAvailable.value == true && (unsyncedCount.value ?: 0) > 0) {
                //    triggerSync() // Cuidado con bucles infinitos si algo sigue fallando.
                // }
            }
        } catch (e: Exception) { // Capturar excepciones generales de la corrutina de sincronización
            withContext(Dispatchers.Main) {
                _syncMessage.value = "Error inesperado durante la sincronización: ${e.message}"
            }
        } finally {
            isSyncing = false
            // Después de terminar un ciclo de sincronización, verificar si aún hay elementos
            // y si las condiciones se cumplen para otro ciclo (por si se añadieron nuevos elementos mientras se sincronizaba).
            withContext(Dispatchers.Main) { // Acceder a LiveData desde el hilo principal
                if (_isNetworkAvailable.value == true && (unsyncedCount.value ?: 0) > 0) {
                    triggerSync() // Esto iniciará otro ciclo si es necesario, protegido por `isSyncing` al inicio de performSync
                }
            }
        }
    }
}

class InspectionViewModelFactory(private val repository: InspectionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InspectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InspectionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
