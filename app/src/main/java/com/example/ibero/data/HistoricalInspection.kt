package com.example.ibero.data

import com.google.gson.annotations.SerializedName

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
    @SerializedName("valorColumnaU") val metrosDeTela: Double? = null
)
