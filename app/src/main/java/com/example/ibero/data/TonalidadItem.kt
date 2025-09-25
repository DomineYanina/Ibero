package com.example.ibero.data

data class TonalidadItem(
    val uniqueId: String, // Usado para identificar el registro (número de fila)
    val valorHojaDeRutaId: String, // Valor de la columna A
    val tonalidadPrevia: String?, // <-- ¡NUEVO CAMPO! Almacena el valor de la Columna V
    val isEditable: Boolean, // <-- ¡NUEVO CAMPO! Para controlar si el campo está deshabilitado
    var nuevaTonalidad: String
)