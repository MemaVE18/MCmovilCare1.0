package com.example.mcmovilcare

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(
                    this,
                    "Las notificaciones estan deshabilitadas",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si ya hay una sesión activa antes de cargar la vista
        val sharedPreferences = getSharedPreferences("MisPreferenciasApp", Context.MODE_PRIVATE)
        if (sharedPreferences.getBoolean("isLoggedIn", false)) {
            val rolGuardado = sharedPreferences.getString("rol", "CLIENTE") ?: "CLIENTE"
            ejecutarRedireccion(rolGuardado)
            return // Salimos del onCreate para no cargar la UI del login
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        verificarYPedirPermisoNotificaciones()

        val etEmail = findViewById<EditText>(R.id.etCorreo)
        val etPassword = findViewById<EditText>(R.id.etContrasena) // Faltaba validar contraseña, pero lo dejamos como tu base
        val btnLogin = findViewById<Button>(R.id.btnIngresar)
        val progressBarLogin = findViewById<ProgressBar>(R.id.progressBarLogin)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "mcmovilcare_db"
        )
            .fallbackToDestructiveMigration()
            .build()

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim().lowercase()

            if (email.isEmpty()) {
                Toast.makeText(this, "Ingresa un correo valido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBarLogin.visibility = View.VISIBLE
            btnLogin.isEnabled = false

            lifecycleScope.launch(Dispatchers.IO) {
                val usuarioDao = db.usuarioDao()
                val usuario = usuarioDao.obtenerPorEmail(email)

                withContext(Dispatchers.Main) {
                    if (usuario != null) {
                        redireccionarPorRol(usuario.rol.toString())
                    } else {
                        if (email.contains("tecnico")) {
                            redireccionarPorRol("TECNICO")
                        } else if (email.contains("admin")) {
                            redireccionarPorRol("ADMIN")
                        } else if (email.contains("cliente")) {
                            redireccionarPorRol("CLIENTE")
                        } else {
                            progressBarLogin.visibility = View.GONE
                            btnLogin.isEnabled = true
                            Toast.makeText(
                                this@MainActivity,
                                "Usuario no encontrado",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun verificarYPedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun redireccionarPorRol(rolNombre: String) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBarLogin)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)

        if (progressBar != null) progressBar.visibility = View.GONE
        if (btnIngresar != null) btnIngresar.isEnabled = true

        // 1. Guardar la sesión localmente
        val sharedPreferences = getSharedPreferences("MisPreferenciasApp", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putBoolean("isLoggedIn", true)
            putString("rol", rolNombre.uppercase())
            apply()
        }

        // 2. Ejecutar la navegación
        ejecutarRedireccion(rolNombre.uppercase())
    }

    private fun ejecutarRedireccion(rolNombre: String) {
        val intent = when (rolNombre) {
            "ADMIN" -> {
                Toast.makeText(this, "Bienvenido Administrador", Toast.LENGTH_SHORT).show()
                Intent(this, HistorialActivity::class.java).apply {
                    putExtra("EXTRA_ROL", "ADMIN")
                }
            }
            "TECNICO" -> {
                Toast.makeText(this, "Bienvenido Tecnico", Toast.LENGTH_SHORT).show()
                Intent(this, HistorialActivity::class.java).apply {
                    putExtra("EXTRA_ROL", rolNombre)
                }
            }
            "CLIENTE" -> {
                Toast.makeText(this, "Bienvenido Cliente", Toast.LENGTH_SHORT).show()
                Intent(this, DashboardClienteActivity::class.java)
            }
            else -> {
                Intent(this, DashboardClienteActivity::class.java)
            }
        }

        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish() // Cierra el login para que el botón atrás no regrese aquí
    }
}