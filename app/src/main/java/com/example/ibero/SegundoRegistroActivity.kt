package com.example.ibero

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.example.ibero.InspectionViewModel
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.core.widget.addTextChangedListener
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog

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

    private lateinit var btnLimpiarCalidad: Button
    private lateinit var btnLimpiarFalla: Button
    private lateinit var btnLimpiarMetros: Button

    private lateinit var containerTipoDeFalla: LinearLayout

    private lateinit var recyclerViewCurrentSessionRecords: RecyclerView
    private lateinit var currentSessionInspectionAdapter: CurrentSessionInspectionAdapter
    private lateinit var textSessionTitle: TextView

    private lateinit var progressBar: ProgressBar
    private lateinit var loadingOverlay: View

    private var hasRegisteredAnItem = false
    private var editingInspection: Inspection? = null // NUEVA VARIABLE DE ESTADO

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

        btnIncorporar.isVisible = false
        btnGuardarRegistro.isVisible = false
        btnVolver.isVisible = true // A diferencia de la otra actividad, aquí el botón Volver siempre es visible hasta que se incorpore un ítem.

        setupFieldWatchers()

        textSessionTitle.text = "Registros ingresados para el artículo: $articulo"

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
                btnVolver.isVisible = false
                btnCancelar.text = "Ir a Inicio"
            }
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

        btnLimpiarCalidad = findViewById(R.id.btn_limpiar_calidad)
        btnLimpiarFalla = findViewById(R.id.btn_limpiar_falla)
        btnLimpiarMetros = findViewById(R.id.btn_limpiar_metros)

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
                if (editingInspection != null) {
                    updateInspectionAndSync()
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
        currentSessionInspectionAdapter = CurrentSessionInspectionAdapter(mutableListOf()) { inspection ->
            // Manejar clic en el item de la lista
            showEditConfirmationDialog(inspection)
        }
        recyclerViewCurrentSessionRecords.layoutManager = LinearLayoutManager(this)
        recyclerViewCurrentSessionRecords.adapter = currentSessionInspectionAdapter
    }

    // NUEVO MÉTODO: Muestra un diálogo para confirmar la edición
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

    // NUEVO MÉTODO: Precarga el formulario con los datos para la edición
    private fun preloadFormForEditing(inspection: Inspection) {
        editingInspection = inspection // Guarda el registro que se está editando

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
        if (hasRegisteredAnItem) {
            btnVolver.isVisible = false
            btnCancelar.isVisible = true
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
                if(editingInspection!=null){
                    btnGuardarRegistro.isVisible=false
                } else{
                    btnGuardarRegistro.isVisible=true
                }
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

    private fun updateInspectionAndSync() {
        val inspectionToUpdate = editingInspection?.copy(
            tipoCalidad = spinnerTipoCalidad.text.toString(),
            tipoDeFalla = if (spinnerTipoCalidad.text.toString() == "Segunda") spinnerTipoDeFalla.text.toString() else null,
            metrosDeTela = editMetrosDeTela.text.toString().toDoubleOrNull() ?: 0.0
        )

        if (inspectionToUpdate != null) {
            lifecycleScope.launch {
                // Llama al nuevo método del ViewModel y deja que el ViewModel
                // se encargue de mostrar y ocultar el estado de carga
                viewModel.updateInspectionAndSync(inspectionToUpdate)
                resetForm()
            }
        } else {
            Log.e("UpdateError", "No se pudo crear el objeto Inspection para actualizar. Objeto nulo.")
        }
    }

    private fun updateInspectionAndFinalize() {
        val inspectionToUpdate = editingInspection?.copy(
            tipoCalidad = spinnerTipoCalidad.text.toString(),
            tipoDeFalla = if (spinnerTipoCalidad.text.toString() == "Segunda") spinnerTipoDeFalla.text.toString() else null,
            metrosDeTela = editMetrosDeTela.text.toString().toDoubleOrNull() ?: 0.0
        )

        if (inspectionToUpdate != null) {
            lifecycleScope.launch {
                val success = viewModel.updateInspectionAndFinalize(inspectionToUpdate)

                viewModel.clearCurrentSessionList()

                if (success) {
                    Toast.makeText(this@SegundoRegistroActivity, "Registro actualizado y finalizado.", Toast.LENGTH_SHORT).show()
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

        // Si estamos editando, usa el uniqueId del objeto de edición, de lo contrario, genera uno nuevo
        val uniqueId = editingInspection?.uniqueId ?: UUID.randomUUID().toString()

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
                spinnerTipoCalidad.setText("", false)
                containerTipoDeFalla.visibility = View.GONE
                spinnerTipoDeFalla.setText("", false)
                editMetrosDeTela.text.clear()
            }
        }

        // Al resetear, también restauramos el estado de edición
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