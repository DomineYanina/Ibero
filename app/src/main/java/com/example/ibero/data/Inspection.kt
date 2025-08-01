package com.example.ibero.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "inspections")
data class Inspection( // <--- ¡AQUÍ ES DONDE VAN LAS PROPIEDADES!
    @PrimaryKey(autoGenerate = true) // Genera automáticamente un ID único para cada inspección
    val id: Long = 0, // ID único de la inspección en la base de datos local

    val inspectionDate: Date, // Fecha de la inspección
    val inspectionTime: String, // Hora de la inspección (formato HH:mm)
    val inspectorName: String, // Nombre del inspector/usuario
    val orderNumber: String, // Número de pedido/lote
    val articleReference: String, // Referencia del artículo/producto
    val supplier: String, // Proveedor
    val color: String, // Color del producto
    val totalLotQuantity: Int, // Cantidad total del lote inspeccionado
    val sampleQuantity: Int, // Cantidad de muestra inspeccionada
    val defectType: String, // Tipo de defecto (ej. "Mancha", "Hebra Suelta", "Otro")
    val otherDefectDescription: String?, // Descripción si el tipo de defecto es "Otro"
    val defectiveItemsQuantity: Int, // Cantidad de artículos con defecto
    val defectDescription: String, // Descripción del defecto/observaciones
    val actionTaken: String, // Acción tomada (ej. "Aprobado", "Rechazado")
    val imagePaths: List<String>, // Rutas locales de las imágenes capturadas (se serializará a JSON)
    val imageUrls: List<String>, // URLs de las imágenes subidas a Google Drive (se serializará a JSON)
    val isSynced: Boolean = false, // Indica si la inspeEcción ya ha sido sincronizada con Google Sheets
    val uniqueId: String // ID único para evitar duplicados en Google Sheets
) // <--- ¡Cierra el paréntesis aquí, no hay '{' después del constructor principal para las propiedades!
{
    // El cuerpo de la clase (si necesitas métodos, inicializadores, etc.) va aquí
    // pero NO las propiedades declaradas en el constructor primario
}