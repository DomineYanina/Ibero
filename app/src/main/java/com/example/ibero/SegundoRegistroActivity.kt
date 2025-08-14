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
    private lateinit var editMetrosDeTela: EditText
    private lateinit var btnGuardarRegistro: Button // Botón original (guardar y finalizar)
    private lateinit var btnIncorporar: Button      // Nuevo botón (guardar y limpiar)

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

        // Observa el estado de sincronización.
        // La actividad solo se cerrará cuando el botón "Guardar Registro" sea presionado
        viewModel.syncMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearSyncMessage()
            }
        }
    }

    private fun initViews() {
        spinnerTipoCalidad = findViewById(R.id.spinner_tipo_calidad)
        layoutTipoDeFalla = findViewById(R.id.layout_tipo_de_falla)
        spinnerTipoDeFalla = findViewById(R.id.spinner_tipo_de_falla)
        editMetrosDeTela = findViewById(R.id.edit_metros_de_tela)
        btnGuardarRegistro = findViewById(R.id.btn_guardar_registro) // Botón original
        btnIncorporar = findViewById(R.id.btn_incorporar)            // Nuevo botón
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
        // Lógica para el nuevo botón "Incorporar"
        btnIncorporar.setOnClickListener {
            if (validateForm()) {
                saveInspectionAndResetForm()
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos obligatorios.", Toast.LENGTH_SHORT).show()
            }
        }

        // Lógica para el botón original "Guardar Registro"
        btnGuardarRegistro.setOnClickListener {
            if (validateForm()) {
                saveInspectionAndFinalize()
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos obligatorios.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveInspectionAndResetForm() {
        val tipoCalidad = spinnerTipoCalidad.text.toString()
        val tipoDeFalla = if (tipoCalidad == "Segunda") spinnerTipoDeFalla.text.toString() else null
        val metrosDeTela = editMetrosDeTela.text.toString().toDoubleOrNull() ?: 0.0

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

        lifecycleScope.launch {
            // Guardar localmente
            viewModel.insertInspection(newInspection)

            // Intentar sincronizar inmediatamente después de guardar
            viewModel.performSync()

            // Limpiar los campos para el siguiente registro
            resetForm()
            Toast.makeText(this@SegundoRegistroActivity, "Registro incorporado. Puede ingresar otro.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveInspectionAndFinalize() {
        val tipoCalidad = spinnerTipoCalidad.text.toString()
        val tipoDeFalla = if (tipoCalidad == "Segunda") spinnerTipoDeFalla.text.toString() else null
        val metrosDeTela = editMetrosDeTela.text.toString().toDoubleOrNull() ?: 0.0

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

        lifecycleScope.launch {
            // Guardar localmente
            viewModel.insertInspection(newInspection)

            // Intentar sincronizar inmediatamente después de guardar
            viewModel.performSync()

            // Finalizar la actividad
            Toast.makeText(this@SegundoRegistroActivity, "Registro finalizado. Sincronizando...", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun resetForm() {
        spinnerTipoCalidad.setText("", false)
        spinnerTipoDeFalla.setText("", false)
        layoutTipoDeFalla.visibility = View.GONE
        editMetrosDeTela.text.clear()
        // Los datos de la primera pantalla se mantienen en la memoria
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