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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ibero.data.Inspection
import com.example.ibero.data.network.GoogleSheetsApi2
import com.example.ibero.ui.InspectionHistoryAdapter
import com.example.ibero.ui.InspectionViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID
import androidx.core.widget.addTextChangedListener

class ContinuarCargaActivity : AppCompatActivity() {

    // Vistas de la interfaz de usuario
    private lateinit var editHojaDeRuta: EditText
    private lateinit var btnBuscar: MaterialButton
    private lateinit var btnCancelar: MaterialButton
    private lateinit var recyclerViewExistingRecords: RecyclerView
    private lateinit var formAndRecordsContainer: LinearLayout
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

    // Nueva variable de estado para la lógica de los botones
    private var hasRegisteredAnItem = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_continuar_carga)
        // Inicialización de vistas
        initViews()
        // Configuración del RecyclerView y el adaptador
        setupHistoryRecyclerView()
        // Inicialización del ViewModel
        val factory = InspectionViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory)[InspectionViewModel::class.java]
        // Configuración de listeners para los botones y spinners
        setupListeners()
        // Observa el estado de carga del ViewModel
        observeViewModel()

        // Ocultar los botones de registro al inicio
        btnIncorporar.isVisible = false
        btnGuardarRegistro.isVisible = false
        // Ocultar el formulario de registro al inicio
        formAndRecordsContainer.visibility = View.GONE

        // Monitorear los campos para mostrar los botones de registro
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

        // Vistas del formulario de ingreso
        spinnerTipoCalidad = findViewById(R.id.spinner_tipo_calidad)
        layoutTipoFalla = findViewById(R.id.layout_tipo_de_falla)
        spinnerTipoFalla = findViewById(R.id.spinner_tipo_de_falla)
        editMetrosDeTela = findViewById(R.id.edit_metros_de_tela)
        btnIncorporar = findViewById(R.id.btn_incorporar)
        btnGuardarRegistro = findViewById(R.id.btn_guardar_registro)

        // Inicializar los nuevos botones de limpieza
        btnLimpiarCalidad = findViewById(R.id.btn_limpiar_calidad)
        btnLimpiarFalla = findViewById(R.id.btn_limpiar_falla)
        btnLimpiarMetros = findViewById(R.id.btn_limpiar_metros)

        // Inicializar el contenedor de tipo de falla
        containerTipoFalla = findViewById(R.id.container_tipo_falla)
    }

    private fun setupHistoryRecyclerView() {
        historyAdapter = InspectionHistoryAdapter(mutableListOf()) { historicalInspection ->
            // Manejar clics en el historial (opcional)
            Toast.makeText(this, "Hoja de Ruta: ${historicalInspection.hojaDeRuta}", Toast.LENGTH_SHORT).show()
        }
        recyclerViewExistingRecords.layoutManager = LinearLayoutManager(this)
        recyclerViewExistingRecords.adapter = historyAdapter
    }

    private fun observeViewModel() {
        // Observa el estado de carga del ViewModel para habilitar/deshabilitar vistas
        viewModel.isLoading.observe(this) { isLoading ->
            setViewsEnabled(!isLoading)
        }
        // Observa los mensajes de sincronización del ViewModel
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

        // Configuración de spinners
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

        val fallaTypes = arrayOf("Aureolas", "Clareadas", "Falla de cadena", "Falla de trama", "Falla de urdido",
            "Gota Espaciada", "Goteras", "Hongos", "Mancha con patrón", "Manchas de aceite",
            "Marcas de sanforizado", "Parada de engomadora", "Parada telar", "Paradas",
            "Quebraduras", "Vainillas")
        val fallaAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, fallaTypes)
        spinnerTipoFalla.setAdapter(fallaAdapter)

        // Listener para el botón "Incorporar"
        btnIncorporar.setOnClickListener {
            if (validateForm()) {
                saveInspectionAndResetForm()
                // Actualizamos el estado después de incorporar el primer registro
                hasRegisteredAnItem = true
                // Cambiamos el texto del botón Cancelar
                btnCancelar.text = "Ir a Inicio"
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos obligatorios.", Toast.LENGTH_SHORT).show()
            }
        }

        // Listener para el botón "Guardar Registro"
        btnGuardarRegistro.setOnClickListener {
            if (validateForm()) {
                saveInspectionAndFinalize()
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos obligatorios.", Toast.LENGTH_SHORT).show()
            }
        }

        // Listeners para los nuevos botones de limpieza
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

    /**
     * Lógica para mostrar/ocultar los botones de registro según el estado de los campos.
     */
    private fun toggleFormButtons() {
        // La lógica solo se aplica si se encontró una hoja de ruta y no se ha presionado "Incorporar"
        if (formAndRecordsContainer.visibility == View.VISIBLE && !hasRegisteredAnItem) {
            val hayInput = spinnerTipoCalidad.text.isNotBlank() ||
                    spinnerTipoFalla.text.isNotBlank() ||
                    editMetrosDeTela.text.isNotBlank()

            if (hayInput) {
                btnCancelar.isVisible = false
                btnIncorporar.isVisible = true
                btnGuardarRegistro.isVisible = true
            } else {
                btnCancelar.isVisible = true
                btnIncorporar.isVisible = false
                btnGuardarRegistro.isVisible = false
            }
        }
        // Si ya se ha incorporado un registro, los botones de registro permanecen visibles
        else if (hasRegisteredAnItem) {
            btnCancelar.text = "Ir a Inicio"
            btnCancelar.isVisible = true
            btnIncorporar.isVisible = true
            btnGuardarRegistro.isVisible = true
        }
    }

    /**
     * Habilita o deshabilita las vistas de la pantalla.
     * @param enabled El estado deseado para las vistas.
     */
    private fun setViewsEnabled(enabled: Boolean) {
        // Habilitar/deshabilitar vistas principales
        editHojaDeRuta.isEnabled = enabled
        btnBuscar.isEnabled = enabled
        btnCancelar.isEnabled = enabled
        btnIncorporar.isEnabled = enabled
        btnGuardarRegistro.isEnabled = enabled
        spinnerTipoCalidad.isEnabled = enabled
        spinnerTipoFalla.isEnabled = enabled
        editMetrosDeTela.isEnabled = enabled
        // Ocultar/mostrar el overlay y el progress bar
        loadingOverlay.visibility = if (enabled) View.GONE else View.VISIBLE
        progressBar.visibility = if (enabled) View.GONE else View.VISIBLE
        Log.d(TAG, "Estado de las vistas cambiado a: $enabled")
    }

    /**
     * Lógica de búsqueda y precarga de datos de la Hoja de Ruta.
     */
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

                        val telarInt = firstRecord.telar?.toInt() ?: 0
                        val tintoreriaInt = firstRecord.tintoreria?.toInt() ?: 0
                        val colorInt = firstRecord.color?.toInt() ?: 0
                        val rolloDeUrdidoInt = firstRecord.rolloDeUrdido?.toInt() ?: 0
                        val cadenaInt = firstRecord.cadena?.toInt() ?: 0
                        val anchoDeRolloInt = firstRecord.anchoDeRollo?.toInt() ?: 0

                        viewModel.initSessionData(
                            usuario = intent.getStringExtra("LOGGED_IN_USER") ?: "Usuario Desconocido",
                            hojaDeRuta = firstRecord.hojaDeRuta,
                            fecha = Date(),
                            tejeduria = firstRecord.tejeduria ?: "",
                            telar = telarInt.toString(),
                            tintoreria = tintoreriaInt.toString(),
                            articulo = firstRecord.articulo,
                            color = colorInt.toString(),
                            rolloDeUrdido = rolloDeUrdidoInt.toString(),
                            orden = firstRecord.orden ?: "",
                            cadena = cadenaInt.toString(),
                            anchoDeRollo = anchoDeRolloInt.toString(),
                            esmerilado = firstRecord.esmerilado ?: "",
                            ignifugo = firstRecord.ignifugo ?: "",
                            impermeable = firstRecord.impermeable ?: "",
                            otro = firstRecord.otro ?: ""
                        )

                        textExistingRecordsTitle.text = "Registros para la Hoja de Ruta: ${firstRecord.hojaDeRuta} - Artículo: ${firstRecord.articulo}"
                        formAndRecordsContainer.visibility = View.VISIBLE
                        historyAdapter.updateList(records)
                        Toast.makeText(this@ContinuarCargaActivity, "Hoja de Ruta encontrada. Historial cargado.", Toast.LENGTH_LONG).show()

                        // Llamar a la lógica de visibilidad inicial después de una búsqueda exitosa
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

    /**
     * Crea un objeto Inspection a partir de los datos del formulario y los datos de sesión del ViewModel.
     */
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

    /**
     * Este método es para el botón "Continuar".
     * Inserta el registro localmente.
     */
    private fun saveInspectionAndResetForm() {
        val inspection = createInspectionObject()
        lifecycleScope.launch {
            viewModel.insertInspection(inspection)
            resetNewRecordForm()
            // Llamar a la lógica de visibilidad después de resetear el formulario
            toggleFormButtons()
        }
    }

    /**
     * Este método es para el botón "Guardar Registro" y "Finalizar".
     * Llama al método `finalizeAndSync` del ViewModel.
     */
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

    /**
     * Resetea el formulario de un nuevo registro.
     */
    private fun resetNewRecordForm() {
        resetForm()
    }

    /**
     * Resetea los campos del formulario de registro de un registro.
     * @param fieldToReset El campo a resetear. Si es nulo, se resetean todos.
     */
    private fun resetForm(fieldToReset: String? = null) {
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
        toggleFormButtons()
    }

    private fun clearAllFormsAndState() {
        editHojaDeRuta.setText("")
        formAndRecordsContainer.visibility = View.GONE
        historyAdapter.updateList(emptyList())
        resetNewRecordForm()
    }
}