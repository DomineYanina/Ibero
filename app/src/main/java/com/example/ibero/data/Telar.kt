package com.example.ibero.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telares")
data class Telar(
    @PrimaryKey
    val numero: Int
)