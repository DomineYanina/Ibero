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
    @SerializedName("valorColumnaA") val valorColumnaA: String?,
    @SerializedName("valorColumnaV") val valorColumnaV: String?,
    @SerializedName("rowNumber") val rowNumber: Int
)

data class TonalidadesResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<TonalidadesData>?
)

data class TonalidadResponseItem(
    val valorColumnaT: String,
    val rowNumber: Int
)

data class InspectionRecord(
    @SerializedName("valorColumnaC") val valorColumnaC: String,
    @SerializedName("valorColumnaD") val valorColumnaD: String,
    @SerializedName("valorColumnaE") val valorColumnaE: String,
    @SerializedName("valorColumnaF") val valorColumnaF: Int,
    @SerializedName("valorColumnaG") val valorColumnaG: Int,
    @SerializedName("valorColumnaH") val valorColumnaH: String,
    @SerializedName("valorColumnaI") val valorColumnaI: Int,
    @SerializedName("valorColumnaJ") val valorColumnaJ: Int,
    @SerializedName("valorColumnaK") val valorColumnaK: String,
    @SerializedName("valorColumnaL") val valorColumnaL: Int,
    @SerializedName("valorColumnaM") val valorColumnaM: Int,
    @SerializedName("valorColumnaN") val valorColumnaN: String,
    @SerializedName("valorColumnaO") val valorColumnaO: String,
    @SerializedName("valorColumnaP") val valorColumnaP: String,
    @SerializedName("valorColumnaQ") val valorColumnaQ: String,
    @SerializedName("valorColumnaS") val valorColumnaS: String,
    @SerializedName("valorColumnaT") val valorColumnaT: String?,
    @SerializedName("valorColumnaU") val valorColumnaU: Double,
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
        @Field("updates") updates: String
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

    // NUEVO: Método para actualizar un registro de inspección
    @FormUrlEncoded
    @POST("exec")
    suspend fun updateInspection(
        @Field("action") action: String = "updateInspection",
        @Field("uniqueId") uniqueId: String,
        @Field("tipoCalidad") tipoCalidad: String,
        @Field("tipoDeFalla") tipoDeFalla: String?,
        @Field("metrosDeTela") metrosDeTela: Double
    ): ApiResponse
}