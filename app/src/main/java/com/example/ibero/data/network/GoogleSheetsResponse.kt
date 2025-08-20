package com.example.ibero.data.network

import com.google.gson.annotations.SerializedName
import com.example.ibero.data.HistoricalInspection

/**
 * Representa la estructura externa de la respuesta de la API, que contiene un objeto 'data' anidado.
 */
data class GoogleSheetsResponse(
    val status: String,
    val message: String,
    val data: InspectionDataResponse
)

data class InspectionDataResponse(
    @SerializedName("data") val records: List<HistoricalInspection>
)
