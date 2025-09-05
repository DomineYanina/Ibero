package com.example.ibero

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ibero.data.AppDatabase
import com.example.ibero.data.Articulo
import com.example.ibero.data.HojaDeRuta
import com.example.ibero.data.Tejeduria
import com.example.ibero.data.Telar
import com.example.ibero.data.TipoDeFalla
import com.example.ibero.data.network.GoogleSheetsApi2
import com.example.ibero.data.network.GoogleSheetsApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var editUsername: EditText
    private lateinit var editPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var formContainer: LinearLayout

    private lateinit var apiService: GoogleSheetsApiService
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        apiService = GoogleSheetsApi2.service
        database = AppDatabase.getDatabase(this)

        editUsername = findViewById(R.id.edit_username)
        editPassword = findViewById(R.id.edit_password)
        btnLogin = findViewById(R.id.btn_login)
        loadingSpinner = findViewById(R.id.loading_spinner)
        formContainer = findViewById(R.id.form_container)

        btnLogin.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {
        val username = editUsername.text.toString().trim()
        val password = editPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, ingrese usuario y contraseña.", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.checkUserCredentials(
                    username = username,
                    password = password
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.status == "SUCCESS") {
                        Toast.makeText(this@LoginActivity, "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show()
                        synchronizeAndNavigate(username)
                    } else {
                        val errorMessage = response.body()?.message ?: "Usuario o contraseña incorrectos."
                        Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
                        Log.e("LoginActivity", "Login failed: ${response.errorBody()?.string() ?: "Unknown error"}")
                        showLoading(false)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("LoginActivity", "Error de red o de API: ${e.message}")
                    Toast.makeText(this@LoginActivity, "Error de conexión. Intente de nuevo.", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            loadingSpinner.visibility = View.VISIBLE
            formContainer.alpha = 0.5f
            formContainer.isEnabled = false
            for (i in 0 until formContainer.childCount) {
                val child = formContainer.getChildAt(i)
                child.isEnabled = false
            }
        } else {
            loadingSpinner.visibility = View.GONE
            formContainer.alpha = 1.0f
            formContainer.isEnabled = true
            for (i in 0 until formContainer.childCount) {
                val child = formContainer.getChildAt(i)
                child.isEnabled = true
            }
        }
    }

    private fun synchronizeAndNavigate(username: String) {
        Toast.makeText(this, "Sincronizando datos de referencia...", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Sincronizar Artículos
                val articulosResponse = apiService.getArticulos()
                if (articulosResponse.isSuccessful) {
                    val articulos = articulosResponse.body()?.data?.articulos?.map { Articulo(nombre = it) }
                    articulos?.let {
                        database.articuloDao().deleteAll()
                        database.articuloDao().insertAll(it)
                    }
                }

                // Sincronizar Tipos de Fallas
                val fallasResponse = apiService.getTiposDeFallas()
                if (fallasResponse.isSuccessful) {
                    val tiposDeFalla = fallasResponse.body()?.data?.tiposDeFallas?.map { TipoDeFalla(nombre = it) }
                    tiposDeFalla?.let {
                        database.tipoDeFallaDao().deleteAll()
                        database.tipoDeFallaDao().insertAll(it)
                    }
                }

                // Sincronizar Tejedurías
                val tejeduriasResponse = apiService.getTejedurias()
                if (tejeduriasResponse.isSuccessful) {
                    val tejedurias = tejeduriasResponse.body()?.data?.tejedurias?.map { Tejeduria(nombre = it) }
                    tejedurias?.let {
                        database.tejeduriaDao().deleteAll()
                        database.tejeduriaDao().insertAll(it)
                    }
                }

                // Sincronizar Telares
                val telaresResponse = apiService.getTelares()
                if (telaresResponse.isSuccessful) {
                    val telares = telaresResponse.body()?.data?.telares?.map { Telar(numero = it) }
                    telares?.let {
                        database.telarDao().deleteAll()
                        database.telarDao().insertAll(it)
                    }
                }

                // Sincronizar Hojas de Ruta
                val hojasDeRutaReponse = apiService.getHojasDeRutaExistentes()
                if (hojasDeRutaReponse.isSuccessful) {
                    val hojasDeRutaStrings = hojasDeRutaReponse.body()?.data?.hojasDeRuta
                    val hojasDeRuta = hojasDeRutaStrings?.map { HojaDeRuta(nombre = it) }
                    hojasDeRuta?.let {
                        database.hojaDeRutaDao().deleteAll()
                        database.hojaDeRutaDao().insertAll(it)
                    }
                }

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@LoginActivity, "Sincronización de datos completada.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                    intent.putExtra("LOGGED_IN_USER", username)
                    startActivity(intent)
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("LoginActivity", "Error de sincronización: ${e.message}")
                    Toast.makeText(this@LoginActivity, "Error al sincronizar datos. Intente de nuevo más tarde.", Toast.LENGTH_LONG).show()
                    showLoading(false)
                }
            }
        }
    }
}
