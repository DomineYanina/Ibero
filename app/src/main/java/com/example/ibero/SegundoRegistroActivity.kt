package com.example.ibero

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.ibero.data.AppDatabase
import com.example.ibero.data.Inspection
import com.example.ibero.repository.InspectionRepository
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
    private lateinit var editMetrosDeTela: EditText // Nuevo: Metros de tela
    private lateinit var btnSave: Button

    // Datos de la primera pantalla
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
    private var anchoDeRolloParte1: Int = 0 // Nuevo: Ancho de rollo de la primera pantalla
    private lateinit var esmerilado: String
    private lateinit var ignifugo: String
    private lateinit var impermeable: String
    private lateinit var otro: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_segundo_registro)

        // Inicializar ViewModel
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = InspectionRepository(database.inspectionDao())
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
        color = intent.getIntExtra("COLOR", 0) // Nuevo
        rolloDeUrdido = intent.getIntExtra("ROLLO_DE_URDIDO", 0) // Nuevo
        orden = intent.getStringExtra("ORDEN") ?: "" // Nuevo
        cadena = intent.getIntExtra("CADENA", 0) // Nuevo
        anchoDeRolloParte1 = intent.getIntExtra("ANCHO_DE_ROLLO", 0) // Nuevo
        esmerilado = intent.getStringExtra("ESMERILADO") ?: "" // Nuevo
        ignifugo = intent.getStringExtra("IGNIFUGO") ?: "" // Nuevo
        impermeable = intent.getStringExtra("IMPERMEABLE") ?: "" // Nuevo
        otro = intent.getStringExtra("OTRO") ?: "" // Nuevo

        initViews()
        setupSpinners()
        setupListeners()

        // ** NUEVA LÓGICA: Observar el estado de sincronización **
        // Esto asegura que la actividad no se cierre hasta que la sincronización se complete
        viewModel.syncMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearSyncMessage()
                // Una vez que se ha recibido la respuesta de sincronización (éxito o error),
                // se puede cerrar la actividad.
                finish()
            }
        }
    }

    private fun initViews() {
        spinnerTipoCalidad = findViewById(R.id.spinner_tipo_calidad)
        layoutTipoDeFalla = findViewById(R.id.layout_tipo_de_falla)
        spinnerTipoDeFalla = findViewById(R.id.spinner_tipo_de_falla)
        editMetrosDeTela = findViewById(R.id.edit_metros_de_tela) // Nuevo
        btnSave = findViewById(R.id.btn_save)
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
        btnSave.setOnClickListener {
            saveInspection()
        }
    }

    private fun saveInspection() {
        if (!validateForm()) {
            Toast.makeText(this, "Por favor, complete todos los campos obligatorios.", Toast.LENGTH_SHORT).show()
            return
        }

        val tipoCalidad = spinnerTipoCalidad.text.toString()
        val tipoDeFalla = if (tipoCalidad == "Segunda") spinnerTipoDeFalla.text.toString() else null
        val metrosDeTela = editMetrosDeTela.text.toString().toDoubleOrNull() ?: 0.0 // Nuevo

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val inspectionDate = dateFormat.parse(fecha) ?: Date()
        val uniqueId = UUID.randomUUID().toString()

        val newInspection = Inspection(
            usuario = usuario,
            fecha = inspectionDate,
            hojaDeRuta = hojaDeRuta,
            tejeduria = tejeduria,
            telar = telar,
            tintoreria = tintoreria,
            articulo = articulo,
            color = color, // Nuevo
            rolloDeUrdido = rolloDeUrdido, // Nuevo
            orden = orden, // Nuevo
            cadena = cadena, // Nuevo
            anchoDeRollo = anchoDeRolloParte1, // Nuevo, de la primera pantalla
            esmerilado = esmerilado, // Nuevo
            ignifugo = ignifugo, // Nuevo
            impermeable = impermeable, // Nuevo
            otro = otro, // Nuevo
            tipoCalidad = tipoCalidad,
            tipoDeFalla = tipoDeFalla,
            metrosDeTela = metrosDeTela, // Nuevo
            uniqueId = uniqueId,
            imagePaths = emptyList(),
            imageUrls = emptyList()
        )

        lifecycleScope.launch {
            // Guardar localmente
            viewModel.insertInspection(newInspection)

            // Intentar sincronizar inmediatamente después de guardar
            viewModel.performSync()
        }
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