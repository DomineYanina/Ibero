package com.example.ibero.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.ibero.data.Inspection

@Database(entities = [Inspection::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    // Define el DAO para acceder a las operaciones de la tabla inspections
    abstract fun inspectionDao(): InspectionDao

    companion object {
        @Volatile // Hace que la instancia sea visible para todos los hilos
        private var INSTANCE: AppDatabase? = null

        //Método para obtener la instancia única de la base de datos (Singleton)
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) { // Bloquea para asegurar una sola instancia
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ibero_database" // Nombre del archivo de la base de datos
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}