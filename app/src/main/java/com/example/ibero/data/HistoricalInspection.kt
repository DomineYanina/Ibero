package com.example.ibero.data

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data class to represent a historical inspection record from the Google Sheets API.
 * Uses @SerializedName to map the API's column names to more descriptive field names.
 */
data class HistoricalInspection(
    @SerializedName("valorColumnaA") val usuario: String? = null,
    @SerializedName("valorColumnaC") val fecha: String,
    @SerializedName("valorColumnaD") val hojaDeRuta: String,
    @SerializedName("valorColumnaE") val tejeduria: String? = null,
    @SerializedName("valorColumnaF") val telar: Double? = null,
    @SerializedName("valorColumnaG") val tintoreria: Double? = null,
    @SerializedName("valorColumnaH") val articulo: String,
    @SerializedName("valorColumnaI") val color: Double? = null,
    @SerializedName("valorColumnaJ") val rolloDeUrdido: Double? = null,
    @SerializedName("valorColumnaK") val orden: String? = null,
    @SerializedName("valorColumnaL") val cadena: Double? = null,
    @SerializedName("valorColumnaM") val anchoDeRollo: Double? = null,
    @SerializedName("valorColumnaN") val esmerilado: String? = null,
    @SerializedName("valorColumnaO") val ignifugo: String? = null,
    @SerializedName("valorColumnaP") val impermeable: String? = null,
    @SerializedName("valorColumnaQ") val otro: String? = null,
    @SerializedName("valorColumnaS") val tipoCalidad: String? = null,
    @SerializedName("valorColumnaT") val tipoDeFalla: String? = null,
    @SerializedName("valorColumnaU") val metrosDeTela: Double? = null,
    @SerializedName("valorColumnaW") val uniqueId: String? = null // ¡IMPORTANTE! Agregado para el método de edición.
)

/**
 * Extensión para convertir un objeto HistoricalInspection a un objeto Inspection.
 * Este método se ubica mejor fuera de la clase, idealmente en un archivo llamado ModelsExtensions.kt.
 */
fun HistoricalInspection.toInspection(): Inspection {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val parsedDate = try {
        dateFormat.parse(this.fecha) ?: Date()
    } catch (e: Exception) {
        Date()
    }

    return Inspection(
        id = 0,
        uniqueId = this.uniqueId ?: "",
        usuario = this.usuario ?: "",
        fecha = parsedDate,
        hojaDeRuta = this.hojaDeRuta,
        tejeduria = this.tejeduria ?: "",
        telar = this.telar?.toInt() ?: 0,
        tintoreria = this.tintoreria?.toInt() ?: 0,
        articulo = this.articulo,
        color = this.color?.toInt() ?: 0,
        rolloDeUrdido = this.rolloDeUrdido?.toInt() ?: 0,
        orden = this.orden ?: "",
        cadena = this.cadena?.toInt() ?: 0,
        anchoDeRollo = this.anchoDeRollo?.toInt() ?: 0,
        esmerilado = this.esmerilado ?: "",
        ignifugo = this.ignifugo ?: "",
        impermeable = this.impermeable ?: "",
        otro = this.otro ?: "",
        tipoCalidad = this.tipoCalidad ?: "",
        tipoDeFalla = this.tipoDeFalla,
        metrosDeTela = this.metrosDeTela ?: 0.0,
        isSynced = true,
        imagePaths = emptyList(),
        imageUrls = emptyList()
    )
}