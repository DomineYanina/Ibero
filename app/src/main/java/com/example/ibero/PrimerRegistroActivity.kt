package com.example.ibero

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
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
    private lateinit var editEsmerilado: EditText // Modificado a EditText
    private lateinit var editIgnifugo: EditText // Modificado a EditText
    private lateinit var editImpermeable: EditText // Modificado a EditText
    private lateinit var editOtro: EditText
    private lateinit var btnNext: Button

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
        editEsmerilado = findViewById(R.id.edit_esmerilado) // Modificado a EditText
        editIgnifugo = findViewById(R.id.edit_ignifugo) // Modificado a EditText
        editImpermeable = findViewById(R.id.edit_impermeable) // Modificado a EditText
        editOtro = findViewById(R.id.edit_otro)
        btnNext = findViewById(R.id.btn_next)

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

        // Ya no se configuran los adaptadores para los campos que ahora son de texto libre
    }

    private fun setupListeners() {
        editFecha.setOnClickListener {
            showDatePickerDialog()
        }

        btnNext.setOnClickListener {
            if (validateForm()) {
                val intent = Intent(this, SegundoRegistroActivity::class.java)

                val telarValue = spinnerTelar.text.toString().toIntOrNull() ?: 0
                val tintoreriaValue = editTintoreria.text.toString().toIntOrNull() ?: 0
                val colorValue = editColor.text.toString().toIntOrNull() ?: 0
                val rolloUrdidoValue = editRolloDeUrdido.text.toString().toIntOrNull() ?: 0
                val cadenaValue = editCadena.text.toString().toIntOrNull() ?: 0
                val anchoRolloValue = editAnchoDeRollo.text.toString().toIntOrNull() ?: 0

                intent.putExtra("LOGGED_IN_USER", loggedInUser)
                intent.putExtra("FECHA", editFecha.text.toString())
                intent.putExtra("HOJA_DE_RUTA", formatHojaDeRuta(editHojaDeRuta.text.toString()))
                intent.putExtra("TEJEDURIA", spinnerTejeduria.text.toString())
                intent.putExtra("TELAR", telarValue)
                intent.putExtra("TINTORERIA", tintoreriaValue)
                intent.putExtra("ARTICULO", spinnerArticulo.text.toString())
                intent.putExtra("COLOR", colorValue)
                intent.putExtra("ROLLO_DE_URDIDO", rolloUrdidoValue)
                intent.putExtra("ORDEN", editOrden.text.toString())
                intent.putExtra("CADENA", cadenaValue)
                intent.putExtra("ANCHO_DE_ROLLO", anchoRolloValue)
                intent.putExtra("ESMERILADO", editEsmerilado.text.toString()) // Ahora se obtiene de EditText
                intent.putExtra("IGNIFUGO", editIgnifugo.text.toString()) // Ahora se obtiene de EditText
                intent.putExtra("IMPERMEABLE", editImpermeable.text.toString()) // Ahora se obtiene de EditText
                intent.putExtra("OTRO", editOtro.text.toString())

                startActivity(intent)
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos obligatorios.", Toast.LENGTH_SHORT).show()
            }
        }
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
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR).toString().takeLast(2)
        return "${hojaDeRutaInput.padStart(2, '0')}-a$year"
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

    // Helper function to find the parent TextInputLayout ID from the child EditText ID
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