package com.example.ibero

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import android.widget.ProgressBar
import android.widget.LinearLayout // Importa LinearLayout para el contenedor del formulario
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ibero.data.network.GoogleSheetsApi2
import com.example.ibero.ui.TonalidadesAdapter
import com.example.ibero.data.TonalidadItem
import com.google.android.material.textfield.TextInputEditText // Importa el tipo correcto
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.net.ConnectivityManager
import androidx.lifecycle.ViewModelProvider
import com.example.ibero.data.Tonalidad

class TonalidadesActivity : AppCompatActivity() {

    private lateinit var viewModel: InspectionViewModel

    private lateinit var editHojaRuta: TextInputEditText // Usamos el tipo correcto
    private lateinit var btnBuscar: Button
    private lateinit var btnGuardar: Button
    private lateinit var btnCancelar: Button // Nuevo: Botón de cancelar
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TonalidadesAdapter
    private lateinit var progressBar: ProgressBar // Referencia a la nueva barra de progreso
    private lateinit var formContainer: LinearLayout // Referencia al nuevo contenedor

    // CAMBIO 1: Variable para guardar el nombre del usuario logeado
    private var loggedInUser: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tonalidades)

        // Obtener el nombre de usuario del Intent
        loggedInUser = intent.getStringExtra("LOGGED_IN_USER")

        val factory = InspectionViewModelFactory(application) // Usa el mismo factory
        viewModel = ViewModelProvider(this, factory).get(InspectionViewModel::class.java)

        initViews()
        setupListeners()
        setupRecyclerView()

        // Observa los mensajes de sincronización del ViewModel
        viewModel.syncMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearSyncMessage()
            }
        }
    }

    private fun initViews() {
        // Obtenemos las referencias a los nuevos elementos del layout
        progressBar = findViewById(R.id.progress_bar_loading)
        formContainer = findViewById(R.id.form_container)

        // Usamos TextInputEditText para evitar el ClassCastException
        editHojaRuta = findViewById(R.id.edit_hoja_ruta_tonalidad)
        btnBuscar = findViewById(R.id.btn_buscar_tonalidades)
        btnGuardar = findViewById(R.id.btn_guardar_tonalidades)
        btnCancelar = findViewById(R.id.btn_cancelar_tonalidades) // Nuevo: Inicializar el botón de cancelar
        recyclerView = findViewById(R.id.recycler_view_tonalidades)
    }

    private fun setupListeners() {
        btnBuscar.setOnClickListener {
            val hojaRuta = editHojaRuta.text.toString().trim()
            if (hojaRuta.isEmpty()) {
                Toast.makeText(this, "Por favor, ingrese una hoja de ruta.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Inicia el estado de carga al iniciar la búsqueda
            toggleLoadingState(true)
            buscarRegistrosPorHojaRuta(hojaRuta)
        }

        btnGuardar.setOnClickListener {
            // Inicia el estado de carga al iniciar el guardado
            toggleLoadingState(true)
            guardarTonalidades()
        }

        btnCancelar.setOnClickListener {
            val loggedInUser = intent.getStringExtra("LOGGED_IN_USER") ?: "Usuario Desconocido"
            val homeIntent = Intent(this, HomeActivity::class.java)
            homeIntent.putExtra("LOGGED_IN_USER", loggedInUser)

            startActivity(homeIntent)
            finish()
        }
    }

    private fun setupRecyclerView() {
        // Se inicializa el adaptador con una lista vacía
        adapter = TonalidadesAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun buscarRegistrosPorHojaRuta(hojaRuta: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // CAMBIO: La respuesta ahora contiene 'uniqueId' en lugar de 'rowNumber'
                val response = GoogleSheetsApi2.service.findTonalidades(hojaDeRuta = hojaRuta)

                withContext(Dispatchers.Main) {
                    if (response.status == "success" && !response.data.isNullOrEmpty()) {
                        val uiItems = response.data!!.map { dataItem ->
                            val tonalidadPrevia = dataItem.valorColumnaV
                            // CAMBIO: Usar dataItem.uniqueId para el 'uniqueId'
                            TonalidadItem(
                                uniqueId = dataItem.uniqueId ?: "",
                                valorHojaDeRutaId = dataItem.valorColumnaA ?: "",
                                tonalidadPrevia = tonalidadPrevia,
                                isEditable = tonalidadPrevia.isNullOrBlank(),
                                nuevaTonalidad = tonalidadPrevia ?: ""
                            )
                        }.toMutableList()

                        adapter.updateList(uiItems)
                        btnGuardar.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(this@TonalidadesActivity, "Error en la búsqueda: ${response.message}", Toast.LENGTH_LONG).show()
                        adapter.updateList(mutableListOf())
                        btnGuardar.visibility = View.GONE
                    }
                    toggleLoadingState(false)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TonalidadesActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("TonalidadesActivity", "Error al buscar registros", e)
                    adapter.updateList(mutableListOf())
                    btnGuardar.visibility = View.GONE
                    toggleLoadingState(false)
                }
            }
        }
    }

    private fun guardarTonalidades() {
        val registrosAActualizar = adapter.getUpdatedItems()
        if (registrosAActualizar.isEmpty()) {
            Toast.makeText(this, "No hay tonalidades para guardar.", Toast.LENGTH_SHORT).show()
            return
        }

        toggleLoadingState(true)

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isConnected = connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isConnected) {
                    val successfulUpdates = mutableListOf<Tonalidad>()
                    val failedUpdates = mutableListOf<Tonalidad>()

                    for (item in registrosAActualizar) {
                        try {
                            val response = GoogleSheetsApi2.service.updateTonalidades(
                                uniqueId = item.uniqueId,
                                nuevaTonalidad = item.nuevaTonalidad,
                                usuario = loggedInUser ?: "Usuario Desconocido"
                            )

                            if (response.status == "success") {
                                val tonalidad = Tonalidad(
                                    uniqueId = item.uniqueId,
                                    valorHojaDeRutaId = item.valorHojaDeRutaId,
                                    nuevaTonalidad = item.nuevaTonalidad,
                                    usuario = loggedInUser ?: "Usuario Desconocido",
                                    isSynced = true
                                )
                                viewModel.insertTonalidad(tonalidad)
                                successfulUpdates.add(tonalidad)
                            } else {
                                val tonalidad = Tonalidad(
                                    uniqueId = item.uniqueId,
                                    valorHojaDeRutaId = item.valorHojaDeRutaId,
                                    nuevaTonalidad = item.nuevaTonalidad,
                                    usuario = loggedInUser ?: "Usuario Desconocido",
                                    isSynced = false
                                )
                                viewModel.insertTonalidad(tonalidad)
                                failedUpdates.add(tonalidad)
                            }
                        } catch (e: Exception) {
                            val tonalidad = Tonalidad(
                                uniqueId = item.uniqueId,
                                valorHojaDeRutaId = item.valorHojaDeRutaId,
                                nuevaTonalidad = item.nuevaTonalidad,
                                usuario = loggedInUser ?: "Usuario Desconocido",
                                isSynced = false
                            )
                            viewModel.insertTonalidad(tonalidad)
                            failedUpdates.add(tonalidad)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        val message = "Tonalidades guardadas: ${successfulUpdates.size} subidas a la nube, ${failedUpdates.size} guardadas localmente."
                        Toast.makeText(this@TonalidadesActivity, message, Toast.LENGTH_LONG).show()
                        finish()
                    }

                } else {
                    registrosAActualizar.forEach { item ->
                        val tonalidad = Tonalidad(
                            uniqueId = item.uniqueId,
                            valorHojaDeRutaId = item.valorHojaDeRutaId,
                            nuevaTonalidad = item.nuevaTonalidad,
                            usuario = loggedInUser ?: "Usuario Desconocido",
                            isSynced = false
                        )
                        viewModel.insertTonalidad(tonalidad)
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TonalidadesActivity, "Sin conexión a Internet. Tonalidades guardadas localmente. Se sincronizarán automáticamente.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TonalidadesActivity, "Error al procesar registros: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("TonalidadesActivity", "Error al procesar registros", e)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    toggleLoadingState(false)
                }
            }
        }
    }

    /**
     * Alterna la visibilidad y el estado del formulario y la barra de progreso.
     * @param isLoading Si es true, el formulario se deshabilita y la barra de progreso se muestra.
     */
    private fun toggleLoadingState(isLoading: Boolean) {
        // Deshabilita/habilita los campos y botones
        editHojaRuta.isEnabled = !isLoading
        btnBuscar.isEnabled = !isLoading
        btnGuardar.isEnabled = !isLoading
        btnCancelar.isEnabled = !isLoading // Nuevo: Habilitar/deshabilitar botón de cancelar

        // El RecyclerView no tiene una propiedad `isEnabled`, pero puedes cambiar su opacidad
        formContainer.alpha = if (isLoading) 0.5f else 1.0f

        // Muestra/oculta la barra de progreso
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
