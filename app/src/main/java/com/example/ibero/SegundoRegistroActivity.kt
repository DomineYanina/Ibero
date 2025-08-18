package com.example.ibero

import android.content.Intent
import android.os.Bundle
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
import com.example.ibero.ui.CurrentSessionInspectionAdapter
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SegundoRegistroActivity : AppCompatActivity() {

    private lateinit var viewModel: InspectionViewModel

    private lateinit var spinnerTipoCalidad: AutoCompleteTextView
    private lateinit var layoutTipoDeFalla: TextInputLayout
    private lateinit var spinnerTipoDeFalla: AutoCompleteTextView
    private lateinit var editMetrosDeTela: EditText
    private lateinit var btnGuardarRegistro: Button
    private lateinit var btnIncorporar: Button

    private lateinit var recyclerViewCurrentSessionRecords: RecyclerView
    private lateinit var currentSessionInspectionAdapter: CurrentSessionInspectionAdapter
    private lateinit var textSessionTitle: TextView

    // Nuevas variables para el estado de carga
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingOverlay: View

    // Datos de la primera pantalla que se mantienen
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

        // Obtener datos de la actividad anterior
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

        // Inicializar las nuevas vistas
        progressBar = findViewById(R.id.progress_bar)
        loadingOverlay = findViewById(R.id.loading_overlay)

        // Actualizar el título con el artículo
        textSessionTitle.text = "Registros ingresados para el artículo: $articulo"

        // Observar el estado de carga del ViewModel
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                progressBar.visibility = View.VISIBLE
                loadingOverlay.visibility = View.VISIBLE

                btnIncorporar.isEnabled = false
                btnGuardarRegistro.isEnabled = false
                spinnerTipoCalidad.isEnabled = false
                spinnerTipoDeFalla.isEnabled = false
                editMetrosDeTela.isEnabled = false
            } else {
                progressBar.visibility = View.GONE
                loadingOverlay.visibility = View.GONE

                btnIncorporar.isEnabled = true
                btnGuardarRegistro.isEnabled = true
                spinnerTipoCalidad.isEnabled = true
                spinnerTipoDeFalla.isEnabled = true
                editMetrosDeTela.isEnabled = true
            }
        }

        // Observa los mensajes de sincronización del ViewModel para mostrarlos al usuario
        viewModel.syncMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearSyncMessage()
            }
        }

        // Observa la lista de registros de la sesión actual
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

    private fun setupListeners() {
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

    private fun setupCurrentSessionRecyclerView() {
        currentSessionInspectionAdapter = CurrentSessionInspectionAdapter(mutableListOf())
        recyclerViewCurrentSessionRecords.layoutManager = LinearLayoutManager(this)
        recyclerViewCurrentSessionRecords.adapter = currentSessionInspectionAdapter
    }

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
                Toast.makeText(this@SegundoRegistroActivity, "Registro subido y finalizado.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@SegundoRegistroActivity, "No se pudo subir a la nube. Guardado localmente. Finalizando.", Toast.LENGTH_LONG).show()
            }

            val intent = Intent(this@SegundoRegistroActivity, HomeActivity::class.java)
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