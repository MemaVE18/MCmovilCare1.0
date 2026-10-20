package com.example.mcmovilcare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.mcmovilcare.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Referencias UI
        val etEmail = findViewById<EditText>(R.id.etCorreo)
        val etPassword = findViewById<EditText>(R.id.etContrasena)
        val btnLogin = findViewById<Button>(R.id.btnIngresar)

        // Inicialización de Room Database
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "mcmovilcare_db"
        )
            .fallbackToDestructiveMigration()
            .build()

        // Evento de Login con simulación integrada
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim().lowercase()

            if (email.isEmpty()) {
                Toast.makeText(this, "Ingresa un correo válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val usuarioDao = db.usuarioDao()
                val usuario = usuarioDao.obtenerPorEmail(email)

                withContext(Dispatchers.Main) {
                    if (usuario != null) {
                        redireccionarPorRol(usuario.rol.toString())
                    } else {
                        // Fallback/Simulación para pruebas cuando la BD está vacía
                        if (email.contains("tecnico")) {
                            redireccionarPorRol("TECNICO")
                        } else if (email.contains("cliente")) {
                            redireccionarPorRol("CLIENTE")
                        } else {
                            Toast.makeText(this@MainActivity, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun redireccionarPorRol(rolNombre: String) {
        val intent = when (rolNombre.uppercase()) {
            "TECNICO", "ADMIN" -> {
                Toast.makeText(this, "Bienvenido Técnico", Toast.LENGTH_SHORT).show()
                Intent(this, NuevoEquipoActivity::class.java)
            }
            "CLIENTE" -> {
                Toast.makeText(this, "Bienvenido Cliente", Toast.LENGTH_SHORT).show()
                Intent(this, DashboardClienteActivity::class.java)
            }
            else -> Intent(this, DashboardClienteActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}