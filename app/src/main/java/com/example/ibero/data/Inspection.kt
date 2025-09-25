package com.example.ibero.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "inspections")
data class Inspection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Campos del formulario de registro (Parte 1)
    val usuario: String,
    val fecha: Date,
    val hojaDeRuta: String,
    val tejeduria: String,
    val telar: Int,
    val tintoreria: Int,
    val articulo: String,
    val color: Int,
    val rolloDeUrdido: Int,
    val orden: String,
    val cadena: Int,
    val anchoDeRollo: Int, // Ancho de rollo de la primera pantalla
    val esmerilado: String,
    val ignifugo: String,
    val impermeable: String,
    val otro: String,

    // Campos de la segunda parte del formulario (Parte 2)
    val tipoCalidad: String,
    val tipoDeFalla: String?, // Puede ser nulo si TipoCalidad es "Primera"
    val metrosDeTela: Double, // Renombrado de anchoDeRolloParte2

    // Campos para la sincronización con Google Sheets
    val isSynced: Boolean = false,
    val uniqueId: String,
    val imagePaths: List<String> = emptyList(),
    val imageUrls: List<String> = emptyList(),
)

// Función de extensión para convertir un objeto Inspection a HistoricalInspection
fun Inspection.toHistoricalInspection(): HistoricalInspection {
    return HistoricalInspection(
        uniqueId = this.uniqueId,
        tipoCalidad = this.tipoCalidad,
        tipoDeFalla = this.tipoDeFalla,
        metrosDeTela = this.metrosDeTela,
        hojaDeRuta = this.hojaDeRuta,
        articulo = this.articulo,
        color = this.color.toDouble(),
        cadena = this.cadena.toDouble(),
        anchoDeRollo = this.anchoDeRollo.toDouble(),
        esmerilado = this.esmerilado,
        ignifugo = this.ignifugo,
        impermeable = this.impermeable,
        otro = this.otro,
        fecha = this.fecha.toString(),
        usuario = this.usuario,
        tejeduria = this.tejeduria
    )
}