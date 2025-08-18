package com.example.ibero

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ibero.data.TonalidadItem
import com.example.ibero.data.network.GoogleSheetsApi
import com.example.ibero.ui.TonalidadesAdapter
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TonalidadesActivity : AppCompatActivity() {

    private lateinit var editHojaRuta: EditText
    private lateinit var btnBuscar: Button
    private lateinit var btnGuardar: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TonalidadesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tonalidades)

        initViews()
        setupListeners()
        setupRecyclerView()
    }

    private fun initViews() {
        editHojaRuta = findViewById(R.id.edit_hoja_ruta_tonalidad)
        btnBuscar = findViewById(R.id.btn_buscar_tonalidades)
        btnGuardar = findViewById(R.id.btn_guardar_tonalidades)
        recyclerView = findViewById(R.id.recycler_view_tonalidades)
    }

    private fun setupListeners() {
        btnBuscar.setOnClickListener {
            val hojaRuta = editHojaRuta.text.toString().trim()
            if (hojaRuta.isEmpty()) {
                Toast.makeText(this, "Por favor, ingrese una hoja de ruta.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            buscarRegistrosPorHojaRuta(hojaRuta)
        }

        btnGuardar.setOnClickListener {
            guardarTonalidades()
        }
    }

    private fun setupRecyclerView() {
        adapter = TonalidadesAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun buscarRegistrosPorHojaRuta(hojaRuta: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Llama al método de búsqueda en la API que configuraste
                val response = GoogleSheetsApi.service.findTonalidades(hojaDeRuta = hojaRuta)

                withContext(Dispatchers.Main) {
                    if (response.status == "success") {
                        val items = response.data.map {
                            // Mapea la respuesta de la API a tu modelo de datos local (TonalidadItem)
                            TonalidadItem(
                                uniqueId = it.rowNumber.toString(), // Usamos el número de fila como ID
                                valorColumnaT = it.valorColumnaT
                            )
                        }

                        if (items.isEmpty()) {
                            Toast.makeText(this@TonalidadesActivity, "No se encontraron registros para esa hoja de ruta.", Toast.LENGTH_LONG).show()
                            adapter.updateList(mutableListOf())
                            btnGuardar.visibility = View.GONE
                        } else {
                            adapter.updateList(items.toMutableList())
                            btnGuardar.visibility = View.VISIBLE
                        }
                    } else {
                        Toast.makeText(this@TonalidadesActivity, "Error en la búsqueda: ${response.message}", Toast.LENGTH_LONG).show()
                        adapter.updateList(mutableListOf())
                        btnGuardar.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TonalidadesActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("TonalidadesActivity", "Error al buscar registros", e)
                    adapter.updateList(mutableListOf())
                    btnGuardar.visibility = View.GONE
                }
            }
        }
    }

    private fun guardarTonalidades() {
        val registrosAActualizar = adapter.getUpdatedItems()
        if (registrosAActualizar.isEmpty()) {
            Toast.makeText(this, "No hay tonalidades para guardar.", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Convierte la lista de items a un JSON string para enviarlo al Apps Script
                val gson = Gson()
                val updatesJson = gson.toJson(registrosAActualizar.map {
                    // Mapea a un formato simple para el Apps Script
                    mapOf("rowNumber" to it.uniqueId.toInt(), "nuevaTonalidad" to it.nuevaTonalidad)
                })

                // Llama al método de actualización en la API
                val response = GoogleSheetsApi.service.updateTonalidades(updates = updatesJson)

                withContext(Dispatchers.Main) {
                    if (response.status == "success") {
                        Toast.makeText(this@TonalidadesActivity, "Tonalidades guardadas exitosamente.", Toast.LENGTH_SHORT).show()
                        finish() // Regresa a la vista anterior
                    } else {
                        Toast.makeText(this@TonalidadesActivity, "Error al guardar: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TonalidadesActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("TonalidadesActivity", "Error al guardar registros", e)
                }
            }
        }
    }
}