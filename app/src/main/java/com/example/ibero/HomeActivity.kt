package com.example.ibero

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
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
import androidx.core.content.ContextCompat
import com.example.ibero.data.TonalidadDao
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Observer
import androidx.room.InvalidationTracker

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
    private lateinit var tonalidadDao: TonalidadDao

    private lateinit var inspectionViewModel: InspectionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Inicialización de la base de datos y el DAO
        inspectionDao = AppDatabase.getDatabase(this).inspectionDao()
        tonalidadDao = AppDatabase.getDatabase(this).tonalidadDao()

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


        inspectionViewModel.isSyncing.observe(this, Observer { isSyncing ->
            val colorResId = if (isSyncing) {
                R.color.magenta // Color para "Sincronizando..."
            } else {
                R.color.verde // Color para "Sincronización finalizada"
            }
            btnSync.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, colorResId))

            if(isSyncing){
                // Se asigna el texto cuando la sincronización está en curso
                btnSync.text = "Sincronizando..."
                textSyncStatus.text = "Sincronizando..."
            } else {
                // Se asigna el texto cuando la sincronización ha terminado
                btnSync.text = "Sincronizar"
                textSyncStatus.text = "Sin registros por sincronizar."
            }

            btnSync.isEnabled = !isSyncing
        })

        btnSync.setOnClickListener {
            lifecycleScope.launch {
                inspectionViewModel.performSync()
            }
        }


        // Configuración de los listeners de los botones
        setupButtonListeners()
        chequearConexion()
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
            if(isNetworkAvailable(this@HomeActivity)){
                inspectionViewModel.performSync()
                textSyncStatus.text = "Sin registros a sincronizar."
                btnSync.text = "Sin registros a sincronizar"
                btnSync.setTextColor(Color.BLACK)
                btnSync.isEnabled = false
                btnSync.setBackgroundColor(Color.GREEN)
            }
            chequearConexion()
        }
    }

    private fun chequearConexion(){
        if (!isNetworkAvailable(this@HomeActivity)) {
            btnUpdateTonalidad.isEnabled = false
            btnViewInspections.isEnabled = false
            btnUpdateTonalidad.setBackgroundColor(Color.GRAY)
            btnViewInspections.setBackgroundColor(Color.GRAY)
        } else {
            btnUpdateTonalidad.isEnabled = true
            btnViewInspections.isEnabled = true
            btnUpdateTonalidad.backgroundTintList = ColorStateList.valueOf("#2196F3".toColorInt())
            btnViewInspections.backgroundTintList = ColorStateList.valueOf("#FF9800".toColorInt())
        }
    }

    private fun checkSyncStatus() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                textSyncStatus.visibility =View.VISIBLE
                btnSync.visibility = View.VISIBLE
                // Obtener el conteo de registros no sincronizados
                val unsyncedCount = inspectionDao.getUnsyncedCount() + tonalidadDao.getUnsyncedCount()

                withContext(Dispatchers.Main) {
                    if (unsyncedCount > 0) {
                        textSyncStatus.text = "¡Cuidado! Hay $unsyncedCount registros pendientes. No cierres la app sin sincronizar."
                        btnSync.setBackgroundColor(Color.RED)
                        if (isNetworkAvailable(this@HomeActivity)) {
                            // Si hay conexión, muestra el botón de sincronizar
                            btnSync.setBackgroundColor(Color.MAGENTA)
                            btnSync.setTextColor(Color.WHITE)
                            btnSync.setText("Sincronizar")
                            return@withContext
                        } else {
                            // Si no hay conexión, muestra un mensaje de error
                            btnSync.setText("Sin conexión a internet")
                            return@withContext
                        }
                    } else {
                        // Oculta los elementos si no hay nada que sincronizar
                        textSyncStatus.text = "Sin registros por sincronizar."
                        btnSync.setBackgroundColor(Color.GREEN)
                        btnSync.setTextColor(Color.BLACK)
                        btnSync.setText("Nada por sincronizar")
                        btnSync.isEnabled = false
                        return@withContext
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