package com.example.ibero

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var editUsername: EditText
    private lateinit var editPassword: EditText
    private lateinit var btnLogin: Button

    // Lista de usuarios y contraseñas válidas
    private val validUsers = mapOf(
        "Juan" to "Juan7",
        "Omar" to "Omar1",
        "Susana" to "Sus86",
        "Mariela" to "Mar95",
        "Celeste" to "Cel45",
        "Pablo" to "Pablo30"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

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

        if (validUsers[username] == password) {
            Toast.makeText(this, "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("LOGGED_IN_USER", username)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Usuario o contraseña incorrectos.", Toast.LENGTH_SHORT).show()
        }
    }
}
