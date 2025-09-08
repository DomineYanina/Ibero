package com.example.ibero.data.network

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import java.util.concurrent.TimeUnit

data class ArticulosResponse(
    val status: String,
    val message: String,
    val data: ArticulosData? // El data puede ser null
)

// Modifica ArticulosData para que contenga solo la lista de strings
data class ArticulosData(
    val articulos: List<String>
)

data class TiposDeFallaResponse(
    val status: String,
    val message: String,
    val data: TiposDeFallaData?
)

data class TiposDeFallaData(
    val tiposDeFallas: List<String>
)

data class TejeduriasResponse(
    val status: String,
    val message: String,
    val data: TejeduriasData?
)

data class TejeduriasData(
    val tejedurias: List<String>
)

data class TelaresResponse(
    val status: String,
    val message: String,
    val data: TelaresData?
)

data class TelaresData(
    val telares: List<Int>
)

data class HojasDeRutaReponse(
    val status: String,
    val message: String,
    val data: HojasDeRutaData?
)

data class HojasDeRutaData(
    val hojasDeRuta: List<String>
)

interface GoogleSheetsApiService {

    @POST("exec")
    suspend fun findInspectionRecords(
        @Query("action") action: String = "findInspectionRecords",
        @Query("hojaDeRuta") hojaDeRuta: String
    ): GoogleSheetsResponse

    @POST("exec")
    suspend fun updateInspection(
        @Query("action") action: String = "updateInspection",
        @Query("uniqueId") uniqueId: String,
        @Query("tipoCalidad") tipoCalidad: String,
        @Query("tipoDeFalla") tipoDeFalla: String?,
        @Query("metrosDeTela") metrosDeTela: Double
    ): UpdateInspectionResponse

    @POST("exec")
    suspend fun addInspection(
        @Query("action") action: String = "addInspection",
        @Query("usuario") usuario: String,
        @Query("hojaDeRuta") hojaDeRuta: String,
        @Query("fecha") fecha: String,
        @Query("tejeduria") tejeduria: String,
        @Query("telar") telar: Int,
        @Query("tintoreria") tintoreria: Int,
        @Query("articulo") articulo: String,
        @Query("color") color: Int,
        @Query("rolloDeUrdido") rolloDeUrdido: Int,
        @Query("orden") orden: String,
        @Query("cadena") cadena: Int,
        @Query("anchoDeRollo") anchoDeRollo: Int,
        @Query("esmerilado") esmerilado: String,
        @Query("ignifugo") ignifugo: String,
        @Query("impermeable") impermeable: String,
        @Query("otro") otro: String,
        @Query("tipoCalidad") tipoCalidad: String,
        @Query("tipoDeFalla") tipoDeFalla: String?,
        @Query("metrosDeTela") metrosDeTela: Double,
        @Query("uniqueId") uniqueId: String
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
        @Field("uniqueId") uniqueId: String, // <-- AGREGADO
        @Field("nuevaTonalidad") nuevaTonalidad: String, // <-- AGREGADO
        @Field("usuario") usuario: String // <-- AGREGADO
    ): ApiResponse

    // Nueva función para el inicio de sesión
    @FormUrlEncoded
    @POST("exec")
    suspend fun checkUserCredentials(
        @Field("action") action: String = "checkUserCredentials",
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<ApiResponse>

    @FormUrlEncoded
    @POST("exec")
    suspend fun getArticulos(
        @Field("action") action: String = "getArticulos"
    ): Response<ArticulosResponse>

    // Añade la nueva función para obtener los tipos de fallas
    @FormUrlEncoded
    @POST("exec")
    suspend fun getTiposDeFallas(
        @Field("action") action: String = "getTiposDeFallas"
    ): Response<TiposDeFallaResponse>

    @FormUrlEncoded
    @POST("exec")
    suspend fun getTejedurias(
        @Field("action") action: String = "getTejedurias"
    ): Response<TejeduriasResponse>

    // Nueva función para obtener telares
    @FormUrlEncoded
    @POST("exec")
    suspend fun getTelares(
        @Field("action") action: String = "getTelares"
    ): Response<TelaresResponse>

    @FormUrlEncoded
    @POST("exec")
    suspend fun getHojasDeRutaExistentes(
        @Field("action") action: String = "getHojasDeRutaExistentes"
    ): Response<HojasDeRutaReponse>
}

// Objeto singleton para acceder al servicio de red
object GoogleSheetsApi2 {
    private const val BASE_URL = "https://script.google.com/macros/s/AKfycbz1Dl1Fc8zUf-OFZMeuWmVF6gTcT4EWwDWte0FLna3gTzm6ZZj0IJ3mKsDiR0cjs8JCOg/"

    val service: GoogleSheetsApiService by lazy {

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // Aumenta el tiempo de espera de conexión a 30 segundos
            .readTimeout(60, TimeUnit.SECONDS) // Aumenta el tiempo de espera de lectura a 60 segundos
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient) // Asigna el cliente OkHttpClient personalizado
            .build()
            .create(GoogleSheetsApiService::class.java)
    }
}