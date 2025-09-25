package com.example.ibero.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tejedurias")
data class Tejeduria(
    @PrimaryKey
    val nombre: String
)