package com.example.ibero.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// Debe corresponder al JSON que devuelve tu Apps Script.
data class ApiResponse(val status: String, val message: String)

// Nuevo objeto de respuesta para la búsqueda de tonalidades
data class TonalidadesResponse(
    val status: String,
    val data: List<TonalidadResponseItem>
) {
    val message: String
        get() {
            TODO()
        }
}

// Nuevo modelo de datos para los ítems de la respuesta de búsqueda
data class TonalidadResponseItem(
    val valorColumnaT: String,
    val rowNumber: Int // El número de fila es clave para la actualización
)

interface GoogleAppsScriptService {
    @FormUrlEncoded
    @POST("exec") // ¡Ojo! Aquí va "exec"
    suspend fun addInspection(
        @Field("action") action: String = "addInspection",
        @Field("uniqueId") uniqueId: String,
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
        @Field("esmerilado") esmerilado: String,
        @Field("ignifugo") ignifugo: String,
        @Field("impermeable") impermeable: String,
        @Field("otro") otro: String,
        @Field("tipoCalidad") tipoCalidad: String,
        @Field("tipoDeFalla") tipoDeFalla: String?,
        @Field("metrosDeTela") metrosDeTela: Double,
        @Field("imageUrls") imageUrls: String
    ): ApiResponse

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
        "https://script.google.com/macros/s/AKfycbyR1hqIeKK245cv4Wm_cMPa8LG0bg5z0HqYMnXd8LlAuO7agjbDUlBKlMfVuMCEFLhHDA/"

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