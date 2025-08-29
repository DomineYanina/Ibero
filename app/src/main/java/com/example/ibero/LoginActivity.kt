package com.example.ibero

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
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

    private lateinit var apiService: GoogleSheetsApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializa Retrofit a través de la clase singleton que ya creaste
        apiService = GoogleSheetsApi2.service

        editUsername = findViewById(R.id.edit_username)
        editPassword = findViewById(R.id.edit_password)
        btnLogin = findViewById(R.id.btn_login)

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

        // Muestra una barra de progreso o deshabilita el botón si es necesario
        // showProgress(true)

        // Ejecuta la petición de red en una corrutina
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Llama a la nueva función directamente desde el servicio centralizado
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
                // Oculta la barra de progreso
                // showProgress(false)
            }
        }
    }
}