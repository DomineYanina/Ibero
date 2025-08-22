package com.example.ibero.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded

interface GoogleSheetsApiService {

    @POST("exec")
    suspend fun findInspectionRecords(
        @Query("action") action: String = "findInspectionRecords",
        @Query("hojaDeRuta") hojaDeRuta: String
    ): GoogleSheetsResponse

    @FormUrlEncoded
    @POST("exec")
    suspend fun updateInspection(
        @Field("action") action: String = "updateInspection",
        @Field("uniqueId") uniqueId: String,
        @Field("tipoCalidad") tipoCalidad: String,
        @Field("tipoDeFalla") tipoDeFalla: String?,
        @Field("metrosDeTela") metrosDeTela: Double
    ): ApiResponse

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
        @Field("imageUrls") imageUrls: String,
        @Field("uniqueId") uniqueId: String // <<-- CAMBIO AQUI: AÑADIR EL PARAMETRO
    ): AddInspectionResponse

    @FormUrlEncoded
    @POST("exec")
    suspend fun checkHojaRutaExistence(
        @Field("action") action: String = "checkHojaRutaExistence",
        @Field("hojaDeRuta") hojaDeRuta: String
    ): CheckHojaRutaResponse

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
}

object GoogleSheetsApi2 {
    private const val BASE_URL = "https://script.google.com/macros/s/AKfycbz_dsYnvVkCNNg_rfm_Q3UUgBf2hFlap_EbItw80m8C3g1w5FVQysmxhH-mFH1Y1YeiRA/"

    val service: GoogleSheetsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleSheetsApiService::class.java)
    }
}