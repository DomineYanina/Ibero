package com.example.ibero.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "tonalidades")
data class Tonalidad(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uniqueId: String, // Corresponde al rowNumber de Google Sheets
    val valorHojaDeRutaId: String,
    val nuevaTonalidad: String,
    val usuario: String,
    val isSynced: Boolean = false,
    val dateRegistered: Date = Date()
)