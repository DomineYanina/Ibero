package com.example.ibero.data

// Nuevo modelo para mostrar los datos del historial en el RecyclerView
data class HistoricalInspection(
    val hojaDeRuta: String?,
    val articulo: String?,
    val tipoCalidad: String?,
    val tipoDeFalla: String?,
    val metrosDeTela: Double?,
    val fecha: String?
)
