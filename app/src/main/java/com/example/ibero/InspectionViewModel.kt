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

    private val _isNetworkAvailable = MutableLiveData<Boolean>(false)
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

    private var isSyncing = false
    private var syncJob: Job? = null

    init {
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
        withContext(Dispatchers.Main) {
            if (_isNetworkAvailable.value == true && !isSyncing) {
                triggerSync()
            }
        }
    }

    fun updateNetworkStatus(available: Boolean) {
        val oldStatus = _isNetworkAvailable.value
        _isNetworkAvailable.value = available
        if (available && oldStatus == false && (unsyncedCount.value ?: 0) > 0 && !isSyncing) {
            triggerSync()
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

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

    private fun triggerSync() {
        if (isSyncing) return

        syncJob?.cancel()
        syncJob = viewModelScope.launch(Dispatchers.IO) {
            performSync()
        }
    }

    private suspend fun performSync() {
        if (isSyncing) {
            return
        }
        isSyncing = true

        try {
            val inspectionsToSync = repository.unsyncedInspections.firstOrNull() ?: emptyList()

            if (inspectionsToSync.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _syncMessage.value = "No hay inspecciones pendientes de sincronizar."
                }
                return
            }

            withContext(Dispatchers.Main) {
                _syncMessage.value = "Sincronizando ${inspectionsToSync.size} inspecciones..."
            }

            var successfulSyncs = 0
            var failedSyncs = 0

            for (inspection in inspectionsToSync) {

                var currentInspectionFailed = false
                val uploadedImageUrls = mutableListOf<String>()

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
                                imageFile.delete()
                            } else {
                                withContext(Dispatchers.Main) {
                                    _syncMessage.value = "Error al subir imagen para ${inspection.articleReference}: ${imageUploadResponse.message}"
                                }
                                currentInspectionFailed = true
                                break
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                _syncMessage.value = "Error de red/API al subir imagen para ${inspection.articleReference}: ${e.message}"
                            }
                            currentInspectionFailed = true
                            break
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            _syncMessage.value = "La imagen local no existe: $imagePath para ${inspection.articleReference}"
                        }
                    }
                }

                if (currentInspectionFailed) {
                    failedSyncs++
                    continue
                }

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
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _syncMessage.value = "Error inesperado durante la sincronización: ${e.message}"
            }
        } finally {
            isSyncing = false
            withContext(Dispatchers.Main) {
                if (_isNetworkAvailable.value == true && (unsyncedCount.value ?: 0) > 0) {
                    triggerSync()
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
