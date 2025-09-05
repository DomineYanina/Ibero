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
import androidx.lifecycle.lifecycleScope
import com.example.ibero.data.AppDatabase
import com.example.ibero.data.HojaDeRutaDao
import com.example.ibero.data.network.GoogleSheetsApi2
import com.example.ibero.data.network.GoogleSheetsApiService
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
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

    private lateinit var progressBar: ProgressBar
    private lateinit var inputFields: List<View>

    private lateinit var progressBarTejeduria: ProgressBar
    private lateinit var progressBarTelar: ProgressBar

    // Se mantiene el servicio de API para la verificación de la hoja de ruta
    private lateinit var apiService: GoogleSheetsApiService
    private lateinit var database: AppDatabase

    private lateinit var hojaDeRutaDao: HojaDeRutaDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_primer_registro)

        loggedInUser = intent.getStringExtra("LOGGED_IN_USER") ?: "Usuario Desconocido"

        apiService = GoogleSheetsApi2.service
        database = AppDatabase.getDatabase(this)
        hojaDeRutaDao = database.hojaDeRutaDao()

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

        progressBar = findViewById(R.id.progress_bar_loading)
        progressBarTejeduria = findViewById(R.id.progress_bar_loading)
        progressBarTelar = findViewById(R.id.progress_bar_loading)

        inputFields = listOf(
            editFecha, editHojaDeRuta, spinnerTejeduria, spinnerTelar, editTintoreria,
            spinnerArticulo, editColor, editRolloDeUrdido, editOrden, editCadena,
            editAnchoDeRollo, editEsmerilado, editIgnifugo, editImpermeable, editOtro,
            btnNext
        )

        textUsuario.text = "Usuario: $loggedInUser"
    }

    private fun setupSpinners() {
        fetchTejeduriasFromDb()
        fetchTelaresFromDb()
        fetchArticulosFromDb()
    }

    private fun fetchTejeduriasFromDb() {
        progressBarTejeduria.visibility = View.VISIBLE
        spinnerTejeduria.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            database.tejeduriaDao().getAllTejedurias().collect { tejedurias ->
                withContext(Dispatchers.Main) {
                    val tejeduriaAdapter = ArrayAdapter(
                        this@PrimerRegistroActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        tejedurias
                    )
                    spinnerTejeduria.setAdapter(tejeduriaAdapter)
                    progressBarTejeduria.visibility = View.GONE
                    spinnerTejeduria.isEnabled = true
                }
            }
        }
    }

    private fun fetchTelaresFromDb() {
        progressBarTelar.visibility = View.VISIBLE
        spinnerTelar.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            database.telarDao().getAllTelares().collect { telares ->
                withContext(Dispatchers.Main) {
                    val telarStrings = telares.map { it.toString() }
                    val telarAdapter = ArrayAdapter(
                        this@PrimerRegistroActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        telarStrings
                    )
                    spinnerTelar.setAdapter(telarAdapter)
                    progressBarTelar.visibility = View.GONE
                    spinnerTelar.isEnabled = true
                }
            }
        }
    }

    private fun fetchArticulosFromDb() {
        progressBar.visibility = View.VISIBLE
        spinnerArticulo.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            database.articuloDao().getAllArticulos().collect { articulos ->
                withContext(Dispatchers.Main) {
                    val articuloAdapter = ArrayAdapter(
                        this@PrimerRegistroActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        articulos
                    )
                    spinnerArticulo.setAdapter(articuloAdapter)
                    progressBar.visibility = View.GONE
                    spinnerArticulo.isEnabled = true
                }
            }
        }
    }

    private fun setupListeners() {
        editFecha.setOnClickListener {
            showDatePickerDialog()
        }

        btnNext.setOnClickListener {
            if (validateForm()) {
                checkHojaDeRutaExistence()
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
    }

    private fun checkHojaDeRutaExistence() {
        val hojaDeRuta = editHojaDeRuta.text.toString().trim()

        // Manejar el caso de campo vacío antes de la verificación
        if (hojaDeRuta.isEmpty()) {
            Toast.makeText(this, "El campo no puede estar vacío.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { setLoadingState(true) }
            try {
                // Reemplazamos la llamada a la API por la consulta al DAO
                val existsInDb = hojaDeRutaDao.exists(hojaDeRuta)

                withContext(Dispatchers.Main) {
                    if (existsInDb) {
                        Toast.makeText(this@PrimerRegistroActivity, "Error: La hoja de ruta ya existe.", Toast.LENGTH_LONG).show()
                        editHojaDeRuta.text.clear()
                        editHojaDeRuta.requestFocus()
                    } else {
                        navigateToNextActivity()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Manejo de errores de la base de datos
                    Toast.makeText(this@PrimerRegistroActivity, "Error al verificar la hoja de ruta.", Toast.LENGTH_LONG).show()
                    Log.e("PrimerRegistroActivity", "Error al verificar hoja de ruta en la base de datos", e)
                }
            } finally {
                withContext(Dispatchers.Main) { setLoadingState(false) }
            }
        }
    }

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
