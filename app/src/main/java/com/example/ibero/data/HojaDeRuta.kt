package com.example.ibero.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.annotation.NonNull

@Entity(tableName = "hojas_de_ruta")
data class HojaDeRuta(
    @PrimaryKey @NonNull val nombre: String
)
