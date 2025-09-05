package com.example.ibero

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var btnRegisterInspection: Button
    private lateinit var btnUpdateTonalidad: Button
    private lateinit var btnViewInspections: Button
    private lateinit var textWelcome: TextView
    private lateinit var loggedInUser: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        btnRegisterInspection = findViewById(R.id.btn_new_record)
        btnUpdateTonalidad = findViewById(R.id.btn_agregar_tonalidades)
        btnViewInspections = findViewById(R.id.btn_continuar_carga)
        textWelcome = findViewById(R.id.text_welcome)

        // Obtiene el nombre de usuario del Intent y actualiza el TextView
        loggedInUser = intent.getStringExtra("LOGGED_IN_USER") ?: "Usuario Desconocido"
        textWelcome.text = "Bienvenido, $loggedInUser"

        btnRegisterInspection.setOnClickListener {
            val intent = Intent(this, PrimerRegistroActivity::class.java)
            intent.putExtra("LOGGED_IN_USER", loggedInUser) // Añade el nombre de usuario al Intent
            startActivity(intent)
        }

        btnUpdateTonalidad.setOnClickListener {
            val intent = Intent(this, TonalidadesActivity::class.java)
            intent.putExtra("LOGGED_IN_USER", loggedInUser) // Añade el nombre de usuario al Intent
            startActivity(intent)
        }

        btnViewInspections.setOnClickListener {
            val intent = Intent(this, ContinuarCargaActivity::class.java)
            intent.putExtra("LOGGED_IN_USER", loggedInUser) // Añade el nombre de usuario al Intent
            startActivity(intent)
        }
    }
}
