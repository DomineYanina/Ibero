/*package com.example.ibero.network

import android.util.Log
import com.example.ibero.data.Inspection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException

// Reemplaza con la URL de despliegue de tu proyecto Apps Script
// La URL de tu Logcat es diferente, por lo que te la he actualizado
private const val SCRIPT_URL = "https://script.google.com/macros/s/AKfycbyFdZgBGRgA4DrZDcOcOwgcGLkRd4xe31QjTB7pRsUcYz3_eFDoTlXS6Lie8_8ZHeYJiA/exec/"

object ApiService {

    private val client = OkHttpClient()

    /**
     * Envía una inspección a Google Sheets.
     * @param inspection El objeto Inspection a enviar.
     * @return true si la operación fue exitosa, false en caso contrario.
     */
    suspend fun addInspection(inspection: Inspection): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("action", "addInspection")
                    .addFormDataPart("uniqueId", inspection.uniqueId)
                    .addFormDataPart("usuario", inspection.usuario)
                    // CAMBIO: Enviamos la fecha como un timestamp (milisegundos)
                    .addFormDataPart("fecha", inspection.fecha.time.toString())
                    .addFormDataPart("hojaDeRuta", inspection.hojaDeRuta)
                    .addFormDataPart("tejeduria", inspection.tejeduria)
                    .addFormDataPart("telar", inspection.telar.toString())
                    .addFormDataPart("tintoreria", inspection.tintoreria.toString())
                    .addFormDataPart("articulo", inspection.articulo)
                    .addFormDataPart("tipoCalidad", inspection.tipoCalidad)
                    .addFormDataPart("tipoDeFalla", inspection.tipoDeFalla ?: "")
                    .addFormDataPart("anchoDeRollo", inspection.anchoDeRollo.toString())
                    .addFormDataPart("imageUrls", inspection.imageUrls.joinToString(","))
                    .build()

                val request = Request.Builder()
                    .url(SCRIPT_URL)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    Log.d("ApiService", "Inspección enviada exitosamente: ${response.body?.string()}")
                    true
                }
            } catch (e: Exception) {
                Log.e("ApiService", "Error al enviar inspección", e)
                false
            }
        }
    }

    /**
     * Sube una imagen a Google Drive.
     * @param uniqueId ID único de la inspección.
     * @param imageFile El archivo de imagen.
     * @return La URL de la imagen si fue exitosa, o null.
     */
    suspend fun uploadImage(uniqueId: String, imageFile: File): String? {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("action", "uploadImage")
                    .addFormDataPart("uniqueId", uniqueId)
                    .addFormDataPart("image", imageFile.name, imageFile.asRequestBody())
                    .build()

                val request = Request.Builder()
                    .url(SCRIPT_URL)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val responseBody = response.body?.string()
                    Log.d("ApiService", "Imagen subida exitosamente: $responseBody")
                    // Aquí podrías parsear el JSON de la respuesta para obtener la URL
                    // Por ahora, asumimos que el script devuelve la URL en el cuerpo de la respuesta
                    responseBody
                }
            } catch (e: Exception) {
                Log.e("ApiService", "Error al subir imagen", e)
                null
            }
        }
    }
}*/
