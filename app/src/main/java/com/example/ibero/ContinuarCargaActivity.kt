package com.example.ibero

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ibero.data.Inspection
import com.example.ibero.data.network.GoogleSheetsApi
import com.example.ibero.ui.InspectionHistoryAdapter
import com.example.ibero.data.HistoricalInspection
import com.example.ibero.data.network.InspectionRecordsResponse
import com.example.ibero.ui.InspectionViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ContinuarCargaActivity : AppCompatActivity() {

    private lateinit var viewModel: InspectionViewModel

    // Elementos del formulario de búsqueda
    private lateinit var editHojaRuta: TextInputEditText
    private lateinit var btnBuscar: Button
    private lateinit var layoutHojaRuta: TextInputLayout
    private lateinit var formAndRecordsContainer: View
    private lateinit var hojaRutaContainer: View

    // Elementos del visualizador de registros existentes
    private lateinit var recyclerViewExistingRecords: RecyclerView
    private lateinit var inspectionHistoryAdapter: InspectionHistoryAdapter
    private lateinit var textExistingRecordsTitle: TextView

    // Elementos del formulario de registro
    private lateinit var spinnerTipoCalidad: AutoCompleteTextView
    private lateinit var layoutTipoDeFalla: TextInputLayout
    private lateinit var spinnerTipoDeFalla: AutoCompleteTextView
    private lateinit var editMetrosDeTela: EditText
    private lateinit var btnGuardarRegistro: Button
    private lateinit var btnIncorporar: Button

    // Nuevas variables para el estado de carga
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingOverlay: View

    // Datos de la sesión de carga
    private lateinit var loggedInUser: String
    private lateinit var currentHojaDeRuta: String
    private var isHojaRutaFound: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_continuar_carga)

        val factory = InspectionViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory).get(InspectionViewModel::class.java)

        loggedInUser = intent.getStringExtra("LOGGED_IN_USER") ?: "Usuario Desconocido"

        initViews()
        setupListeners()
        setupSpinners()
        setupRecyclerViews()
    }

    private fun initViews() {
        // Vistas de búsqueda
        editHojaRuta = findViewById(R.id.edit_hoja_ruta_continuar)
        btnBuscar = findViewById(R.id.btn_buscar_continuar)
        layoutHojaRuta = findViewById(R.id.layout_hoja_ruta_continuar)
        hojaRutaContainer = findViewById(R.id.hoja_ruta_container)

        // Vistas de visualización de registros y formulario
        formAndRecordsContainer = findViewById(R.id.form_and_records_container)
        recyclerViewExistingRecords = findViewById(R.id.recycler_view_existing_records)
        textExistingRecordsTitle = findViewById(R.id.text_existing_records_title)

        // Vistas del formulario de registro (similares a SegundoRegistroActivity)
        spinnerTipoCalidad = findViewById(R.id.spinner_tipo_calidad)
        layoutTipoDeFalla = findViewById(R.id.layout_tipo_de_falla)
        spinnerTipoDeFalla = findViewById(R.id.spinner_tipo_de_falla)
        editMetrosDeTela = findViewById(R.id.edit_metros_de_tela)
        btnGuardarRegistro = findViewById(R.id.btn_guardar_registro)
        btnIncorporar = findViewById(R.id.btn_incorporar)

        // Vistas del estado de carga
        progressBar = findViewById(R.id.progress_bar)
        loadingOverlay = findViewById(R.id.loading_overlay)
    }

    private fun setupListeners() {
        btnBuscar.setOnClickListener {
            val hojaRuta = editHojaRuta.text.toString().trim()
            if (hojaRuta.isEmpty()) {
                layoutHojaRuta.error = "Por favor, ingrese una hoja de ruta."
                return@setOnClickListener
            }
            layoutHojaRuta.error = null
            checkAndFetchHojaRuta(hojaRuta)
        }

        btnIncorporar.setOnClickListener {
            if (validateForm()) {
                saveInspectionAndResetForm()
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos obligatorios.", Toast.LENGTH_SHORT).show()
            }
        }

        btnGuardarRegistro.setOnClickListener {
            if (validateForm()) {
                saveInspectionAndFinalize()
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos obligatorios.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSpinners() {
        val tipoCalidadOptions = arrayOf("Primera", "Segunda")
        val tipoCalidadAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, tipoCalidadOptions)
        spinnerTipoCalidad.setAdapter(tipoCalidadAdapter)

        val tipoDeFallaOptions = arrayOf(
            "Aureolas", "Clareadas", "Falla de cadena", "Falla de trama", "Falla de urdido",
            "Gota Espaciada", "Goteras", "Hongos", "Mancha con patrón", "Manchas de aceite",
            "Marcas de sanforizado", "Parada de engomadora", "Parada telar", "Paradas",
            "Quebraduras", "Vainillas"
        )
        val tipoDeFallaAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, tipoDeFallaOptions)
        spinnerTipoDeFalla.setAdapter(tipoDeFallaAdapter)

        spinnerTipoCalidad.setOnItemClickListener { _, _, _, _ ->
            val selectedQuality = spinnerTipoCalidad.text.toString()
            if (selectedQuality == "Segunda") {
                layoutTipoDeFalla.visibility = View.VISIBLE
            } else {
                layoutTipoDeFalla.visibility = View.GONE
                spinnerTipoDeFalla.setText("", false)
            }
        }
    }

    private fun setupRecyclerViews() {
        // Se corrigió el constructor del adaptador
        inspectionHistoryAdapter = InspectionHistoryAdapter(mutableListOf()) { inspection ->
            Toast.makeText(this, "Detalles de: ${inspection.articulo}", Toast.LENGTH_SHORT).show()
        }
        recyclerViewExistingRecords.layoutManager = LinearLayoutManager(this)
        recyclerViewExistingRecords.adapter = inspectionHistoryAdapter
    }

    private fun checkAndFetchHojaRuta(hojaRuta: String) {
        setLoadingState(true)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Primero, verifica la existencia
                val checkResponse = GoogleSheetsApi.service.checkHojaRutaExistence(hojaDeRuta = hojaRuta)
                if (checkResponse.status == "success" && checkResponse.data.exists) {
                    // Si existe, busca los registros
                    val findResponse = GoogleSheetsApi.service.findInspectionRecords(hojaDeRuta = hojaRuta)
                    withContext(Dispatchers.Main) {
                        handleFindRecordsResponse(findResponse)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        setLoadingState(false)
                        editHojaRuta.text?.clear()
                        Toast.makeText(this@ContinuarCargaActivity, "La hoja de ruta no existe. Por favor, verifique el valor ingresado.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setLoadingState(false)
                    Log.e("ContinuarCargaActivity", "Error de conexión o búsqueda", e)
                    Toast.makeText(this@ContinuarCargaActivity, "Error de conexión o búsqueda: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun handleFindRecordsResponse(response: InspectionRecordsResponse) {
        if (response.status == "success" && !response.data.isNullOrEmpty()) {
            val records = response.data.map { dataItem ->
                HistoricalInspection(
                    hojaDeRuta = dataItem.valorColumnaD,
                    articulo = dataItem.valorColumnaH,
                    tipoCalidad = dataItem.valorColumnaS,
                    tipoDeFalla = dataItem.valorColumnaT,
                    metrosDeTela = dataItem.valorColumnaU,
                    fecha = dataItem.valorColumnaC
                )
            }.toMutableList()

            // Almacenar datos para el formulario
            currentHojaDeRuta = response.data[0].valorColumnaD
            val articulo = response.data[0].valorColumnaH
            val fecha = response.data[0].valorColumnaC
            val tejeduria = response.data[0].valorColumnaE
            val telar = response.data[0].valorColumnaF
            val tintoreria = response.data[0].valorColumnaG
            val color = response.data[0].valorColumnaI
            val rolloDeUrdido = response.data[0].valorColumnaJ
            val orden = response.data[0].valorColumnaK
            val cadena = response.data[0].valorColumnaL
            val anchoDeRollo = response.data[0].valorColumnaM
            val esmerilado = response.data[0].valorColumnaN
            val ignifugo = response.data[0].valorColumnaO
            val impermeable = response.data[0].valorColumnaP
            val otro = response.data[0].valorColumnaQ

            // Inicializar el ViewModel con los datos de la sesión
            viewModel.initSessionData(
                usuario = loggedInUser,
                hojaDeRuta = currentHojaDeRuta,
                fecha = Date(),
                tejeduria = tejeduria,
                telar = telar.toString(),
                tintoreria = tintoreria.toString(),
                articulo = articulo,
                color = color.toString(),
                rolloDeUrdido = rolloDeUrdido.toString(),
                orden = orden,
                cadena = cadena.toString(),
                anchoDeRollo = anchoDeRollo.toString(),
                esmerilado = esmerilado,
                ignifugo = ignifugo,
                impermeable = impermeable,
                otro = otro
            )

            // Actualizar el RecyclerView y mostrar el formulario
            // Se corrigió el tipo de dato que se pasa al adaptador
            inspectionHistoryAdapter.updateList(records)
            textExistingRecordsTitle.text = "Registros previamente ingresados para el artículo: $articulo"
            formAndRecordsContainer.visibility = View.VISIBLE
            hojaRutaContainer.visibility = View.GONE
            isHojaRutaFound = true

        } else {
            Toast.makeText(this@ContinuarCargaActivity, "No se encontraron registros para la hoja de ruta.", Toast.LENGTH_LONG).show()
        }
        setLoadingState(false)
    }

    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            loadingOverlay.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.GONE
            loadingOverlay.visibility = View.GONE
        }
        editHojaRuta.isEnabled = !isLoading
        btnBuscar.isEnabled = !isLoading
    }

    // El resto de los métodos son idénticos a los de SegundoRegistroActivity
    // por lo que se pueden copiar y pegar aquí.

    /**
     * Este método es para el botón "Continuar".
     * Inserta el registro localmente, y la sincronización se dispara de forma
     * reactiva por la observación de la base de datos y la red.
     */
    private fun saveInspectionAndResetForm() {
        val inspection = createInspectionObject()
        lifecycleScope.launch {
            viewModel.insertInspection(inspection)
            resetForm()
        }
    }

    /**
     * Este método es para el botón "Finalizar".
     * Llama al método `finalizeAndSync` del ViewModel para asegurarse
     * de que el registro se suba a la nube antes de redirigir.
     */
    private fun saveInspectionAndFinalize() {
        val inspection = createInspectionObject()
        lifecycleScope.launch {
            // Llama a la nueva función suspendida para insertar y sincronizar de forma síncrona
            val success = viewModel.finalizeAndSync(inspection)

            // Limpiar la lista de registros de la sesión
            viewModel.clearCurrentSessionList()

            // Después de que la sincronización termine, redirigimos
            if (success) {
                Toast.makeText(this@ContinuarCargaActivity, "Registro subido y finalizado.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@ContinuarCargaActivity, "No se pudo subir a la nube. Guardado localmente. Finalizando.", Toast.LENGTH_LONG).show()
            }

            val intent = Intent(this@ContinuarCargaActivity, HomeActivity::class.java)
            intent.putExtra("LOGGED_IN_USER", loggedInUser)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun createInspectionObject(): Inspection {
        val tipoCalidad = spinnerTipoCalidad.text.toString()
        val tipoDeFalla = if (tipoCalidad == "Segunda") spinnerTipoDeFalla.text.toString() else null
        val metrosDeTela = editMetrosDeTela.text.toString().toDoubleOrNull() ?: 0.0

        // Los datos de la hoja de ruta ya están almacenados en el ViewModel
        val inspectionData = viewModel.getCurrentSessionData()

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val inspectionDate = dateFormat.parse(inspectionData.fecha.toString()) ?: Date()
        val uniqueId = UUID.randomUUID().toString()

        return Inspection(
            usuario = loggedInUser,
            fecha = inspectionDate,
            hojaDeRuta = inspectionData.hojaDeRuta,
            tejeduria = inspectionData.tejeduria,
            telar = inspectionData.telar,
            tintoreria = inspectionData.tintoreria,
            articulo = inspectionData.articulo,
            color = inspectionData.color,
            rolloDeUrdido = inspectionData.rolloDeUrdido,
            orden = inspectionData.orden,
            cadena = inspectionData.cadena,
            anchoDeRollo = inspectionData.anchoDeRollo,
            esmerilado = inspectionData.esmerilado,
            ignifugo = inspectionData.ignifugo,
            impermeable = inspectionData.impermeable,
            otro = inspectionData.otro,
            tipoCalidad = tipoCalidad,
            tipoDeFalla = tipoDeFalla,
            metrosDeTela = metrosDeTela,
            uniqueId = uniqueId,
            imagePaths = emptyList(),
            imageUrls = emptyList()
        )
    }

    private fun resetForm() {
        spinnerTipoCalidad.setText("", false)
        spinnerTipoDeFalla.setText("", false)
        layoutTipoDeFalla.visibility = View.GONE
        editMetrosDeTela.text.clear()
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (spinnerTipoCalidad.text.isNullOrBlank()) {
            findViewById<TextInputLayout>(R.id.layout_tipo_calidad).error = "Tipo de Calidad es obligatorio"
            isValid = false
        } else {
            findViewById<TextInputLayout>(R.id.layout_tipo_calidad).error = null
        }

        if (spinnerTipoCalidad.text.toString() == "Segunda" && spinnerTipoDeFalla.text.isNullOrBlank()) {
            findViewById<TextInputLayout>(R.id.layout_tipo_de_falla).error = "Tipo de Falla es obligatorio"
            isValid = false
        } else if (spinnerTipoCalidad.text.toString() == "Primera") {
            findViewById<TextInputLayout>(R.id.layout_tipo_de_falla).error = null
        }

        if (editMetrosDeTela.text.isNullOrBlank()) {
            findViewById<TextInputLayout>(R.id.layout_metros_de_tela).error = "Metros de Tela es obligatorio"
            isValid = false
        } else {
            findViewById<TextInputLayout>(R.id.layout_metros_de_tela).error = null
        }

        return isValid
    }
}
