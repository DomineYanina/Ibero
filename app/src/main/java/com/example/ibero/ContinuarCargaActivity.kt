package com.example.ibero

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
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
import com.example.ibero.data.HistoricalInspection
import com.example.ibero.data.Inspection
import com.example.ibero.data.toInspection
import com.example.ibero.data.network.GoogleSheetsApi2
import com.example.ibero.ui.InspectionHistoryAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID

class ContinuarCargaActivity : AppCompatActivity() {

    // Vistas de la interfaz de usuario
    private lateinit var editHojaDeRuta: EditText
    private lateinit var btnBuscar: MaterialButton
    private lateinit var btnCancelar: MaterialButton
    private lateinit var recyclerViewExistingRecords: RecyclerView
    private lateinit var formAndRecordsContainer: LinearLayout
    private lateinit var contenedorHojaRuta: LinearLayout
    private lateinit var textExistingRecordsTitle: TextView
    private lateinit var loadingOverlay: View
    private lateinit var progressBar: View

    // Elementos del formulario de ingreso
    private lateinit var spinnerTipoCalidad: AutoCompleteTextView
    private lateinit var layoutTipoFalla: TextInputLayout
    private lateinit var spinnerTipoFalla: AutoCompleteTextView
    private lateinit var editMetrosDeTela: EditText
    private lateinit var btnIncorporar: MaterialButton
    private lateinit var btnGuardarRegistro: MaterialButton
    private lateinit var viewModel: InspectionViewModel
    private lateinit var historyAdapter: InspectionHistoryAdapter
    private val TAG = "ContinuarCargaLog"

    // Nuevas variables para los botones de limpieza
    private lateinit var btnLimpiarCalidad: MaterialButton
    private lateinit var btnLimpiarFalla: MaterialButton
    private lateinit var btnLimpiarMetros: MaterialButton

    // Nueva variable para el contenedor de tipo de falla
    private lateinit var containerTipoFalla: LinearLayout

    // Nuevas variables de estado para la lógica de los botones y la edición
    private var hasRegisteredAnItem = false
    private var editingHistoricalInspection: HistoricalInspection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_continuar_carga)

        initViews()
        setupHistoryRecyclerView()
        val factory = InspectionViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory)[InspectionViewModel::class.java]
        setupListeners()
        observeViewModel()
        fetchFallaTypes() // Nueva llamada para cargar los tipos de fallas

        btnIncorporar.isVisible = false
        btnGuardarRegistro.isVisible = false
        formAndRecordsContainer.visibility = View.GONE

        setupFieldWatchers()
    }

    private fun initViews() {
        editHojaDeRuta = findViewById(R.id.edit_hoja_ruta_continuar)
        btnBuscar = findViewById(R.id.btn_buscar_continuar)
        btnCancelar = findViewById(R.id.btn_cancelar_continuar)
        recyclerViewExistingRecords = findViewById(R.id.recycler_view_existing_records)
        formAndRecordsContainer = findViewById(R.id.form_and_records_container)
        textExistingRecordsTitle = findViewById(R.id.text_existing_records_title)
        loadingOverlay = findViewById(R.id.loading_overlay)
        progressBar = findViewById(R.id.progress_bar)

        spinnerTipoCalidad = findViewById(R.id.spinner_tipo_calidad)
        layoutTipoFalla = findViewById(R.id.layout_tipo_de_falla)
        spinnerTipoFalla = findViewById(R.id.spinner_tipo_de_falla)
        editMetrosDeTela = findViewById(R.id.edit_metros_de_tela)
        btnIncorporar = findViewById(R.id.btn_incorporar)
        btnGuardarRegistro = findViewById(R.id.btn_guardar_registro)
        contenedorHojaRuta = findViewById(R.id.hoja_ruta_container)

        btnLimpiarCalidad = findViewById(R.id.btn_limpiar_calidad)
        btnLimpiarFalla = findViewById(R.id.btn_limpiar_falla)
        btnLimpiarMetros = findViewById(R.id.btn_limpiar_metros)

        containerTipoFalla = findViewById(R.id.container_tipo_falla)
    }

    private fun setupHistoryRecyclerView() {
        historyAdapter = InspectionHistoryAdapter(mutableListOf()) { historicalInspection ->
            Log.d(TAG, "RecyclerView clic en item: ID único = ${historicalInspection.uniqueId}")
            showEditConfirmationDialog(historicalInspection)
        }
        recyclerViewExistingRecords.layoutManager = LinearLayoutManager(this)
        recyclerViewExistingRecords.adapter = historyAdapter
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            setViewsEnabled(!isLoading)
        }
        viewModel.syncMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearSyncMessage()
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

        btnCancelar.setOnClickListener {
            val loggedInUser = intent.getStringExtra("LOGGED_IN_USER") ?: "Usuario Desconocido"
            val homeIntent = Intent(this, HomeActivity::class.java)
            homeIntent.putExtra("LOGGED_IN_USER", loggedInUser)
            startActivity(homeIntent)
            finish()
        }

        val calidadTypes = arrayOf("Primera", "Segunda")
        val calidadAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, calidadTypes)
        spinnerTipoCalidad.setAdapter(calidadAdapter)
        spinnerTipoCalidad.setOnItemClickListener { parent, _, position, _ ->
            val selectedCalidad = parent.getItemAtPosition(position).toString()
            if (selectedCalidad == "Segunda") {
                containerTipoFalla.visibility = View.VISIBLE
            } else {
                containerTipoFalla.visibility = View.GONE
                spinnerTipoFalla.setText("", false)
            }
        }

        // Se elimina la lista hardcodeada de fallas. Ahora se cargará dinámicamente.
        // La configuración del adapter se mueve a la nueva función fetchFallaTypes().

        btnIncorporar.setOnClickListener {
            Log.d(TAG, "Botón 'Incorporar' presionado. editingHistoricalInspection es nulo? ${editingHistoricalInspection == null}")
            if (validateForm()) {
                if (editingHistoricalInspection != null) {
                    updateInspectionAndSync()
                } else {
                    saveInspectionAndResetForm()
                }
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos obligatorios.", Toast.LENGTH_SHORT).show()
            }
        }

        btnGuardarRegistro.setOnClickListener {
            Log.d(TAG, "Botón 'Guardar Registro' presionado. editingHistoricalInspection es nulo? ${editingHistoricalInspection == null}")
            if (validateForm()) {
                if (editingHistoricalInspection != null) {
                    updateInspectionAndFinalize()
                } else {
                    saveInspectionAndFinalize()
                }
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos obligatorios.", Toast.LENGTH_SHORT).show()
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

    private fun setupFieldWatchers() {
        editMetrosDeTela.addTextChangedListener { toggleFormButtons() }
        spinnerTipoCalidad.addTextChangedListener { toggleFormButtons() }
        spinnerTipoFalla.addTextChangedListener { toggleFormButtons() }
    }

    private fun fetchFallaTypes() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = GoogleSheetsApi2.service.getTiposDeFallas()
                if (response.isSuccessful) {
                    val fallaTypes = response.body()?.data?.tiposDeFallas ?: emptyList()
                    withContext(Dispatchers.Main) {
                        val fallaAdapter = ArrayAdapter(this@ContinuarCargaActivity, android.R.layout.simple_dropdown_item_1line, fallaTypes)
                        spinnerTipoFalla.setAdapter(fallaAdapter)
                        Log.d(TAG, "Tipos de falla cargados: $fallaTypes")
                    }
                } else {
                    Log.e(TAG, "Error al obtener tipos de falla: ${response.errorBody()?.string()}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ContinuarCargaActivity, "Error al cargar tipos de falla desde la nube.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de red al obtener tipos de falla: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ContinuarCargaActivity, "Error de red: no se pudo cargar la lista de fallas.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun toggleFormButtons() {
        if (formAndRecordsContainer.visibility == View.VISIBLE) {
            val hayInput = spinnerTipoCalidad.text.isNotBlank() ||
                    spinnerTipoFalla.text.isNotBlank() ||
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

    private fun setViewsEnabled(enabled: Boolean) {
        editHojaDeRuta.isEnabled = enabled
        btnBuscar.isEnabled = enabled
        btnCancelar.isEnabled = enabled
        btnIncorporar.isEnabled = enabled
        btnGuardarRegistro.isEnabled = enabled
        spinnerTipoCalidad.isEnabled = enabled
        spinnerTipoFalla.isEnabled = enabled
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
                        textExistingRecordsTitle.text = "Registros para la Hoja de Ruta: ${firstRecord.hojaDeRuta} - Artículo: ${firstRecord.articulo}"
                        formAndRecordsContainer.visibility = View.VISIBLE
                        historyAdapter.updateList(records)
                        Toast.makeText(this@ContinuarCargaActivity, "Hoja de Ruta encontrada. Historial cargado.", Toast.LENGTH_LONG).show()
                        toggleFormButtons()
                    } else {
                        Toast.makeText(this@ContinuarCargaActivity, "No se encontraron registros para la Hoja de Ruta. Por favor, revisa el número.", Toast.LENGTH_LONG).show()
                        formAndRecordsContainer.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al buscar y precargar registros: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ContinuarCargaActivity, "Error de red o API: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    setViewsEnabled(true)
                }
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true
        val tipoCalidadInput = spinnerTipoCalidad.text.toString().trim()
        val tipoFallaInput = spinnerTipoFalla.text.toString().trim()
        val metrosDeTelaInput = editMetrosDeTela.text.toString().trim()

        if (tipoCalidadInput.isEmpty()) {
            findViewById<TextInputLayout>(R.id.layout_tipo_calidad).error = "Tipo de Calidad es obligatorio"
            isValid = false
        } else {
            findViewById<TextInputLayout>(R.id.layout_tipo_calidad).error = null
        }
        if (tipoCalidadInput == "Segunda" && tipoFallaInput.isEmpty()) {
            findViewById<TextInputLayout>(R.id.layout_tipo_de_falla).error = "Tipo de Falla es obligatorio"
            isValid = false
        } else if (tipoCalidadInput == "Primera") {
            findViewById<TextInputLayout>(R.id.layout_tipo_de_falla).error = null
        }
        if (metrosDeTelaInput.isEmpty()) {
            findViewById<TextInputLayout>(R.id.layout_metros_de_tela).error = "Metros de Tela es obligatorio"
            isValid = false
        } else {
            findViewById<TextInputLayout>(R.id.layout_metros_de_tela).error = null
        }
        return isValid
    }

    private fun createInspectionObject(): Inspection {
        val sessionData = viewModel.getCurrentSessionData()
        val tipoCalidad = spinnerTipoCalidad.text.toString()
        val tipoDeFalla = if (tipoCalidad == "Segunda") spinnerTipoFalla.text.toString() else null
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
    }

    private fun saveInspectionAndResetForm() {
        val inspection = createInspectionObject()
        lifecycleScope.launch {
            viewModel.insertInspection(inspection)
            resetNewRecordForm()
            hasRegisteredAnItem = true
            btnCancelar.text = "Ir a Inicio"
            toggleFormButtons()
        }
    }

    private fun saveInspectionAndFinalize() {
        val inspection = createInspectionObject()
        lifecycleScope.launch {
            val success = viewModel.finalizeAndSync(inspection)
            viewModel.clearCurrentSessionList()
            if (success) {
                Toast.makeText(this@ContinuarCargaActivity, "Registro subido y finalizado.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@ContinuarCargaActivity, "No se pudo subir a la nube. Guardado localmente. Finalizando.", Toast.LENGTH_LONG).show()
            }

            val userFromSession = viewModel.getCurrentSessionData().usuario
            val intent = Intent(this@ContinuarCargaActivity, HomeActivity::class.java)
            intent.putExtra("LOGGED_IN_USER", userFromSession)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showEditConfirmationDialog(historicalInspection: HistoricalInspection) {
        Log.d(TAG, "showEditConfirmationDialog llamado para uniqueId: ${historicalInspection.uniqueId}")
        AlertDialog.Builder(this)
            .setTitle("Modificar Registro")
            .setMessage("¿Deseas modificar este registro?")
            .setPositiveButton("Sí") { dialog, _ ->
                preloadFormForEditing(historicalInspection)
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun preloadFormForEditing(historicalInspection: HistoricalInspection) {
        btnBuscar.isVisible = false
        Log.d(TAG, "preloadFormForEditing: Estableciendo editingHistoricalInspection con uniqueId: ${historicalInspection.uniqueId}")
        editingHistoricalInspection = historicalInspection

        val inspection = historicalInspection.toInspection()

        spinnerTipoCalidad.setText(inspection.tipoCalidad, false)
        if (inspection.tipoCalidad == "Segunda") {
            containerTipoFalla.visibility = View.VISIBLE
            spinnerTipoFalla.setText(inspection.tipoDeFalla ?: "", false)
        } else {
            containerTipoFalla.visibility = View.GONE
            spinnerTipoFalla.setText("", false)
        }
        editMetrosDeTela.setText(inspection.metrosDeTela.toString())

        btnIncorporar.text = "Actualizar"
        //btnGuardarRegistro.text = "Actualizar y Finalizar"
        Toast.makeText(this, "Modifica los campos y toca Actualizar", Toast.LENGTH_SHORT).show()

        toggleFormButtons()
        btnGuardarRegistro.isVisible = false
    }

    private fun updateInspectionAndSync() {
        Log.d(TAG, "updateInspectionAndSync: Iniciando. editingHistoricalInspection es nulo? ${editingHistoricalInspection == null}")
        btnBuscar.isVisible = true
        btnGuardarRegistro.isVisible = true
        val inspectionUniqueId = editingHistoricalInspection?.uniqueId

        if (inspectionUniqueId != null) {
            Log.d(TAG, "updateInspectionAndSync: uniqueId no es nulo. Procediendo con la actualización.")
            val updatedInspection = createUpdatedInspectionObject()

            lifecycleScope.launch {
                val success = viewModel.updateInspectionAndSync(updatedInspection)

                if (success.toString().isNotEmpty()) {
                    Toast.makeText(this@ContinuarCargaActivity, "Registro actualizado en la nube.", Toast.LENGTH_SHORT).show()
                    searchAndFetchRecords(editHojaDeRuta.text.toString().trim())
                } else {
                    Toast.makeText(this@ContinuarCargaActivity, "No se pudo actualizar en la nube. Intenta nuevamente.", Toast.LENGTH_LONG).show()
                }

                resetForm()
            }
        } else {
            Log.e(TAG, "updateInspectionAndSync: ERROR. uniqueId es nulo. No se puede actualizar el registro.")
            Toast.makeText(this, "No hay registro seleccionado para actualizar.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateInspectionAndFinalize() {
        Log.d(TAG, "updateInspectionAndFinalize: Iniciando. editingHistoricalInspection es nulo? ${editingHistoricalInspection == null}")
        val inspectionUniqueId = editingHistoricalInspection?.uniqueId

        if (inspectionUniqueId != null) {
            Log.d(TAG, "updateInspectionAndFinalize: uniqueId no es nulo. Procediendo con la actualización y finalización.")
            val updatedInspection = createUpdatedInspectionObject()

            lifecycleScope.launch {
                val success = viewModel.updateInspectionAndFinalize(updatedInspection)

                if (success) {
                    Toast.makeText(this@ContinuarCargaActivity, "Registro actualizado y finalizado.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ContinuarCargaActivity, "No se pudo subir a la nube. Finalizando.", Toast.LENGTH_LONG).show()
                }

                viewModel.clearCurrentSessionList()

                val userFromSession = intent.getStringExtra("LOGGED_IN_USER") ?: "Usuario Desconocido"
                val intent = Intent(this@ContinuarCargaActivity, HomeActivity::class.java)
                intent.putExtra("LOGGED_IN_USER", userFromSession)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        } else {
            Log.e(TAG, "updateInspectionAndFinalize: ERROR. uniqueId es nulo. No se puede actualizar y finalizar.")
            Toast.makeText(this, "No hay registro seleccionado para actualizar.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createUpdatedInspectionObject(): Inspection {
        val sessionData = viewModel.getCurrentSessionData()
        val tipoCalidad = spinnerTipoCalidad.text.toString()
        val tipoDeFalla = if (tipoCalidad == "Segunda") spinnerTipoFalla.text.toString() else null
        val metrosDeTela = editMetrosDeTela.text.toString().toDoubleOrNull() ?: 0.0
        Log.d(TAG, "createUpdatedInspectionObject: Creando nuevo objeto de inspección con uniqueId: ${editingHistoricalInspection?.uniqueId}")

        return sessionData.copy(
            uniqueId = editingHistoricalInspection?.uniqueId ?: UUID.randomUUID().toString(),
            tipoCalidad = tipoCalidad,
            tipoDeFalla = tipoDeFalla,
            metrosDeTela = metrosDeTela,
            isSynced = false,
        )
    }

    private fun resetNewRecordForm() {
        Log.d(TAG, "resetNewRecordForm: Reseteando formulario y eliminando el registro en edición.")
        resetForm()
        editingHistoricalInspection = null
        btnIncorporar.text = "Continuar"
        btnGuardarRegistro.text = "Guardar Registro"
    }

    private fun resetForm(fieldToReset: String? = null) {
        Log.d(TAG, "resetForm: Llamado con fieldToReset = $fieldToReset")
        when (fieldToReset) {
            "calidad" -> {
                spinnerTipoCalidad.setText("", false)
                containerTipoFalla.visibility = View.GONE
                spinnerTipoFalla.setText("", false)
            }
            "falla" -> {
                spinnerTipoFalla.setText("", false)
            }
            "metros" -> {
                editMetrosDeTela.text.clear()
            }
            else -> {
                spinnerTipoCalidad.setText("", false)
                containerTipoFalla.visibility = View.GONE
                spinnerTipoFalla.setText("", false)
                editMetrosDeTela.text.clear()
            }
        }
        editingHistoricalInspection = null
        btnIncorporar.text = "Continuar"
        btnGuardarRegistro.text = "Guardar Registro"
        toggleFormButtons()
    }

    private fun clearAllFormsAndState() {
        editHojaDeRuta.setText("")
        formAndRecordsContainer.visibility = View.GONE
        historyAdapter.updateList(emptyList())
        resetNewRecordForm()
    }
}