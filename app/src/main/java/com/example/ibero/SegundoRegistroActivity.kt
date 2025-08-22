package com.example.ibero

import android.content.Intent
import android.os.Bundle
import android.widget.*
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ibero.data.Inspection
import com.example.ibero.ui.CurrentSessionInspectionAdapter
import com.example.ibero.ui.InspectionViewModel
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.core.widget.addTextChangedListener
import android.widget.LinearLayout // Importa la clase LinearLayout

class SegundoRegistroActivity : AppCompatActivity() {

    private lateinit var viewModel: InspectionViewModel

    private lateinit var spinnerTipoCalidad: AutoCompleteTextView
    private lateinit var layoutTipoDeFalla: TextInputLayout
    private lateinit var spinnerTipoDeFalla: AutoCompleteTextView
    private lateinit var editMetrosDeTela: EditText
    private lateinit var btnGuardarRegistro: Button
    private lateinit var btnIncorporar: Button
    private lateinit var btnCancelar: Button
    private lateinit var btnVolver: Button

    // Nuevas variables para los botones de limpieza
    private lateinit var btnLimpiarCalidad: Button
    private lateinit var btnLimpiarFalla: Button
    private lateinit var btnLimpiarMetros: Button

    // Nueva variable para el contenedor de tipo de falla
    private lateinit var containerTipoDeFalla: LinearLayout

    private lateinit var recyclerViewCurrentSessionRecords: RecyclerView
    private lateinit var currentSessionInspectionAdapter: CurrentSessionInspectionAdapter
    private lateinit var textSessionTitle: TextView

    private lateinit var progressBar: ProgressBar
    private lateinit var loadingOverlay: View

    // 1) Nueva variable de estado para controlar la visibilidad del botón Volver
    private var hasRegisteredAnItem = false

    // Datos recibidos
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
        setContentView(R.layout.activity_segundo_registro)

        val factory = InspectionViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory).get(InspectionViewModel::class.java)

        // Datos de la Activity anterior
        usuario = intent.getStringExtra("LOGGED_IN_USER") ?: "Usuario Desconocido"
        fecha = intent.getStringExtra("FECHA") ?: ""
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

        initViews()
        setupSpinners()
        setupListeners()
        setupCurrentSessionRecyclerView()

        progressBar = findViewById(R.id.progress_bar)
        loadingOverlay = findViewById(R.id.loading_overlay)

        // 🔹 1) Al iniciar, Continuar y Finalizar deshabilitados
        btnIncorporar.isVisible = false
        btnGuardarRegistro.isVisible = false

        // 🔹 2) Monitoreo de campos
        setupFieldWatchers()

        textSessionTitle.text = "Registros ingresados para el artículo: $articulo"

        // Observa el estado de carga del ViewModel
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

        // Observa los mensajes de sincronización
        viewModel.syncMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearSyncMessage()
            }
        }

        // Observa la lista de registros
        viewModel.currentSessionInspections.observe(this) { inspections ->
            currentSessionInspectionAdapter.updateList(inspections)
        }
    }

    private fun initViews() {
        spinnerTipoCalidad = findViewById(R.id.spinner_tipo_calidad)
        layoutTipoDeFalla = findViewById(R.id.layout_tipo_de_falla)
        spinnerTipoDeFalla = findViewById(R.id.spinner_tipo_de_falla)
        editMetrosDeTela = findViewById(R.id.edit_metros_de_tela)
        btnGuardarRegistro = findViewById(R.id.btn_guardar_registro)
        btnIncorporar = findViewById(R.id.btn_incorporar)
        recyclerViewCurrentSessionRecords = findViewById(R.id.recycler_view_current_session_records)
        textSessionTitle = findViewById(R.id.text_session_title)
        btnCancelar = findViewById(R.id.btn_cancelar_segundo_registro)
        btnVolver = findViewById(R.id.btn_volver)

        // Inicializar los nuevos botones
        btnLimpiarCalidad = findViewById(R.id.btn_limpiar_calidad)
        btnLimpiarFalla = findViewById(R.id.btn_limpiar_falla)
        btnLimpiarMetros = findViewById(R.id.btn_limpiar_metros)

        // Inicializar el contenedor de tipo de falla
        containerTipoDeFalla = findViewById(R.id.container_tipo_falla)
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
                containerTipoDeFalla.visibility = View.VISIBLE
            } else {
                containerTipoDeFalla.visibility = View.GONE
                spinnerTipoDeFalla.setText("", false)
            }
        }
    }

    private fun setupListeners() {
        btnIncorporar.setOnClickListener {
            if (validateForm()) {
                saveInspectionAndResetForm()

                // 2) Actualiza el estado cuando se incorpora un registro
                hasRegisteredAnItem = true
                btnVolver.isVisible = false
                btnCancelar.text = "Ir a Inicio"
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

        btnCancelar.setOnClickListener {
            val loggedInUser = intent.getStringExtra("LOGGED_IN_USER") ?: "Usuario Desconocido"
            val homeIntent = Intent(this, HomeActivity::class.java)
            homeIntent.putExtra("LOGGED_IN_USER", loggedInUser)
            startActivity(homeIntent)
            finish()
        }

        btnVolver.setOnClickListener {
            val backIntent = Intent(this, PrimerRegistroActivity::class.java)
            backIntent.putExtras(intent.extras ?: Bundle())
            startActivity(backIntent)
            finish()
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

    private fun setupCurrentSessionRecyclerView() {
        currentSessionInspectionAdapter = CurrentSessionInspectionAdapter(mutableListOf())
        recyclerViewCurrentSessionRecords.layoutManager = LinearLayoutManager(this)
        recyclerViewCurrentSessionRecords.adapter = currentSessionInspectionAdapter
    }

    private fun setupFieldWatchers() {
        editMetrosDeTela.addTextChangedListener { toggleButtonsBasedOnInput() }
        spinnerTipoCalidad.addTextChangedListener { toggleButtonsBasedOnInput() }
        spinnerTipoDeFalla.addTextChangedListener { toggleButtonsBasedOnInput() }
    }

    private fun toggleButtonsBasedOnInput() {
        // 3) Ajusta la lógica de visibilidad para que dependa del nuevo estado
        if (hasRegisteredAnItem) {
            btnVolver.isVisible = false
            btnCancelar.isVisible = true // Mantener visible el botón Cancelar/Ir a Inicio
            btnIncorporar.isVisible = true
            btnGuardarRegistro.isVisible = true
        } else {
            val hayInput = spinnerTipoCalidad.text.isNotBlank() ||
                    spinnerTipoDeFalla.text.isNotBlank() ||
                    editMetrosDeTela.text.isNotBlank()

            if (hayInput) {
                btnCancelar.isVisible = false
                btnVolver.isVisible = false
                btnIncorporar.isVisible = true
                btnGuardarRegistro.isVisible = true
            } else {
                btnCancelar.isVisible = true
                btnVolver.isVisible = true
                btnIncorporar.isVisible = false
                btnGuardarRegistro.isVisible = false
            }
        }
    }

    private fun saveInspectionAndResetForm() {
        val inspection = createInspectionObject()
        lifecycleScope.launch {
            viewModel.insertInspection(inspection)
            resetForm()
        }
    }

    private fun saveInspectionAndFinalize() {
        val inspection = createInspectionObject()
        lifecycleScope.launch {
            val success = viewModel.finalizeAndSync(inspection)
            viewModel.clearCurrentSessionList()
            if (success) {
                Toast.makeText(this@SegundoRegistroActivity, "Registro subido y finalizado.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@SegundoRegistroActivity, "No se pudo subir a la nube. Guardado localmente. Finalizando.", Toast.LENGTH_LONG).show()
            }
            val intent = Intent(this@SegundoRegistroActivity, HomeActivity::class.java)
            intent.putExtra("LOGGED_IN_USER", usuario)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun createInspectionObject(): Inspection {
        val tipoCalidad = spinnerTipoCalidad.text.toString()
        val tipoDeFalla = if (tipoCalidad == "Segunda") spinnerTipoDeFalla.text.toString() else null
        val metrosDeTela = editMetrosDeTela.text.toString().toDoubleOrNull() ?: 0.0

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val inspectionDate = dateFormat.parse(fecha) ?: Date()
        val uniqueId = UUID.randomUUID().toString()

        return Inspection(
            usuario = usuario,
            fecha = inspectionDate,
            hojaDeRuta = hojaDeRuta,
            tejeduria = tejeduria,
            telar = telar,
            tintoreria = tintoreria,
            articulo = articulo,
            color = color,
            rolloDeUrdido = rolloDeUrdido,
            orden = orden,
            cadena = cadena,
            anchoDeRollo = anchoDeRolloParte1,
            esmerilado = esmerilado,
            ignifugo = ignifugo,
            impermeable = impermeable,
            otro = otro,
            tipoCalidad = tipoCalidad,
            tipoDeFalla = tipoDeFalla,
            metrosDeTela = metrosDeTela,
            uniqueId = uniqueId,
            imagePaths = emptyList(),
            imageUrls = emptyList()
        )
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
                // Limpia todos los campos si no se especifica
                spinnerTipoCalidad.setText("", false)
                containerTipoDeFalla.visibility = View.GONE
                spinnerTipoDeFalla.setText("", false)
                editMetrosDeTela.text.clear()
            }
        }
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