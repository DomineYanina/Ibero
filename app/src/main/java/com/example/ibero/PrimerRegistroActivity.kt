package com.example.ibero

import android.app.DatePickerDialog
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
import com.example.ibero.data.network.GoogleSheetsApi
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PrimerRegistroActivity : AppCompatActivity() {

    private lateinit var loggedInUser: String
    private lateinit var textUsuario: TextView
    private lateinit var editFecha: EditText
    private lateinit var editHojaDeRuta: EditText
    private lateinit var spinnerTejeduria: AutoCompleteTextView
    private lateinit var spinnerTelar: AutoCompleteTextView
    private lateinit var editTintoreria: EditText
    private lateinit var spinnerArticulo: AutoCompleteTextView
    private lateinit var editColor: EditText
    private lateinit var editRolloDeUrdido: EditText
    private lateinit var editOrden: EditText
    private lateinit var editCadena: EditText
    private lateinit var editAnchoDeRollo: EditText
    private lateinit var editEsmerilado: EditText
    private lateinit var editIgnifugo: EditText
    private lateinit var editImpermeable: EditText
    private lateinit var editOtro: EditText
    private lateinit var btnNext: Button
    private lateinit var btnCancelar: Button

    // --- CAMBIO AÑADIDO ---
    private lateinit var progressBar: ProgressBar
    // Lista de vistas a habilitar/deshabilitar
    private lateinit var inputFields: List<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_primer_registro)

        loggedInUser = intent.getStringExtra("LOGGED_IN_USER") ?: "Usuario Desconocido"

        initViews()
        setupSpinners()
        setupListeners()
        setCurrentDateTime()
    }

    private fun initViews() {
        textUsuario = findViewById(R.id.text_usuario)
        editFecha = findViewById(R.id.edit_fecha)
        editHojaDeRuta = findViewById(R.id.edit_hoja_de_ruta)
        spinnerTejeduria = findViewById(R.id.spinner_tejeduria)
        spinnerTelar = findViewById(R.id.spinner_telar)
        editTintoreria = findViewById(R.id.edit_tintoreria)
        spinnerArticulo = findViewById(R.id.spinner_articulo)
        editColor = findViewById(R.id.edit_color)
        editRolloDeUrdido = findViewById(R.id.edit_rollo_de_urdido)
        editOrden = findViewById(R.id.edit_orden)
        editCadena = findViewById(R.id.edit_cadena)
        editAnchoDeRollo = findViewById(R.id.edit_ancho_de_rollo)
        editEsmerilado = findViewById(R.id.edit_esmerilado)
        editIgnifugo = findViewById(R.id.edit_ignifugo)
        editImpermeable = findViewById(R.id.edit_impermeable)
        editOtro = findViewById(R.id.edit_otro)
        btnNext = findViewById(R.id.btn_next)
        btnCancelar = findViewById(R.id.btn_cancelar_primer_registro)

        // --- CAMBIO AÑADIDO ---
        progressBar = findViewById(R.id.progress_bar_loading)
        inputFields = listOf(
            editFecha, editHojaDeRuta, spinnerTejeduria, spinnerTelar, editTintoreria,
            spinnerArticulo, editColor, editRolloDeUrdido, editOrden, editCadena,
            editAnchoDeRollo, editEsmerilado, editIgnifugo, editImpermeable, editOtro,
            btnNext
        )

        textUsuario.text = "Usuario: $loggedInUser"
    }

    private fun setupSpinners() {
        val tejeduriaOptions = arrayOf("Somet", "GTM-2", "GTM-3")
        val tejeduriaAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, tejeduriaOptions)
        spinnerTejeduria.setAdapter(tejeduriaAdapter)

        val telarOptions = (1..36).map { it.toString() }.toTypedArray()
        val telarAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, telarOptions)
        spinnerTelar.setAdapter(telarAdapter)

        val articuloOptions = arrayOf(
            "75", "193", "530", "531", "569", "1101", "1015", "1080/2", "1080/16", "1080/F", "1116",
            "3004", "3005", "3008", "3010", "3013", "3034", "3036", "3073-TB", "3073-TB BLACK", "3073-TN",
            "3073-TN BLACK", "3075", "3080", "3080P", "3117", "3132", "3122/24", "3126", "3127", "3130", "DC"
        )
        val articuloAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, articuloOptions)
        spinnerArticulo.setAdapter(articuloAdapter)
    }

    private fun setupListeners() {
        editFecha.setOnClickListener {
            showDatePickerDialog()
        }

        btnNext.setOnClickListener {
            if (validateForm()) {
                // Validación local pasada, ahora verificar en Google Sheets
                checkHojaDeRutaExistence()
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos obligatorios.", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancelar.setOnClickListener {
            // Nuevo: Redirigir a HomeActivity al presionar "Cancelar"
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun checkHojaDeRutaExistence() {
        val hojaDeRuta = editHojaDeRuta.text.toString().trim()

        CoroutineScope(Dispatchers.IO).launch {
            // --- CAMBIO AÑADIDO: Mostrar estado de carga ---
            withContext(Dispatchers.Main) {
                setLoadingState(true)
            }

            try {
                // Llama al método de la API para verificar la existencia
                val response = GoogleSheetsApi.service.checkHojaRutaExistence(hojaDeRuta = hojaDeRuta)

                withContext(Dispatchers.Main) {
                    if (response.status == "SUCCESS") {
                        if (response.data.exists) {
                            // La hoja de ruta YA existe, mostrar error y limpiar campo
                            Toast.makeText(this@PrimerRegistroActivity, "Error: La hoja de ruta ya existe.", Toast.LENGTH_LONG).show()
                            editHojaDeRuta.text.clear()
                            editHojaDeRuta.requestFocus()
                        } else {
                            // La hoja de ruta NO existe, proceder al siguiente paso
                            navigateToNextActivity()
                        }
                    } else {
                        // Error del servidor (ej. Hoja no encontrada)
                        Toast.makeText(this@PrimerRegistroActivity, "Error en la verificación: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PrimerRegistroActivity, "Error de conexión. Intente nuevamente.", Toast.LENGTH_LONG).show()
                    Log.e("PrimerRegistroActivity", "Error al verificar hoja de ruta", e)
                }
            } finally {
                // --- CAMBIO AÑADIDO: Ocultar estado de carga, sin importar el resultado ---
                withContext(Dispatchers.Main) {
                    setLoadingState(false)
                }
            }
        }
    }

    // --- NUEVA FUNCIÓN: Manejar el estado de carga ---
    private fun setLoadingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        for (field in inputFields) {
            field.isEnabled = !isLoading
        }
    }

    private fun navigateToNextActivity() {
        val intent = Intent(this, SegundoRegistroActivity::class.java)

        val telarValue = spinnerTelar.text.toString().toIntOrNull() ?: 0
        val tintoreriaValue = editTintoreria.text.toString().toIntOrNull() ?: 0
        val colorValue = editColor.text.toString().toIntOrNull() ?: 0
        val rolloUrdidoValue = editRolloDeUrdido.text.toString().toIntOrNull() ?: 0
        val cadenaValue = editCadena.text.toString().toIntOrNull() ?: 0
        val anchoRolloValue = editAnchoDeRollo.text.toString().toIntOrNull() ?: 0

        intent.putExtra("LOGGED_IN_USER", loggedInUser)
        intent.putExtra("FECHA", editFecha.text.toString())
        intent.putExtra("HOJA_DE_RUTA", editHojaDeRuta.text.toString())
        intent.putExtra("TEJEDURIA", spinnerTejeduria.text.toString())
        intent.putExtra("TELAR", telarValue)
        intent.putExtra("TINTORERIA", tintoreriaValue)
        intent.putExtra("ARTICULO", spinnerArticulo.text.toString())
        intent.putExtra("COLOR", colorValue)
        intent.putExtra("ROLLO_DE_URDIDO", rolloUrdidoValue)
        intent.putExtra("ORDEN", editOrden.text.toString())
        intent.putExtra("CADENA", cadenaValue)
        intent.putExtra("ANCHO_DE_ROLLO", anchoRolloValue)
        intent.putExtra("ESMERILADO", editEsmerilado.text.toString())
        intent.putExtra("IGNIFUGO", editIgnifugo.text.toString())
        intent.putExtra("IMPERMEABLE", editImpermeable.text.toString())
        intent.putExtra("OTRO", editOtro.text.toString())

        startActivity(intent)
    }

    private fun setCurrentDateTime() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        editFecha.setText(dateFormat.format(calendar.time))
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDayOfMonth ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            editFecha.setText(dateFormat.format(selectedCalendar.time))
        }, year, month, day)
        datePickerDialog.show()
    }

    private fun formatHojaDeRuta(hojaDeRutaInput: String): String {
        val cleanedInput = hojaDeRutaInput.trim()
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR).toString().takeLast(2)
        return "$cleanedInput-a$year"
    }

    private fun validateForm(): Boolean {
        var isValid = true
        val requiredEditTexts = listOf(
            R.id.edit_hoja_de_ruta to "Hoja de Ruta es obligatoria",
            R.id.edit_tintoreria to "Tintoreria es obligatoria",
            R.id.edit_color to "Color es obligatorio",
            R.id.edit_rollo_de_urdido to "Rollo de urdido es obligatorio",
            R.id.edit_orden to "Orden es obligatoria",
            R.id.edit_cadena to "Cadena es obligatoria",
            R.id.edit_ancho_de_rollo to "Ancho de rollo es obligatorio"
        )

        for ((id, message) in requiredEditTexts) {
            val editText = findViewById<EditText>(id)
            if (editText.text.isNullOrBlank()) {
                findViewById<TextInputLayout>(parentLayoutId(id)).error = message
                isValid = false
            } else {
                findViewById<TextInputLayout>(parentLayoutId(id)).error = null
            }
        }

        val requiredSpinners = listOf(
            R.id.spinner_tejeduria to "Tejeduria es obligatoria",
            R.id.spinner_telar to "Telar es obligatorio",
            R.id.spinner_articulo to "Artículo es obligatorio"
        )

        for ((id, message) in requiredSpinners) {
            val spinner = findViewById<AutoCompleteTextView>(id)
            if (spinner.text.isNullOrBlank()) {
                findViewById<TextInputLayout>(parentLayoutId(id)).error = message
                isValid = false
            } else {
                findViewById<TextInputLayout>(parentLayoutId(id)).error = null
            }
        }

        return isValid
    }

    private fun parentLayoutId(childId: Int): Int {
        return when (childId) {
            R.id.edit_hoja_de_ruta -> R.id.layout_hoja_de_ruta
            R.id.spinner_tejeduria -> R.id.layout_tejeduria
            R.id.spinner_telar -> R.id.layout_telar
            R.id.edit_tintoreria -> R.id.layout_tintoreria
            R.id.spinner_articulo -> R.id.layout_articulo
            R.id.edit_color -> R.id.layout_color
            R.id.edit_rollo_de_urdido -> R.id.layout_rollo_de_urdido
            R.id.edit_orden -> R.id.layout_orden
            R.id.edit_cadena -> R.id.layout_cadena
            R.id.edit_ancho_de_rollo -> R.id.layout_ancho_de_rollo
            R.id.edit_esmerilado -> R.id.layout_esmerilado
            R.id.edit_ignifugo -> R.id.layout_ignifugo
            R.id.edit_impermeable -> R.id.layout_impermeable
            R.id.edit_otro -> R.id.layout_otro
            else -> throw IllegalArgumentException("Unknown child ID")
        }
    }
}