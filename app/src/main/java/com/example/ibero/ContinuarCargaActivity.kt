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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ibero.R
import com.example.ibero.data.HistoricalInspection
import com.example.ibero.data.Inspection
import com.example.ibero.ui.InspectionHistoryAdapter
import com.example.ibero.ui.InspectionViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class ContinuarCargaActivity : AppCompatActivity() {

// Vistas de la interfaz de usuario, actualizadas para el nuevo layout
    private lateinit var editHojaDeRuta: EditText
    private lateinit var btnBuscar: MaterialButton
    private lateinit var recyclerViewExistingRecords: RecyclerView
    private lateinit var formAndRecordsContainer: LinearLayout
    private lateinit var textExistingRecordsTitle: TextView
    private lateinit var loadingOverlay: View
    private lateinit var progressBar: View

// Nuevos elementos del formulario de ingreso
    private lateinit var spinnerTipoCalidad: AutoCompleteTextView
    private lateinit var layoutTipoFalla: TextInputLayout
    private lateinit var spinnerTipoFalla: AutoCompleteTextView
    private lateinit var editMetrosDeTela: EditText
    private lateinit var btnIncorporar: MaterialButton
    private lateinit var btnGuardarRegistro: MaterialButton
    private lateinit var viewModel: InspectionViewModel
    private lateinit var historyAdapter: InspectionHistoryAdapter
    private val client = OkHttpClient()
    private val TAG = "ContinuarCargaLog" // Etiqueta para los logs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_continuar_carga)
        // Inicialización de vistas, actualizadas para los nuevos IDs
        initViews()
        // Configuración del RecyclerView y el adaptador
        setupHistoryRecyclerView()
        // Configuración de listeners para los botones y spinners
        setupListeners()
        // Inicialización del ViewModel
        val factory = InspectionViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory)[InspectionViewModel::class.java]
    }

    private fun initViews() {
    // Vistas actualizadas
        recyclerViewExistingRecords = findViewById(R.id.recycler_view_existing_records)
        formAndRecordsContainer = findViewById(R.id.form_and_records_container)
        textExistingRecordsTitle = findViewById(R.id.text_existing_records_title)
        loadingOverlay = findViewById(R.id.loading_overlay)
        progressBar = findViewById(R.id.progress_bar)

        // Nuevas vistas del formulario de ingreso
        spinnerTipoCalidad = findViewById(R.id.spinner_tipo_calidad)
        layoutTipoFalla = findViewById(R.id.layout_tipo_de_falla)
        spinnerTipoFalla = findViewById(R.id.spinner_tipo_de_falla)
        editMetrosDeTela = findViewById(R.id.edit_metros_de_tela)
        btnIncorporar = findViewById(R.id.btn_incorporar)
        btnGuardarRegistro = findViewById(R.id.btn_guardar_registro)
    }

    private fun setupHistoryRecyclerView() {
        historyAdapter = InspectionHistoryAdapter(mutableListOf()) { historicalInspection ->
        // Manejar clics en el historial (opcional)
            Toast.makeText(this, "Hoja de Ruta: ${historicalInspection.hojaDeRuta}", Toast.LENGTH_SHORT).show()
        }
        recyclerViewExistingRecords.layoutManager = LinearLayoutManager(this)
        recyclerViewExistingRecords.adapter = historyAdapter
    }

    private fun setupListeners() {
        btnBuscar.setOnClickListener {
            val hojaDeRuta = editHojaDeRuta.text.toString().trim()
            if (hojaDeRuta.isNotEmpty()) {
                Log.d(TAG, "Botón 'Buscar' presionado. Hoja de Ruta: $hojaDeRuta")
                checkHojaRutaExistence(hojaDeRuta)
            } else {
                Toast.makeText(this, "Por favor, ingresa una Hoja de Ruta.", Toast.LENGTH_SHORT).show()
            }
        }

        // Listener para el spinner de Tipo de Calidad
        val calidadTypes = arrayOf("Primera", "Segunda")
        val calidadAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, calidadTypes)
        spinnerTipoCalidad.setAdapter(calidadAdapter)
        spinnerTipoCalidad.setOnItemClickListener { parent, view, position, id ->
            val selectedCalidad = parent.getItemAtPosition(position).toString()
            if (selectedCalidad == "Segunda") {
                layoutTipoFalla.visibility = View.VISIBLE
            } else {
                layoutTipoFalla.visibility = View.GONE
                spinnerTipoFalla.setText("")
            }
        }

        // Listener para el spinner de Tipo de Falla
        val fallaTypes = arrayOf("Aureolas", "Clareadas", "Falla de cadena", "Falla de trama", "Falla de urdido",
            "Gota Espaciada", "Goteras", "Hongos", "Mancha con patrón", "Manchas de aceite",
            "Marcas de sanforizado", "Parada de engomadora", "Parada telar", "Paradas",
            "Quebraduras", "Vainillas")
        val fallaAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, fallaTypes)
        spinnerTipoFalla.setAdapter(fallaAdapter)

        // Listener para el botón "Continuar" (btn_incorporar)
        btnIncorporar.setOnClickListener {
            saveInspectionRecord(hojaDeRuta = editHojaDeRuta.text.toString().trim(), final = false)
        }

        // Listener para el botón "Finalizar" (btn_guardar_registro)
        btnGuardarRegistro.setOnClickListener {
            saveInspectionRecord(hojaDeRuta = editHojaDeRuta.text.toString().trim(), final = true)
        }
    }

    private fun setViewsEnabled(enabled: Boolean) {
        editHojaDeRuta.isEnabled = enabled
        btnBuscar.isEnabled = enabled
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

    private fun checkHojaRutaExistence(hojaDeRuta: String) {
        setViewsEnabled(false)
        Toast.makeText(this@ContinuarCargaActivity, "Buscando Hoja de Ruta...", Toast.LENGTH_SHORT).show()

        val formBody = FormBody.Builder()
            .add("action", "checkHojaRutaExistence")
            .add("hojaDeRuta", hojaDeRuta)
            .build()

        val request = Request.Builder()
            .url("https://script.google.com/macros/s/AKfycbx1iM9b8wjFknfL9p2dKtqH6f6EWaKmpw4QEhlGYavC7j7YRUjtcprVnMpVpSCq4kJXYg/exec")
            .post(formBody)
            .build()

        Log.d(TAG, "Iniciando llamada a la API para verificar la existencia de la Hoja de Ruta.")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Error de red en checkHojaRutaExistence: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@ContinuarCargaActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                    setViewsEnabled(true)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { responseBody ->
                    Log.d(TAG, "Respuesta del servidor (Hoja de Ruta): $responseBody")
                    // Verificar si la respuesta es HTML en lugar de JSON
                    if (responseBody.startsWith("<!DOCTYPE html>")) {
                        Log.e(TAG, "El servidor devolvió una página HTML de error. La URL del script podría ser incorrecta o los permisos son insuficientes.")
                        runOnUiThread {
                            Toast.makeText(this@ContinuarCargaActivity, "Error: No se pudo conectar al servidor. Por favor, verifica la URL del script y los permisos.", Toast.LENGTH_LONG).show()
                            setViewsEnabled(true)
                        }
                        return@let
                    }
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val status = jsonResponse.getString("status")
                        val message = jsonResponse.getString("message")
                        Log.d(TAG, "Status: $status, Message: $message")

                        val dataObject = jsonResponse.optJSONObject("data")
                        val exists = dataObject?.optBoolean("exists", false) ?: false
                        Log.d(TAG, "Hoja de Ruta existe: $exists")

                        runOnUiThread {
                            if (status == "SUCCESS" && exists) {
                                // Se encontró la hoja de ruta. Proceder a cargar historial y mostrar formulario.
                                Toast.makeText(this@ContinuarCargaActivity, message, Toast.LENGTH_SHORT).show()
                                textExistingRecordsTitle.text = "Registros para la Hoja de Ruta: $hojaDeRuta"
                                formAndRecordsContainer.visibility = View.VISIBLE

                                // Se carga el historial automáticamente
                                findInspectionRecords(hojaDeRuta)
                            } else {
                                // No se encontró la hoja de ruta. El formulario no se muestra.
                                Toast.makeText(this@ContinuarCargaActivity, "No se encontró la Hoja de Ruta.", Toast.LENGTH_LONG).show()
                                formAndRecordsContainer.visibility = View.GONE
                                setViewsEnabled(true)
                            }
                        }
                    } catch (e: JSONException) {
                        Log.e(TAG, "Error al procesar la respuesta JSON: ${e.message}")
                        runOnUiThread {
                            Toast.makeText(this@ContinuarCargaActivity, "Error al procesar la respuesta del servidor: ${e.message}", Toast.LENGTH_LONG).show()
                            setViewsEnabled(true)
                        }
                    }
                } ?: run {
                    Log.e(TAG, "Respuesta del servidor vacía.")
                    runOnUiThread {
                        Toast.makeText(this@ContinuarCargaActivity, "Respuesta vacía del servidor.", Toast.LENGTH_LONG).show()
                        setViewsEnabled(true)
                    }
                }
            }
        })
    }

    private fun findInspectionRecords(hojaDeRuta: String) {
        setViewsEnabled(false)
        Toast.makeText(this@ContinuarCargaActivity, "Buscando historial...", Toast.LENGTH_SHORT).show()

        val formBody = FormBody.Builder()
            .add("action", "findInspectionRecords")
            .add("hojaDeRuta", hojaDeRuta)
            .build()

        val request = Request.Builder()
            .url("https://script.google.com/macros/s/AKfycbx1iM9b8wjFknfL9p2dKtqH6f6EWaKmpw4QEhlGYavC7j7YRUjtcprVnMpVpSCq4kJXYg/exec")
            .post(formBody)
            .build()

        Log.d(TAG, "Iniciando llamada a la API para encontrar historial.")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Error de red en findInspectionRecords: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@ContinuarCargaActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                    setViewsEnabled(true)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { responseBody ->
                    Log.d(TAG, "Respuesta del servidor (Historial): $responseBody")
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val status = jsonResponse.getString("status")
                        Log.d(TAG, "Status del historial: $status")

                        runOnUiThread {
                            if (status == "success") {
                                val dataArray = jsonResponse.getJSONObject("data").getJSONArray("data")
                                val inspectionRecords = mutableListOf<HistoricalInspection>()
                                for (i in 0 until dataArray.length()) {
                                    val record = dataArray.getJSONObject(i)
                                    val historicalInspection = HistoricalInspection(
                                        hojaDeRuta = record.getString("valorColumnaD"),
                                        articulo = record.getString("valorColumnaH"),
                                        tipoCalidad = record.optString("valorColumnaS", ""),
                                        // Usar optString para evitar crash si el valor es null
                                        tipoDeFalla = record.optString("valorColumnaT", null),
                                        // Usar optDouble para evitar crash si el valor es null
                                        metrosDeTela = record.optDouble("valorColumnaU", 0.0),
                                        fecha = record.getString("valorColumnaC")
                                    )
                                    inspectionRecords.add(historicalInspection)
                                }

                                historyAdapter.updateList(inspectionRecords)
                                Toast.makeText(this@ContinuarCargaActivity, "Historial cargado correctamente.", Toast.LENGTH_SHORT).show()
                                setViewsEnabled(true)
                            } else {
                                Toast.makeText(this@ContinuarCargaActivity, jsonResponse.getString("message"), Toast.LENGTH_LONG).show()
                                historyAdapter.updateList(emptyList()) // Limpia el historial
                                setViewsEnabled(true)
                            }
                        }

                    } catch (e: JSONException) {
                        Log.e(TAG, "Error al procesar los datos de historial: ${e.message}")
                        runOnUiThread {
                            Toast.makeText(this@ContinuarCargaActivity, "Error al procesar los datos de historial: ${e.message}", Toast.LENGTH_LONG).show()
                            setViewsEnabled(true)
                        }
                    }
                } ?: run {
                    Log.e(TAG, "Respuesta del historial vacía.")
                    runOnUiThread {
                        Toast.makeText(this@ContinuarCargaActivity, "Respuesta vacía del servidor.", Toast.LENGTH_LONG).show()
                        setViewsEnabled(true)
                    }
                }
            }
        })
    }

    private fun saveInspectionRecord(hojaDeRuta: String, final: Boolean) {
        // Validación de campos
        val tipoCalidad = spinnerTipoCalidad.text.toString().trim()
        val tipoFalla = if (layoutTipoFalla.visibility == View.VISIBLE) spinnerTipoFalla.text.toString().trim() else ""
        val metrosDeTela = editMetrosDeTela.text.toString().trim()

        if (tipoCalidad.isEmpty() || metrosDeTela.isEmpty() || (layoutTipoFalla.visibility == View.VISIBLE && tipoFalla.isEmpty())) {
            Toast.makeText(this, "Por favor, completa todos los campos del nuevo registro.", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Validación de campos fallida.")
            return
        }

        // Aquí iría la lógica para enviar los datos al servidor o guardarlos localmente
        // Por ahora, solo mostraremos un mensaje
        Toast.makeText(this, "Registro guardado para la Hoja de Ruta: $hojaDeRuta. Tipo: $tipoCalidad", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Registro a guardar: Hoja de Ruta: $hojaDeRuta, Tipo Calidad: $tipoCalidad, Metros: $metrosDeTela")

        if (final) {
            // Lógica para el botón "Finalizar": limpiar todo y regresar a un estado inicial.
            clearForm()
            Log.d(TAG, "Limpiando formulario completo.")
        } else {
            // Lógica para el botón "Continuar": limpiar el formulario de ingreso de datos
            clearNewRecordForm()
            Log.d(TAG, "Limpiando formulario de nuevo registro.")
        }
    }

    private fun clearForm() {
        editHojaDeRuta.setText("")
        formAndRecordsContainer.visibility = View.GONE
        historyAdapter.updateList(emptyList())
        clearNewRecordForm()
    }

    private fun clearNewRecordForm() {
        spinnerTipoCalidad.setText("", false)
        layoutTipoFalla.visibility = View.GONE
        spinnerTipoFalla.setText("", false)
        editMetrosDeTela.setText("")
    }
}