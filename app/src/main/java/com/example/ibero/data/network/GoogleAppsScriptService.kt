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

interface GoogleAppsScriptService {
    @FormUrlEncoded
    @POST("exec") // ¡Ojo! Aquí va "exec"
    suspend fun addInspection(
        @Field("action") action: String,     // "addInspection"
        @Field("uniqueId") uniqueId: String,
        @Field("usuario") usuario: String,
        @Field("fecha") fecha: String,       // timestamp en milisegundos como String
        @Field("hojaDeRuta") hojaDeRuta: String,
        @Field("tejeduria") tejeduria: String,
        @Field("telar") telar: Int,
        @Field("tintoreria") tintoreria: Int,
        @Field("articulo") articulo: String,
        @Field("tipoCalidad") tipoCalidad: String,
        @Field("tipoDeFalla") tipoDeFalla: String?,  // puede ir null
        @Field("anchoDeRollo") anchoDeRollo: Double,
        @Field("imageUrls") imageUrls: String        // CSV si hay varias
    ): ApiResponse
}

object GoogleSheetsApi {
    /**
     * IMPORTANTÍSIMO:
     * Pega aquí la URL de despliegue de tu Apps Script PERO sin el "/exec" final.
     * Si la URL publicada es:
     *   https://script.google.com/macros/s/AKfycb.../exec
     * usa como BASE_URL:
     *   https://script.google.com/macros/s/AKfycb.../
     */
    private const val BASE_URL =
        "https://script.google.com/macros/s/AKfycbyOV70_ZRAtwe5u9-htIcFFn1LOgKKrjc8KzTrBrkZV-nsM4B7NBhDk0GTZHiV7GEx4NQ/"

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
            .baseUrl(BASE_URL)  // base SIN 'exec'
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(GoogleAppsScriptService::class.java)
    }
}
