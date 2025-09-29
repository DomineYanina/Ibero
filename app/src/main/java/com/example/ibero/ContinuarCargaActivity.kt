package com.example.ibero

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ibero.data.AppDatabase
import com.example.ibero.data.HistoricalInspection
import com.example.ibero.data.HojaDeRuta
import com.example.ibero.data.Inspection
import com.example.ibero.data.network.GoogleSheetsApi2
import com.example.ibero.ui.CurrentSessionInspectionAdapter
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ContinuarCargaActivity : AppCompatActivity() {

    private lateinit var viewModel: InspectionViewModel
    private lateinit var database: AppDatabase
    private lateinit var editHojaDeRuta: EditText
    private lateinit var spinnerTipoCalidad: AutoCompleteTextView
    private lateinit var layoutTipoDeFalla: TextInputLayout
    private lateinit var spinnerTipoDeFalla: AutoCompleteTextView
    private lateinit var editMetrosDeTela: EditText
    private lateinit var btnGuardarRegistro: Button
    private lateinit var btnIncorporar: Button
    private lateinit var btnCancelar: Button
    private lateinit var btnLimpiarCalidad: Button
    private lateinit var btnLimpiarFalla: Button
    private lateinit var btnLimpiarMetros: Button
    private var editingHistoricalInspection: HistoricalInspection? = null
    private lateinit var btnBuscar: Button
    private lateinit var containerTipoDeFalla: LinearLayout
    private lateinit var recyclerViewCurrentSessionRecords: RecyclerView
    private lateinit var currentSessionInspectionAdapter: CurrentSessionInspectionAdapter
    private lateinit var textSessionTitle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingOverlay: View
    private lateinit var formAndRecordsContainer: LinearLayout
    private lateinit var progressBarFalla: ProgressBar
    private var editingInspection: Inspection? = null
    private var hasRegisteredAnItem = false
    private lateinit var usuario: String
    private lateinit var fecha: String
    private lateinit var hojaDeRuta: String
    private lateinit var tejeduria: String
    private var telar: Int = 0
    private var tintoreria: Int = 0
    private lateinit var articulo: String
    private var color: Int = 0
    private var rolloDeUrdido: Int = 0
    private lateinit var orden: String
    private var cadena: Int = 0
    private var anchoDeRolloParte1: Int = 0
    private lateinit var esmerilado: String
    private lateinit var ignifugo: String
    private lateinit var impermeable: String
    private lateinit var otro: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_continuar_carga)

        val factory = InspectionViewModelFactory(application, enableAutoSync = false)
        viewModel = ViewModelProvider(this, factory).get(InspectionViewModel::class.java)

        database = AppDatabase.getDatabase(this)

        usuario = intent.getStringExtra("LOGGED_IN_USER") ?: "Usuario Desconocido"
        hojaDeRuta = intent.getStringExtra("HOJA_DE_RUTA") ?: ""
        tejeduria = intent.getStringExtra("TEJEDURIA") ?: ""
        telar = intent.getIntExtra("TELAR", 0)
        tintoreria = intent.getIntExtra("TINTORERIA", 0)
        articulo = intent.getStringExtra("ARTICULO") ?: ""
        color = intent.getIntExtra("COLOR", 0)
        rolloDeUrdido = intent.getIntExtra("ROLLO_DE_URDIDO", 0)
        orden = intent.getStringExtra("ORDEN") ?: ""
        cadena = intent.getIntExtra("CADENA", 0)
        anchoDeRolloParte1 = intent.getIntExtra("ANCHO_DE_ROLLO", 0)
        esmerilado = intent.getStringExtra("ESMERILADO") ?: ""
        ignifugo = intent.getStringExtra("IGNIFUGO") ?: ""
        impermeable = intent.getStringExtra("IMPERMEABLE") ?: ""
        otro = intent.getStringExtra("OTRO") ?: ""

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        fecha = dateFormat.format(Date())

        // 1. Inicializar todas las vistas primero
        initViews()
        setupSpinners()
        setupListeners()
        setupCurrentSessionRecyclerView()
        setupFieldWatchers()

        // 2. Después de inicializar, puedes acceder a las propiedades
        progressBar = findViewById(R.id.progress_bar)
        loadingOverlay = findViewById(R.id.loading_overlay)

        btnIncorporar.isVisible = false
        btnGuardarRegistro.isVisible = false

        textSessionTitle.text = "Registros ingresados para la hoja: $hojaDeRuta"

        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                progressBar.visibility = View.VISIBLE
                loadingOverlay.visibility = View.VISIBLE
                btnIncorporar.isVisible = false
                btnGuardarRegistro.isVisible = false
                spinnerTipoCalidad.isVisible = false
                spinnerTipoDeFalla.isVisible = false
                editMetrosDeTela.isVisible = false
            } else {
                progressBar.visibility = View.GONE
                loadingOverlay.visibility = View.GONE
                spinnerTipoCalidad.isVisible = true
                spinnerTipoDeFalla.isVisible = true
                editMetrosDeTela.isVisible = true
                toggleButtonsBasedOnInput()
            }
        }

        viewModel.syncMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearSyncMessage()
            }
        }

        viewModel.currentSessionInspections.observe(this) { inspections ->
            currentSessionInspectionAdapter.updateList(inspections)
            if (inspections.isNotEmpty()) {
                hasRegisteredAnItem = true
                btnCancelar.text = "Ir a Inicio"
            } else {
                hasRegisteredAnItem = false
                btnCancelar.text = "Cancelar"
            }
        }
    }

    private fun initViews() {
        formAndRecordsContainer = findViewById(R.id.form_and_records_container)
        editHojaDeRuta = findViewById(R.id.edit_hoja_ruta_continuar)
        btnBuscar = findViewById(R.id.btn_buscar_continuar)
        spinnerTipoCalidad = findViewById(R.id.spinner_tipo_calidad)
        layoutTipoDeFalla = findViewById(R.id.layout_tipo_de_falla)
        spinnerTipoDeFalla = findViewById(R.id.spinner_tipo_de_falla)
        editMetrosDeTela = findViewById(R.id.edit_metros_de_tela)
        btnGuardarRegistro = findViewById(R.id.btn_guardar_registro)
        btnIncorporar = findViewById(R.id.btn_incorporar)
        recyclerViewCurrentSessionRecords = findViewById(R.id.recycler_view_existing_records)
        textSessionTitle = findViewById(R.id.text_existing_records_title)
        btnCancelar = findViewById(R.id.btn_cancelar_continuar)

        btnLimpiarCalidad = findViewById(R.id.btn_limpiar_calidad)
        btnLimpiarFalla = findViewById(R.id.btn_limpiar_falla)
        btnLimpiarMetros = findViewById(R.id.btn_limpiar_metros)

        containerTipoDeFalla = findViewById(R.id.container_tipo_falla)

        progressBarFalla = findViewById(R.id.progress_bar)
    }

    private fun setupSpinners() {
        val tipoCalidadOptions = arrayOf("Primera", "Segunda")
        val tipoCalidadAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, tipoCalidadOptions)
        spinnerTipoCalidad.setAdapter(tipoCalidadAdapter)

        fetchTiposDeFallaFromDb()

        spinnerTipoCalidad.setOnItemClickListener { _, _, _, _ ->
            val selectedQuality = spinnerTipoCalidad.text.toString()
            if (selectedQuality == "Segunda") {
                containerTipoDeFalla.visibility = View.VISIBLE
            } else {
                containerTipoDeFalla.visibility = View.GONE
                spinnerTipoDeFalla.setText("", false)
            }
        }
    }

    private fun fetchTiposDeFallaFromDb() {
        progressBarFalla.visibility = View.VISIBLE
        spinnerTipoDeFalla.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            database.tipoDeFallaDao().getAllTiposDeFallas().collect { tiposDeFalla ->
                withContext(Dispatchers.Main) {
                    val fallas = tiposDeFalla
                    val tipoDeFallaAdapter = ArrayAdapter(
                        this@ContinuarCargaActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        fallas
                    )
                    spinnerTipoDeFalla.setAdapter(tipoDeFallaAdapter)
                    progressBarFalla.visibility = View.GONE
                    spinnerTipoDeFalla.isEnabled = true
                }
            }
        }

    }

    private fun setupListeners() {

        btnBuscar.setOnClickListener {
            val hojaDeRutaInput = editHojaDeRuta.text.toString().trim()
            if (hojaDeRutaInput.isNotEmpty()) {
                Log.d(TAG, "Botón 'Buscar' presionado. Hoja de Ruta: $hojaDeRutaInput")
                searchAndFetchRecords(hojaDeRutaInput)
            } else {
                Toast.makeText(this, "Por favor, ingresa una Hoja de Ruta.", Toast.LENGTH_SHORT).show()
            }
        }

        btnIncorporar.setOnClickListener {
            if (validateForm()) {
                if (editingInspection != null) {
                    updateInspectionAndResetForm()
                } else {
                    saveInspectionAndResetForm()
                }
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos obligatorios.", Toast.LENGTH_SHORT).show()
            }
        }

        btnGuardarRegistro.setOnClickListener {
            if (validateForm()) {
                if (editingInspection != null) {
                    updateInspectionAndFinalize()
                } else {
                    saveInspectionAndFinalize()
                }
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos obligatorios.", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancelar.setOnClickListener {
            if (hasRegisteredAnItem) {
                // Al tocar "Ir a Inicio", se sincronizan todos los registros pendientes
                finalizeAndSyncAll()
            } else {
                val loggedInUser = intent.getStringExtra("LOGGED_IN_USER") ?: "Usuario Desconocido"
                val homeIntent = Intent(this, HomeActivity::class.java)
                homeIntent.putExtra("LOGGED_IN_USER", loggedInUser)
                startActivity(homeIntent)
                finish()
            }
        }

        btnLimpiarCalidad.setOnClickListener {
            resetForm(fieldToReset = "calidad")
        }

        btnLimpiarFalla.setOnClickListener {
            resetForm(fieldToReset = "falla")
        }

        btnLimpiarMetros.setOnClickListener {
            resetForm(fieldToReset = "metros")
        }
    }

    private fun setViewsEnabled(enabled: Boolean) {
        editHojaDeRuta.isEnabled = enabled
        btnBuscar.isEnabled = enabled
        btnCancelar.isEnabled = enabled
        btnIncorporar.isEnabled = enabled
        btnGuardarRegistro.isEnabled = enabled
        spinnerTipoCalidad.isEnabled = enabled
        spinnerTipoDeFalla.isEnabled = enabled
        editMetrosDeTela.isEnabled = enabled
        loadingOverlay.visibility = if (enabled) View.GONE else View.VISIBLE
        progressBar.visibility = if (enabled) View.GONE else View.VISIBLE
        Log.d(TAG, "Estado de las vistas cambiado a: $enabled")
    }

    private fun searchAndFetchRecords(hojaDeRutaInput: String) {
        setViewsEnabled(false)
        Toast.makeText(this, "Buscando Hoja de Ruta e historial...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = GoogleSheetsApi2.service.findInspectionRecords(hojaDeRuta = hojaDeRutaInput)

                withContext(Dispatchers.Main) {
                    val records = response.data.records
                    if (response.status == "success" && records.isNotEmpty()) {
                        Log.d(TAG, "Registros encontrados: ${records.size}")
                        val firstRecord = records.first()

                        // Limpia la sesión actual y carga los registros históricos en el ViewModel
                        viewModel.clearCurrentSessionList()
                        viewModel.loadHistoricalInspections(records)

                        viewModel.initSessionData(
                            usuario = intent.getStringExtra("LOGGED_IN_USER") ?: "Usuario Desconocido",
                            hojaDeRuta = firstRecord.hojaDeRuta,
                            fecha = Date(),
                            tejeduria = firstRecord.tejeduria ?: "",
                            telar = firstRecord.telar?.toString() ?: "0",
                            tintoreria = firstRecord.tintoreria?.toString() ?: "0",
                            articulo = firstRecord.articulo,
                            color = firstRecord.color?.toString() ?: "0",
                            rolloDeUrdido = firstRecord.rolloDeUrdido?.toString() ?: "0",
                            orden = firstRecord.orden ?: "",
                            cadena = firstRecord.cadena?.toString() ?: "0",
                            anchoDeRollo = firstRecord.anchoDeRollo?.toString() ?: "0",
                            esmerilado = firstRecord.esmerilado ?: "",
                            ignifugo = firstRecord.ignifugo ?: "",
                            impermeable = firstRecord.impermeable ?: "",
                            otro = firstRecord.otro ?: "",
                            uniqueId = firstRecord.uniqueId ?: ""
                        )

                        Log.d(TAG, "uniqueId del primer registro: ${firstRecord.uniqueId}")
                        Log.d(TAG, "hojaDeRuta del primer registro: ${firstRecord.hojaDeRuta}")

                        textSessionTitle.text = "Registros para la Hoja de Ruta: ${firstRecord.hojaDeRuta} - Artículo: ${firstRecord.articulo}"
                        formAndRecordsContainer.visibility = View.VISIBLE
                        // Ya no se llama al adaptador directamente aquí, el observador se encargará de esto
                        //Toast.makeText(this@ContinuarCargaActivity, "Hoja de Ruta encontrada. Historial cargado.", Toast.LENGTH_LONG).show()
                        toggleFormButtons()
                    } else {
                        Toast.makeText(this@ContinuarCargaActivity, "No se encontraron registros para la Hoja de Ruta. Por favor, revisa el número.", Toast.LENGTH_LONG).show()
                        formAndRecordsContainer.visibility = View.GONE
                        viewModel.clearCurrentSessionList()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al buscar y precargar registros: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ContinuarCargaActivity, "Error al buscar, por favor intente de nuevo más tarde.", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    setViewsEnabled(true)
                }
            }
        }
    }

    private fun toggleFormButtons() {
        if (formAndRecordsContainer.visibility == View.VISIBLE) {
            val hayInput = spinnerTipoCalidad.text.isNotBlank() ||
                    spinnerTipoDeFalla.text.isNotBlank() ||
                    editMetrosDeTela.text.isNotBlank()

            if (hayInput) {
                btnCancelar.isVisible = false
                btnIncorporar.isVisible = true
                if(editingHistoricalInspection != null){
                    btnGuardarRegistro.isVisible = false
                    btnCancelar.isVisible = true
                } else{
                    btnGuardarRegistro.isVisible = true
                }
            } else {
                btnCancelar.isVisible = true
                btnIncorporar.isVisible = false
                btnGuardarRegistro.isVisible = false
            }
        }
    }

    private fun setupCurrentSessionRecyclerView() {
        currentSessionInspectionAdapter = CurrentSessionInspectionAdapter(mutableListOf()) { inspection ->
            showEditConfirmationDialog(inspection)
        }
        recyclerViewCurrentSessionRecords.layoutManager = LinearLayoutManager(this)
        recyclerViewCurrentSessionRecords.adapter = currentSessionInspectionAdapter
    }

    private fun showEditConfirmationDialog(inspection: Inspection) {
        AlertDialog.Builder(this)
            .setTitle("Modificar Registro")
            .setMessage("¿Deseas modificar el registro de ${inspection.tipoCalidad} con ${inspection.metrosDeTela} metros?")
            .setPositiveButton("Sí") { dialog, _ ->
                preloadFormForEditing(inspection)
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun preloadFormForEditing(inspection: Inspection) {
        editingInspection = inspection
        spinnerTipoCalidad.setText(inspection.tipoCalidad, false)

        if (inspection.tipoCalidad == "Segunda") {
            containerTipoDeFalla.visibility = View.VISIBLE
            spinnerTipoDeFalla.setText(inspection.tipoDeFalla, false)
        } else {
            containerTipoDeFalla.visibility = View.GONE
            spinnerTipoDeFalla.setText("", false)
        }

        editMetrosDeTela.setText(inspection.metrosDeTela.toString())

        btnIncorporar.text = "Actualizar"
        Toast.makeText(this, "Modifica los campos y toca Actualizar", Toast.LENGTH_SHORT).show()
    }

    private fun setupFieldWatchers() {
        editMetrosDeTela.addTextChangedListener { toggleButtonsBasedOnInput() }
        spinnerTipoCalidad.addTextChangedListener { toggleButtonsBasedOnInput() }
        spinnerTipoDeFalla.addTextChangedListener { toggleButtonsBasedOnInput() }
    }

    private fun toggleButtonsBasedOnInput() {
        val hayInput = spinnerTipoCalidad.text.isNotBlank() ||
                spinnerTipoDeFalla.text.isNotBlank() ||
                editMetrosDeTela.text.isNotBlank()

        if (hayInput) {
            btnCancelar.isVisible = false
            btnIncorporar.isVisible = true
            if(editingInspection!=null){
                btnGuardarRegistro.isVisible=false
            } else{
                btnGuardarRegistro.isVisible=true
            }
        } else {
            btnCancelar.isVisible = true
            btnIncorporar.isVisible = false
            btnGuardarRegistro.isVisible = false
        }
    }

    private fun saveInspectionAndResetForm() {
        val inspection = createInspectionObject()
        Log.e("HojaDeRuta", "Hoja de ruta por transmitir: ${inspection.hojaDeRuta}")
        lifecycleScope.launch {
            viewModel.insertInspection(inspection)
            resetForm()
        }
    }

    private fun updateInspectionAndResetForm() {
        val inspectionToUpdate = editingInspection?.copy(
            tipoCalidad = spinnerTipoCalidad.text.toString(),
            tipoDeFalla = if (spinnerTipoCalidad.text.toString() == "Segunda") spinnerTipoDeFalla.text.toString() else null,
            metrosDeTela = editMetrosDeTela.text.toString().toDoubleOrNull() ?: 0.0,
            isSynced = false // Asegurar que el registro se marque como no sincronizado para ser subido
        )

        if (inspectionToUpdate != null) {
            lifecycleScope.launch {
                viewModel.updateInspection(inspectionToUpdate)
                // Se actualiza la lista de la sesión para reflejar el cambio en la UI
                viewModel.addInspectionToSessionList(inspectionToUpdate)
                resetForm()
            }
        } else {
            Log.e("UpdateError", "No se pudo crear el objeto Inspection para actualizar. Objeto nulo.")
        }
    }

    // CAMBIO: Se modifica la función para que solo guarde en la base de datos local antes de finalizar
    private fun saveInspectionAndFinalize() {
        val inspection = createInspectionObject()
        lifecycleScope.launch {
            viewModel.insertInspection(inspection)
            finalizeAndSyncAll()
        }
    }

    // CAMBIO: Se modifica la función para que solo actualice en la base de datos local antes de finalizar
    private fun updateInspectionAndFinalize() {
        val inspectionToUpdate = editingInspection?.copy(
            tipoCalidad = spinnerTipoCalidad.text.toString(),
            tipoDeFalla = if (spinnerTipoCalidad.text.toString() == "Segunda") spinnerTipoDeFalla.text.toString() else null,
            metrosDeTela = editMetrosDeTela.text.toString().toDoubleOrNull() ?: 0.0,
            isSynced = false // Asegurar que el registro se marque como no sincronizado
        )

        if (inspectionToUpdate != null) {
            lifecycleScope.launch {
                viewModel.updateInspection(inspectionToUpdate)
                finalizeAndSyncAll()
            }
        } else {
            Log.e("UpdateError", "No se pudo crear el objeto Inspection para actualizar. Objeto nulo.")
        }
    }

    // CAMBIO: Esta función se mantiene igual, ya que es la que se encarga de la sincronización
    private fun finalizeAndSyncAll() {
        lifecycleScope.launch {
            val hojaDeRutaEntity = HojaDeRuta(nombre = hojaDeRuta)
            database.hojaDeRutaDao().insert(hojaDeRutaEntity)

            viewModel.performSync()
            viewModel.clearCurrentSessionList()

            // Navegar a HomeActivity después de la sincronización
            val intent = Intent(this@ContinuarCargaActivity, HomeActivity::class.java)
            intent.putExtra("LOGGED_IN_USER", usuario)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun createInspectionObject(): Inspection {
        val sessionData = viewModel.getCurrentSessionData()
        val tipoCalidad = spinnerTipoCalidad.text.toString()
        val tipoDeFalla = if (tipoCalidad == "Segunda") spinnerTipoDeFalla.text.toString() else null
        val metrosDeTela = editMetrosDeTela.text.toString().toDoubleOrNull() ?: 0.0

        return sessionData.copy(
            id = 0,
            uniqueId = UUID.randomUUID().toString(),
            tipoCalidad = tipoCalidad,
            tipoDeFalla = tipoDeFalla,
            metrosDeTela = metrosDeTela,
            isSynced = false,
            imagePaths = emptyList(),
            imageUrls = emptyList()
        )

        Log.e("HojaDeRuta","Hoja de ruta: $hojaDeRuta")

    }

    private fun resetForm(fieldToReset: String? = null) {
        when (fieldToReset) {
            "calidad" -> {
                spinnerTipoCalidad.setText("", false)
                containerTipoDeFalla.visibility = View.GONE
                spinnerTipoDeFalla.setText("", false)
            }
            "falla" -> {
                spinnerTipoDeFalla.setText("", false)
            }
            "metros" -> {
                editMetrosDeTela.text.clear()
            }
            else -> {
                spinnerTipoCalidad.setText("", false)
                containerTipoDeFalla.visibility = View.GONE
                spinnerTipoDeFalla.setText("", false)
                editMetrosDeTela.text.clear()
            }
        }

        editingInspection = null
        btnIncorporar.text = "Continuar"
        toggleButtonsBasedOnInput()
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