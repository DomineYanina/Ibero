package com.example.ibero

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var textWelcome: TextView
    private lateinit var btnNewRecord: Button
    private lateinit var loggedInUser: String
    private lateinit var btnAgregarTonalidades: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        textWelcome = findViewById(R.id.text_welcome)
        btnNewRecord = findViewById(R.id.btn_new_record)
        btnAgregarTonalidades = findViewById(R.id.btn_agregar_tonalidades)

        loggedInUser = intent.getStringExtra("LOGGED_IN_USER") ?: "Usuario"
        textWelcome.text = "Bienvenido, $loggedInUser"

        btnNewRecord.setOnClickListener {
            val intent = Intent(this, PrimerRegistroActivity::class.java)
            intent.putExtra("LOGGED_IN_USER", loggedInUser)
            startActivity(intent)
        }

        btnAgregarTonalidades.setOnClickListener {
            val intent = Intent(this, TonalidadesActivity::class.java)
            // Opcional: pasar el nombre de usuario
            intent.putExtra("LOGGED_IN_USER", loggedInUser)
            startActivity(intent)
        }
    }
}
