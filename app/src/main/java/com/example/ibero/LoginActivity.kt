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
    private lateinit var formContainer: LinearLayout // Agregamos una referencia al nuevo contenedor

    private lateinit var apiService: GoogleSheetsApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        apiService = GoogleSheetsApi2.service

        editUsername = findViewById(R.id.edit_username)
        editPassword = findViewById(R.id.edit_password)
        btnLogin = findViewById(R.id.btn_login)
        loadingSpinner = findViewById(R.id.loading_spinner)
        formContainer = findViewById(R.id.form_container) // Inicializamos el contenedor

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

        // 1. Mostrar el ícono de carga y deshabilitar el formulario
        showLoading(true)

        // Ejecuta la petición de red en una corrutina
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.checkUserCredentials(
                    username = username,
                    password = password
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.status == "SUCCESS") {
                        // Inicio de sesión exitoso
                        Toast.makeText(this@LoginActivity, "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                        intent.putExtra("LOGGED_IN_USER", username)
                        startActivity(intent)
                        finish()
                    } else {
                        // Inicio de sesión fallido
                        val errorMessage = response.body()?.message ?: "Usuario o contraseña incorrectos."
                        Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
                        Log.e("LoginActivity", "Login failed: ${response.errorBody()?.string() ?: "Unknown error"}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("LoginActivity", "Error de red o de API: ${e.message}")
                    Toast.makeText(this@LoginActivity, "Error de conexión. Intente de nuevo.", Toast.LENGTH_SHORT).show()
                }
            } finally {
                // 2. Ocultar el ícono de carga y habilitar el formulario, sin importar el resultado
                withContext(Dispatchers.Main) {
                    showLoading(false)
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            loadingSpinner.visibility = View.VISIBLE
            formContainer.alpha = 0.5f // Hace el formulario semitransparente
            formContainer.isEnabled = false
            for (i in 0 until formContainer.childCount) {
                val child = formContainer.getChildAt(i)
                child.isEnabled = false
            }
        } else {
            loadingSpinner.visibility = View.GONE
            formContainer.alpha = 1.0f // Restablece la transparencia
            formContainer.isEnabled = true
            for (i in 0 until formContainer.childCount) {
                val child = formContainer.getChildAt(i)
                child.isEnabled = true
            }
        }
    }
}