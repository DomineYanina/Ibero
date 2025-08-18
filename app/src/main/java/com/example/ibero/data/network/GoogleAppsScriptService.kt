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

// Modelo de datos para la respuesta de la búsqueda de tonalidades
data class TonalidadesResponse(
    val status: String,
    val message: String?, // Ahora el mensaje puede ser nulo en caso de éxito
    val data: List<TonalidadResponseItem>?
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
}

object GoogleSheetsApi {
    /**
     * Pega aquí la URL de despliegue de tu Apps Script PERO sin el "/exec" final.
     */
    private const val BASE_URL =
        "https://script.google.com/macros/s/AKfycbxTONND5KQtyhwqCm_HwN5yz57MsxJjsU_XikpY1jGcioqgJxdapG7ztDSB7so14Az3dQ/"

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