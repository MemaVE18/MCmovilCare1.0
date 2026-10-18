package com.example.mcmovilcare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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

        // Referencias UI de tu XML
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

        // Evento de Login
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                val intent = Intent(this@MainActivity, DashboardClienteActivity::class.java)
                startActivity(intent)
                finish()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val usuarioDao = db.usuarioDao()
                val usuario = usuarioDao.obtenerPorEmail(email)

                withContext(Dispatchers.Main) {
                    if (usuario != null) {
                        // Pasamos el Enum convertido a String mediante .name
                        redireccionarPorRol(usuario.rol.name)
                    } else {
                        val intent = Intent(this@MainActivity, DashboardClienteActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
    }

    private fun redireccionarPorRol(rolNombre: String) {
        val intent = when (rolNombre.uppercase()) {
            "CLIENTE", "TECNICO", "ADMIN" -> Intent(this, DashboardClienteActivity::class.java)
            else -> Intent(this, DashboardClienteActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}