package com.example.ibero.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    // Convierte un Long (timestamp) a un objeto Date
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    // Convierte un objeto Date a un Long (timestamp)
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // Convierte una cadena JSON a una lista de cadenas
    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        if (value == null) {
            return null
        }
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    // Convierte una lista de cadenas a una cadena JSON
    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        if (list == null) {
            return null
        }
        return Gson().toJson(list)
    }
}