package com.example.mcmovilcare

import android.Manifest
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

    // Launcher para pedir el permiso de notificaciones en Android 13+
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
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Solicitar el permiso de notificaciones al iniciar la app
        verificarYPedirPermisoNotificaciones()

        // Referencias UI
        val etEmail = findViewById<EditText>(R.id.etCorreo)
        val etPassword = findViewById<EditText>(R.id.etContrasena)
        val btnLogin = findViewById<Button>(R.id.btnIngresar)
        val progressBarLogin = findViewById<ProgressBar>(R.id.progressBarLogin)

        // Inicializacion de Room Database
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "mcmovilcare_db"
        )
            .fallbackToDestructiveMigration()
            .build()

        // Evento de Login con animacion de carga y corrutina
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim().lowercase()

            if (email.isEmpty()) {
                Toast.makeText(this, "Ingresa un correo valido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Mostrar el loader y deshabilitar el boton mientras valida
            progressBarLogin.visibility = View.VISIBLE
            btnLogin.isEnabled = false

            lifecycleScope.launch(Dispatchers.IO) {
                val usuarioDao = db.usuarioDao()
                val usuario = usuarioDao.obtenerPorEmail(email)

                withContext(Dispatchers.Main) {
                    if (usuario != null) {
                        redireccionarPorRol(usuario.rol.toString())
                    } else {
                        // Fallback y simulacion para pruebas cuando la BD esta vacia
                        if (email.contains("tecnico")) {
                            redireccionarPorRol("TECNICO")
                        } else if (email.contains("admin")) {
                            redireccionarPorRol("ADMIN")
                        } else if (email.contains("cliente")) {
                            redireccionarPorRol("CLIENTE")
                        } else {
                            // Ocultar loader y rehabilitar boton si falla la validacion
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
        // Rehabilitar boton y ocultar loader antes de navegar
        findViewById<ProgressBar>(R.id.progressBarLogin).visibility = View.GONE
        findViewById<Button>(R.id.btnIngresar).isEnabled = true

        val intent = when (rolNombre.uppercase()) {
            "TECNICO", "ADMIN" -> {
                Toast.makeText(this, "Bienvenido $rolNombre", Toast.LENGTH_SHORT).show()
                Intent(this, HistorialActivity::class.java).apply {
                    putExtra("EXTRA_ROL", rolNombre.uppercase())
                }
            }
            "CLIENTE" -> {
                Toast.makeText(this, "Bienvenido Cliente", Toast.LENGTH_SHORT).show()
                Intent(this, DashboardClienteActivity::class.java)
            }
            else -> Intent(this, DashboardClienteActivity::class.java)
        }
        startActivity(intent)

        // Aplicar la transicion suave entre pantallas
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}