package com.example.ibero

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ibero.data.AppDatabase
import com.example.ibero.data.Articulo
import com.example.ibero.data.Tejeduria
import com.example.ibero.data.Telar
import com.example.ibero.data.TipoDeFalla
import com.example.ibero.data.network.GoogleSheetsApi2
import com.example.ibero.data.network.GoogleSheetsApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {

    private lateinit var btnRegisterInspection: Button
    private lateinit var btnUpdateTonalidad: Button
    private lateinit var btnViewInspections: Button
    private lateinit var apiService: GoogleSheetsApiService
    private lateinit var database: AppDatabase
    private lateinit var textWelcome: TextView
    private lateinit var loggedInUser: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        apiService = GoogleSheetsApi2.service
        database = AppDatabase.getDatabase(this)

        btnRegisterInspection = findViewById(R.id.btn_new_record)
        btnUpdateTonalidad = findViewById(R.id.btn_agregar_tonalidades)
        btnViewInspections = findViewById(R.id.btn_continuar_carga)
        textWelcome = findViewById(R.id.text_welcome)

        // Obtiene el nombre de usuario del Intent y actualiza el TextView
        loggedInUser = intent.getStringExtra("LOGGED_IN_USER") ?: "Usuario Desconocido"
        textWelcome.text = "Bienvenido, $loggedInUser"

        // Se puede sincronizar al iniciar la actividad
        synchronizeInitialData()

        btnRegisterInspection.setOnClickListener {
            val intent = Intent(this, PrimerRegistroActivity::class.java)
            intent.putExtra("LOGGED_IN_USER", loggedInUser) // Añade el nombre de usuario al Intent
            startActivity(intent)
        }

        btnUpdateTonalidad.setOnClickListener {
            val intent = Intent(this, TonalidadesActivity::class.java)
            intent.putExtra("LOGGED_IN_USER", loggedInUser) // Añade el nombre de usuario al Intent
            startActivity(intent)
        }

        btnViewInspections.setOnClickListener {
            val intent = Intent(this, ContinuarCargaActivity::class.java)
            intent.putExtra("LOGGED_IN_USER", loggedInUser) // Añade el nombre de usuario al Intent
            startActivity(intent)
        }
    }

    private fun synchronizeInitialData() {
        Log.d("HomeActivity", "Iniciando sincronización de datos...")
        Toast.makeText(this, "Sincronizando datos de referencia...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Sincronizar Artículos
                val articulosResponse = apiService.getArticulos()
                if (articulosResponse.isSuccessful) {
                    val articulos = articulosResponse.body()?.data?.articulos?.map { Articulo(nombre = it) }
                    articulos?.let {
                        database.articuloDao().deleteAll()
                        database.articuloDao().insertAll(it)
                        withContext(Dispatchers.Main) { Log.d("HomeActivity", "Artículos sincronizados.") }
                    }
                }

                // Sincronizar Tipos de Fallas
                val fallasResponse = apiService.getTiposDeFallas()
                if (fallasResponse.isSuccessful) {
                    val tiposDeFalla = fallasResponse.body()?.data?.tiposDeFallas?.map { TipoDeFalla(nombre = it) }
                    tiposDeFalla?.let {
                        database.tipoDeFallaDao().deleteAll()
                        database.tipoDeFallaDao().insertAll(it)
                        withContext(Dispatchers.Main) { Log.d("HomeActivity", "Tipos de falla sincronizados.") }
                    }
                }

                // Sincronizar Tejedurías
                val tejeduriasResponse = apiService.getTejedurias()
                if (tejeduriasResponse.isSuccessful) {
                    val tejedurias = tejeduriasResponse.body()?.data?.tejedurias?.map { Tejeduria(nombre = it) }
                    tejedurias?.let {
                        database.tejeduriaDao().deleteAll()
                        database.tejeduriaDao().insertAll(it)
                        withContext(Dispatchers.Main) { Log.d("HomeActivity", "Tejedurías sincronizadas.") }
                    }
                }

                // Sincronizar Telares
                val telaresResponse = apiService.getTelares()
                if (telaresResponse.isSuccessful) {
                    val telares = telaresResponse.body()?.data?.telares?.map { Telar(numero = it) }
                    telares?.let {
                        database.telarDao().deleteAll()
                        database.telarDao().insertAll(it)
                        withContext(Dispatchers.Main) { Log.d("HomeActivity", "Telares sincronizados.") }
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HomeActivity, "Sincronización de datos completada.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("HomeActivity", "Error de sincronización: ${e.message}")
                    Toast.makeText(this@HomeActivity, "Error al sincronizar datos.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
