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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import okhttp3.RequestBody.Companion.asRequestBody // ¡Añade esta línea!
import okhttp3.MediaType.Companion.toMediaTypeOrNull // ¡Añade esta línea!

// Interfaz para la API de Google Apps Script
interface GoogleAppsScriptService {
    @FormUrlEncoded
    @POST("exec") // El endpoint para Google Apps Script Web App
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
        @Field("imageUrls") imageUrls: String // URLs de las imágenes separadas por coma
    ): ApiResponse

    @Multipart
    @POST("exec")
    suspend fun uploadImage(
        @Part("action") action: String = "uploadImage",
        @Part("uniqueId") uniqueId: String, // Para asociar la imagen con la inspección
        @Part image: MultipartBody.Part
    ): ImageUploadResponse
}

data class ApiResponse(val status: String, val message: String)
data class ImageUploadResponse(val status: String, val message: String, val imageUrl: String?)

class InspectionViewModel(private val repository: InspectionRepository) : ViewModel() {

    // LiveData para observar todas las inspecciones (historial)
    val allInspections: LiveData<List<Inspection>> = repository.allInspections.asLiveData()

    // LiveData para observar las inspecciones no sincronizadas y su conteo
    private val unsyncedInspections = repository.unsyncedInspections.asLiveData()
    val unsyncedCount: LiveData<Int> = unsyncedInspections.map { it.size }

    // LiveData para el estado de la red (simulado por ahora)
    private val _isNetworkAvailable = MutableLiveData<Boolean>()
    val isNetworkAvailable: LiveData<Boolean> = _isNetworkAvailable

    // LiveData para mensajes de sincronización al usuario
    private val _syncMessage = MutableLiveData<String?>()
    val syncMessage: LiveData<String?> = _syncMessage

    // Instancia del servicio de Google Apps Script (inicializar con tu URL)
    // REEMPLAZA ESTA URL CON LA URL DE DESPLIEGUE DE TU GOOGLE APPS SCRIPT WEB APP
    private val GOOGLE_APPS_SCRIPT_WEB_APP_URL = "https://script.google.com/macros/s/AKfycbxd5xeOKNkdNjycH-RlgvW3KdzF8yBy7kmfq8jk9GJO9UhjXWIAY4poCU3RnR9TcMU/exec/" // <-- ¡IMPORTANTE!

    private val googleAppsScriptService: GoogleAppsScriptService by lazy {
        Retrofit.Builder()
            .baseUrl(GOOGLE_APPS_SCRIPT_WEB_APP_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleAppsScriptService::class.java)
    }

    init {
        // Observar inspecciones no sincronizadas para iniciar la sincronización automática
        unsyncedInspections.observeForever { inspections ->
            if (isNetworkAvailable.value == true && inspections.isNotEmpty()) {
                syncUnsyncedInspections()
            }
        }
    }

    fun insertInspection(inspection: Inspection) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(inspection)
    }

    fun updateNetworkStatus(available: Boolean) {
        _isNetworkAvailable.value = available
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun syncUnsyncedInspections() = viewModelScope.launch(Dispatchers.IO) {
        val inspectionsToSync = repository.unsyncedInspections.firstOrNull() ?: emptyList() // Obtener la lista actual

        if (inspectionsToSync.isEmpty()) {
            withContext(Dispatchers.Main) {
                _syncMessage.value = "No hay inspecciones pendientes de sincronizar."
            }
            return@launch
        }

        withContext(Dispatchers.Main) {
            _syncMessage.value = "Sincronizando ${inspectionsToSync.size} inspecciones..."
        }

        var successfulSyncs = 0
        var failedSyncs = 0

        for (inspection in inspectionsToSync) {
            try {
                // Paso 1: Subir imágenes a Google Drive
                val imageUrls = mutableListOf<String>()
                for (imagePath in inspection.imagePaths) {
                    val imageFile = File(imagePath)
                    if (imageFile.exists()) {
                        val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                        val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

                        val imageUploadResponse = googleAppsScriptService.uploadImage(
                            uniqueId = inspection.uniqueId,
                            image = imagePart
                        )

                        if (imageUploadResponse.status == "SUCCESS" && imageUploadResponse.imageUrl != null) {
                            imageUrls.add(imageUploadResponse.imageUrl)
                            // Opcional: Eliminar la imagen local después de subirla
                            imageFile.delete()
                        } else {
                            // Manejar error de subida de imagen
                            withContext(Dispatchers.Main) {
                                _syncMessage.value = "Error al subir imagen para ${inspection.articleReference}: ${imageUploadResponse.message}"
                            }
                            // No continuar con la sincronización de esta inspección si la imagen falla
                            failedSyncs++
                            continue // Ir a la siguiente inspección
                        }
                    } else {
                        // La imagen local no existe, quizás ya fue eliminada o hubo un error previo
                        withContext(Dispatchers.Main) {
                            _syncMessage.value = "La imagen local no existe: $imagePath"
                        }
                    }
                }

                // Paso 2: Enviar datos de la inspección a Google Sheets
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
                    imageUrls = imageUrls.joinToString(",") // Unir URLs con coma
                )

                if (apiResponse.status == "SUCCESS") {
                    // Marcar la inspección como sincronizada en la base de datos local
                    val updatedInspection = inspection.copy(isSynced = true, imageUrls = imageUrls)
                    repository.update(updatedInspection)
                    successfulSyncs++
                } else {
                    // Manejar error de sincronización de datos
                    withContext(Dispatchers.Main) {
                        _syncMessage.value = "Error de sincronización para ${inspection.articleReference}: ${apiResponse.message}"
                    }
                    failedSyncs++
                }
            } catch (e: Exception) {
                // Manejar errores de red o API
                withContext(Dispatchers.Main) {
                    _syncMessage.value = "Error de red/API para ${inspection.articleReference}: ${e.message}"
                }
                failedSyncs++
            }
        }

        withContext(Dispatchers.Main) {
            _syncMessage.value = "Sincronización completada. Éxitos: $successfulSyncs, Fallos: $failedSyncs."
        }
    }
}

// Factory para crear instancias de ViewModel con dependencias
class InspectionViewModelFactory(private val repository: InspectionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InspectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InspectionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}