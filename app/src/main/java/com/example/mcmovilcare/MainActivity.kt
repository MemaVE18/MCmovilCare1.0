package com.example.mcmovilcare

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.mcmovilcare.data.local.AppDatabase
import com.example.mcmovilcare.ui.theme.MCMovilCareTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var db: AppDatabase

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(
                    this,
                    "Las notificaciones están deshabilitadas",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("MisPreferenciasApp", Context.MODE_PRIVATE)
        if (sharedPreferences.getBoolean("isLoggedIn", false)) {
            val rolGuardado = sharedPreferences.getString("rol", "CLIENTE") ?: "CLIENTE"
            ejecutarRedireccion(rolGuardado)
            return
        }

        enableEdgeToEdge()

        verificarYPedirPermisoNotificaciones()

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "mcmovilcare_db"
        )
            .fallbackToDestructiveMigration()
            .build()

        // 🚀 Interfaz de usuario con Jetpack Compose y Material 3
        setContent {
            MCMovilCareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreenContent(
                        onLoginClick = { email -> procesarLogin(email) }
                    )
                }
            }
        }
    }

    @Composable
    private fun LoginScreenContent(onLoginClick: (String) -> Unit) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MCMovilCare",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo Electrónico") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Button(
                    onClick = {
                        if (email.trim().isEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Ingresa un correo válido",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            isLoading = true
                            onLoginClick(email.trim().lowercase())
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ingresar")
                }
            }
        }
    }

    private fun procesarLogin(email: String) {
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
                        Toast.makeText(
                            this@MainActivity,
                            "Usuario no encontrado",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Recargar Compose si no se encontró usuario
                        setContent {
                            MCMovilCareTheme {
                                Surface(modifier = Modifier.fillMaxSize()) {
                                    LoginScreenContent(onLoginClick = { procesarLogin(it) })
                                }
                            }
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
        val sharedPreferences = getSharedPreferences("MisPreferenciasApp", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putBoolean("isLoggedIn", true)
            putString("rol", rolNombre.uppercase())
            apply()
        }

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
                Toast.makeText(this, "Bienvenido Técnico", Toast.LENGTH_SHORT).show()
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
        finish()
    }
}