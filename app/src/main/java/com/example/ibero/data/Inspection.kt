package com.example.ibero.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "inspections")
data class Inspection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Nuevos campos del formulario de registro
    val usuario: String,
    val fecha: Date,
    val hojaDeRuta: String, // Se almacena como String después del formato
    val tejeduria: String,
    val telar: Int,
    val tintoreria: Int,
    val articulo: String, // Se almacena como String para mayor flexibilidad con los valores

    // Nuevos campos de la segunda parte del formulario
    val tipoCalidad: String,
    val tipoDeFalla: String?, // Puede ser nulo si TipoCalidad es "Primera"
    val anchoDeRollo: Double, // Se cambia a Double para permitir valores decimales

    // Campos para la sincronización con Google Sheets
    val isSynced: Boolean = false,
    val uniqueId: String,
    val imagePaths: List<String> = emptyList(), // Rutas locales de las imágenes
    val imageUrls: List<String> = emptyList(), // URLs de las imágenes subidas a Google Sheets
)
