package com.example.ibero.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tipos_de_falla")
data class TipoDeFalla(
    @PrimaryKey
    val nombre: String
)