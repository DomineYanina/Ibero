package com.example.ibero.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.Query
import com.example.ibero.data.network.GoogleSheetsResponse

interface GoogleSheetsApiService {

    @POST("exec")
    suspend fun findInspectionRecords(
        @Query("action") action: String = "findInspectionRecords",
        @Query("hojaDeRuta") hojaDeRuta: String
    ): GoogleSheetsResponse
}

object GoogleSheetsApi2 {
    private const val BASE_URL = "https://script.google.com/macros/s/AKfycbxflqwCs0_ZY2ipShIjAil2duFAoL1HoqHZxb28IXzOAG3vv_G_dXashFgqUO4V7DHkYA/"

    val service: GoogleSheetsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleSheetsApiService::class.java)
    }
}
