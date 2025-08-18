package com.example.ibero.data.network

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// Modelos de datos para la nueva respuesta de addInspection
data class AddInspectionResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String?,
    @SerializedName("data")
    val data: AddInspectionData
)

data class AddInspectionData(
    @SerializedName("uniqueId")
    val uniqueId: String?
)

data class TonalidadesData(
    // El campo que antes se llamaba valorColumnaT ahora es más claro como valorColumnaA
    @SerializedName("valorColumnaA") val valorColumnaA: String?,
    // ¡NUEVO CAMPO!
    @SerializedName("valorColumnaV") val valorColumnaV: String?,
    @SerializedName("rowNumber") val rowNumber: Int
)

// Modelo de la respuesta completa del servidor
data class TonalidadesResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<TonalidadesData>?
)
// Modelo de datos para los ítems de la respuesta de búsqueda
data class TonalidadResponseItem(
    val valorColumnaT: String,
    val rowNumber: Int // El número de fila es clave para la actualización
)

interface GoogleAppsScriptService {
    @FormUrlEncoded
    @POST("exec")
    suspend fun addInspection(
        @Field("action") action: String = "addInspection",
        // Campo 'uniqueId' ELIMINADO. Ahora lo devuelve el servidor.
        @Field("usuario") usuario: String,
        @Field("fecha") fecha: String,
        @Field("hojaDeRuta") hojaDeRuta: String,
        @Field("tejeduria") tejeduria: String,
        @Field("telar") telar: Int,
        @Field("tintoreria") tintoreria: Int,
        @Field("articulo") articulo: String,
        @Field("color") color: Int,
        @Field("rolloDeUrdido") rolloDeUrdido: Int,
        @Field("orden") orden: String,
        @Field("cadena") cadena: Int,
        @Field("anchoDeRollo") anchoDeRollo: Int,
        @Field("esmerilado") esmerilado: String?,
        @Field("ignifugo") ignifugo: String?,
        @Field("impermeable") impermeable: String?,
        @Field("otro") otro: String?,
        @Field("tipoCalidad") tipoCalidad: String?,
        @Field("tipoDeFalla") tipoDeFalla: String?,
        @Field("metrosDeTela") metrosDeTela: Double,
        @Field("imageUrls") imageUrls: String
    ): AddInspectionResponse

    @FormUrlEncoded
    @POST("exec")
    suspend fun findTonalidades(
        @Field("action") action: String = "findTonalidades",
        @Field("hojaDeRuta") hojaDeRuta: String
    ): TonalidadesResponse

    @FormUrlEncoded
    @POST("exec")
    suspend fun updateTonalidades(
        @Field("action") action: String = "updateTonalidades",
        @Field("updates") updates: String // JSON string de los ítems a actualizar
    ): ApiResponse

    @FormUrlEncoded
    @POST("exec")
    suspend fun checkHojaRutaExistence(
        @Field("action") action: String = "checkHojaRutaExistence",
        @Field("hojaDeRuta") hojaDeRuta: String
    ): CheckHojaRutaResponse
}

object GoogleSheetsApi {
    /**
     * Pega aquí la URL de despliegue de tu Apps Script PERO sin el "/exec" final.
     */
    private const val BASE_URL =
        "https://script.google.com/macros/s/AKfycbw2IMC8WgTLb23bRHZpjpc7lH9opF0wcWcwpqN2_bldP4UCsu2M8c0axugTexY1v6jp1Q/"

    val service: GoogleAppsScriptService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(GoogleAppsScriptService::class.java)
    }
}