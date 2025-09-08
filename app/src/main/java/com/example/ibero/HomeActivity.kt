package com.example.ibero

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.ibero.data.AppDatabase
import com.example.ibero.data.InspectionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

class HomeActivity : AppCompatActivity() {

    private lateinit var btnRegisterInspection: Button
    private lateinit var btnUpdateTonalidad: Button
    private lateinit var btnViewInspections: Button
    private lateinit var textWelcome: TextView
    private lateinit var loggedInUser: String
    private lateinit var textSyncStatus: TextView
    private lateinit var btnSync: Button

    // Inyecta tu DAO. Si no usas Hilt, inicialízalo manualmente en onCreate.
    private lateinit var inspectionDao: InspectionDao

    private lateinit var inspectionViewModel: InspectionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Inicialización de la base de datos y el DAO
        inspectionDao = AppDatabase.getDatabase(this).inspectionDao()

        val factory = InspectionViewModelFactory(application)
        inspectionViewModel = ViewModelProvider(this, factory)[InspectionViewModel::class.java]

        // Inicialización de las vistas
        btnRegisterInspection = findViewById(R.id.btn_new_record)
        btnUpdateTonalidad = findViewById(R.id.btn_agregar_tonalidades)
        btnViewInspections = findViewById(R.id.btn_continuar_carga)
        textWelcome = findViewById(R.id.text_welcome)
        textSyncStatus = findViewById(R.id.text_sync_status)
        btnSync = findViewById(R.id.btn_sync)

        loggedInUser = intent.getStringExtra("LOGGED_IN_USER") ?: "Usuario Desconocido"
        textWelcome.text = "Bienvenido, $loggedInUser"

        // Configuración de los listeners de los botones
        setupButtonListeners()
    }

    override fun onResume() {
        super.onResume()
        // Verificar el estado de la sincronización cada vez que la actividad se reanude
        checkSyncStatus()
    }

    private fun setupButtonListeners() {
        btnRegisterInspection.setOnClickListener {
            val intent = Intent(this, PrimerRegistroActivity::class.java).apply {
                putExtra("LOGGED_IN_USER", loggedInUser)
            }
            startActivity(intent)
        }

        btnUpdateTonalidad.setOnClickListener {
            val intent = Intent(this, TonalidadesActivity::class.java).apply {
                putExtra("LOGGED_IN_USER", loggedInUser)
            }
            startActivity(intent)
        }

        btnViewInspections.setOnClickListener {
            val intent = Intent(this, ContinuarCargaActivity::class.java).apply {
                putExtra("LOGGED_IN_USER", loggedInUser)
            }
            startActivity(intent)
        }

        btnSync.setOnClickListener {
            inspectionViewModel.performSync()
            textSyncStatus.text = "Sin registros a sincronizar."
            btnSync.text = "Sincronizar"
            btnSync.isEnabled = false
            btnSync.setBackgroundColor(Color.GREEN)
        }
    }

    private fun checkSyncStatus() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                textSyncStatus.visibility =View.VISIBLE
                btnSync.visibility = View.VISIBLE
                // Obtener el conteo de registros no sincronizados
                val unsyncedCount = inspectionDao.getUnsyncedCount()

                withContext(Dispatchers.Main) {
                    if (unsyncedCount > 0) {
                        textSyncStatus.text = "Tienes $unsyncedCount registros sin sincronizar."
                        btnSync.setBackgroundColor(Color.RED)
                        if (isNetworkAvailable(this@HomeActivity)) {
                            // Si hay conexión, muestra el botón de sincronizar
                            btnSync.setText("Sincronizar")
                        } else {
                            // Si no hay conexión, muestra un mensaje de error
                            btnSync.setText("Sin conexión a internet")
                        }
                    } else {
                        // Oculta los elementos si no hay nada que sincronizar
                        textSyncStatus.text = "Tienes $unsyncedCount registros sin sincronizar."
                        btnSync.setBackgroundColor(Color.GREEN)
                        btnSync.setText("Nada por sincronizar")
                        btnSync.isEnabled = false
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "Error al verificar el estado de sincronización", e)
                withContext(Dispatchers.Main) {
                    // Oculta los elementos en caso de error
                    btnSync.setBackgroundColor(Color.RED)
                    btnSync.visibility = View.VISIBLE
                }
            }
        }
    }
}

// Función auxiliar para verificar la conexión a internet
fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = connectivityManager.activeNetworkInfo
    return activeNetwork?.isConnectedOrConnecting == true
}