package com.example.ibero.data.network

import com.example.ibero.data.HistoricalInspection
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Query
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

// NUEVOS modelos de datos para los registros de inspección del historial
data class InspectionRecord(
    @SerializedName("valorColumnaC") val valorColumnaC: String, // Fecha
    @SerializedName("valorColumnaD") val valorColumnaD: String, // Hoja de Ruta
    @SerializedName("valorColumnaE") val valorColumnaE: String, // Tejeduria
    @SerializedName("valorColumnaF") val valorColumnaF: Int,    // Telar
    @SerializedName("valorColumnaG") val valorColumnaG: Int,    // Tintoreria
    @SerializedName("valorColumnaH") val valorColumnaH: String, // Articulo
    @SerializedName("valorColumnaI") val valorColumnaI: Int,    // Color
    @SerializedName("valorColumnaJ") val valorColumnaJ: Int,    // Rollo de Urdido
    @SerializedName("valorColumnaK") val valorColumnaK: String, // Orden
    @SerializedName("valorColumnaL") val valorColumnaL: Int,    // Cadena
    @SerializedName("valorColumnaM") val valorColumnaM: Int,    // Ancho de Rollo
    @SerializedName("valorColumnaN") val valorColumnaN: String, // Esmerilado
    @SerializedName("valorColumnaO") val valorColumnaO: String, // Ignifugo
    @SerializedName("valorColumnaP") val valorColumnaP: String, // Impermeable
    @SerializedName("valorColumnaQ") val valorColumnaQ: String, // Otro
    @SerializedName("valorColumnaS") val valorColumnaS: String, // Tipo de Calidad
    @SerializedName("valorColumnaT") val valorColumnaT: String?, // Tipo de Falla
    @SerializedName("valorColumnaU") val valorColumnaU: Double, // Metros de Tela
    @SerializedName("valorColumnaA") val valorColumnaA: String?
)

data class InspectionRecordsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<InspectionRecord>?
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
        @Field("telar") telar: String,
        @Field("tintoreria") tintoreria: String,
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

    @FormUrlEncoded
    @POST("exec")
    suspend fun findInspectionRecords(
        @Field("action") action: String = "findInspectionRecords",
        @Field("hojaDeRuta") hojaDeRuta: String
    ): InspectionRecordsResponse

}

object GoogleSheetsApi {
    /**
     * Pega aquí la URL de despliegue de tu Apps Script PERO sin el "/exec" final.
     */
    private const val BASE_URL =
        "https://script.google.com/macros/s/AKfycbyMPARoOA3VpB28wo7pMT5gKPikS29NLGIEyUxxv0P9h1FJL2ZK7IyUMYvczTCBgIai/"

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
