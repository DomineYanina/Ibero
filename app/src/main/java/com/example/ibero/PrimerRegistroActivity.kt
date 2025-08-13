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
    }

    private fun setupListeners() {
        editFecha.setOnClickListener {
            showDatePickerDialog()
        }

        btnNext.setOnClickListener {
            if (validateForm()) {
                val intent = Intent(this, SegundoRegistroActivity::class.java)

                // Obtener valores y manejar conversiones de forma segura
                val telarValue = spinnerTelar.text.toString().toIntOrNull() ?: 0
                val tintoreriaValue = editTintoreria.text.toString().toIntOrNull() ?: 0

                intent.putExtra("LOGGED_IN_USER", loggedInUser)
                intent.putExtra("FECHA", editFecha.text.toString())
                intent.putExtra("HOJA_DE_RUTA", formatHojaDeRuta(editHojaDeRuta.text.toString()))
                intent.putExtra("TEJEDURIA", spinnerTejeduria.text.toString())
                intent.putExtra("TELAR", telarValue)
                intent.putExtra("TINTORERIA", tintoreriaValue)
                intent.putExtra("ARTICULO", spinnerArticulo.text.toString())
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
        if (editHojaDeRuta.text.isNullOrBlank()) {
            findViewById<TextInputLayout>(R.id.layout_hoja_de_ruta).error = "Hoja de Ruta es obligatoria"
            isValid = false
        } else {
            findViewById<TextInputLayout>(R.id.layout_hoja_de_ruta).error = null
        }
        if (spinnerTejeduria.text.isNullOrBlank()) {
            findViewById<TextInputLayout>(R.id.layout_tejeduria).error = "Tejeduria es obligatoria"
            isValid = false
        } else {
            findViewById<TextInputLayout>(R.id.layout_tejeduria).error = null
        }
        if (spinnerTelar.text.isNullOrBlank()) {
            findViewById<TextInputLayout>(R.id.layout_telar).error = "Telar es obligatorio"
            isValid = false
        } else {
            findViewById<TextInputLayout>(R.id.layout_telar).error = null
        }
        if (editTintoreria.text.isNullOrBlank()) {
            findViewById<TextInputLayout>(R.id.layout_tintoreria).error = "Tintoreria es obligatoria"
            isValid = false
        } else {
            findViewById<TextInputLayout>(R.id.layout_tintoreria).error = null
        }
        if (spinnerArticulo.text.isNullOrBlank()) {
            findViewById<TextInputLayout>(R.id.layout_articulo).error = "Artículo es obligatorio"
            isValid = false
        } else {
            findViewById<TextInputLayout>(R.id.layout_articulo).error = null
        }
        return isValid
    }
}
