package com.example.ibero.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Inspection::class, Articulo::class, TipoDeFalla::class, Tejeduria::class, Telar::class, HojaDeRuta::class, Tonalidad::class],
    version = 6, // **IMPORTANTE: Incrementa la versión a 6 o superior**
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun inspectionDao(): InspectionDao
    abstract fun articuloDao(): ArticuloDao
    abstract fun tipoDeFallaDao(): TipoDeFallaDao
    abstract fun tejeduriaDao(): TejeduriaDao
    abstract fun telarDao(): TelarDao
    abstract fun hojaDeRutaDao(): HojaDeRutaDao
    abstract fun tonalidadDao(): TonalidadDao // <-- NUEVO DAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inspection_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}