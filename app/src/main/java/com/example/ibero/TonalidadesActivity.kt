package com.example.ibero

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
import com.example.ibero.data.network.GoogleSheetsApi
import com.example.ibero.ui.TonalidadesAdapter
import com.example.ibero.data.TonalidadItem
import com.google.android.material.textfield.TextInputEditText // Importa el tipo correcto
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent

class TonalidadesActivity : AppCompatActivity() {

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

        initViews()
        setupListeners()
        setupRecyclerView()
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
                val response = GoogleSheetsApi.service.findTonalidades(hojaDeRuta = hojaRuta)

                withContext(Dispatchers.Main) {
                    if (response.status == "success" && !response.data.isNullOrEmpty()) {
                        // ... (mapeo de datos, sin cambios aquí)
                        val uiItems = response.data!!.map { dataItem ->
                            val tonalidadPrevia = dataItem.valorColumnaV
                            TonalidadItem(
                                uniqueId = dataItem.rowNumber.toString(),
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
                    // Finaliza el estado de carga
                    toggleLoadingState(false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TonalidadesActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("TonalidadesActivity", "Error al buscar registros", e)
                    adapter.updateList(mutableListOf())
                    btnGuardar.visibility = View.GONE
                    // Finaliza el estado de carga en caso de error
                    toggleLoadingState(false)
                }
            }
        }
    }

    private fun guardarTonalidades() {
        val registrosAActualizar = adapter.getUpdatedItems()
        if (registrosAActualizar.isEmpty()) {
            Toast.makeText(this, "No hay tonalidades para guardar.", Toast.LENGTH_SHORT).show()
            // Finaliza el estado de carga si no hay nada que guardar
            toggleLoadingState(false)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // CAMBIO 2: Incluir el nombre del usuario en el objeto de actualización
                val updates = registrosAActualizar.map {
                    mapOf(
                        "rowNumber" to it.uniqueId.toInt(),
                        "nuevaTonalidad" to it.nuevaTonalidad,
                        "usuario" to (loggedInUser ?: "Usuario Desconocido") // Añade el usuario aquí
                    )
                }

                val gson = Gson()
                val updatesJson = gson.toJson(updates)
                val response = GoogleSheetsApi.service.updateTonalidades(updates = updatesJson)

                withContext(Dispatchers.Main) {
                    if (response.status == "success") {
                        Toast.makeText(this@TonalidadesActivity, "Tonalidades guardadas exitosamente.", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@TonalidadesActivity, "Error al guardar: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                    // Finaliza el estado de carga
                    toggleLoadingState(false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TonalidadesActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("TonalidadesActivity", "Error al guardar registros", e)
                    // Finaliza el estado de carga en caso de error
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
