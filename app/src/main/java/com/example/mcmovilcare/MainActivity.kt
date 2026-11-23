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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.mcmovilcare.data.local.AppDatabase
import com.example.mcmovilcare.model.Rol
import com.example.mcmovilcare.model.Usuario
import com.example.mcmovilcare.ui.theme.MCMovilCareTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

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

        mostrarPantallaLogin()
    }

    private fun mostrarPantallaLogin() {
        setContent {
            MCMovilCareTheme {
                val isDark = isSystemInDarkTheme()
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isDark) {
                        Image(
                            painter = painterResource(id = R.drawable.bg_dandadan),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.62f))
                        )
                    }
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (isDark) Color.Transparent else MaterialTheme.colorScheme.background
                    ) {
                        LoginScreenContent(
                            onLoginClick = { email, pass -> procesarLogin(email, pass) },
                            onGoogleLoginClick = { iniciarLoginConGoogle() }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun LoginScreenContent(
        onLoginClick: (String, String) -> Unit,
        onGoogleLoginClick: () -> Unit
    ) {
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
                        if (email.trim().isEmpty() || password.isEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Ingresa un correo válido",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            isLoading = true
                            onLoginClick(email.trim().lowercase(), password)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ingresar")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onGoogleLoginClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continuar con Google")
                }
            }
        }
    }

    private fun iniciarLoginConGoogle() {
        val webClientId = "369502010508-34n1qiagmda3k3naee4fcfg3733butbr.apps.googleusercontent.com"
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(this)
        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@MainActivity
                )
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                    val emailGoogle = googleCred.id.trim().lowercase()
                    val nombreGoogle = googleCred.displayName ?: emailGoogle.substringBefore("@")
                    procesarLoginGoogle(emailGoogle, nombreGoogle)
                }
            } catch (e: GetCredentialException) {
                Toast.makeText(
                    this@MainActivity,
                    "No se pudo iniciar con Google: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun procesarLoginGoogle(emailGoogle: String, nombreGoogle: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val usuarioDao = db.usuarioDao()
            try {
                val usuariosApi = RetrofitClient.apiService.obtenerUsuarios()
                for (u in usuariosApi) {
                    usuarioDao.insertarUsuario(u.copy(correo = u.correo.trim().lowercase()))
                }
            } catch (_: Exception) { }

            val existente = usuarioDao.obtenerPorEmail(emailGoogle)
            val usuarioFinal: Usuario = if (existente != null) {
                existente
            } else {
                val nuevoUsuario = Usuario(
                    id = UUID.randomUUID().toString().take(6),
                    nombre = nombreGoogle,
                    correo = emailGoogle,
                    rol = Rol.CLIENTE,
                    password = UUID.randomUUID().toString()
                )
                val creado = try {
                    RetrofitClient.apiService.crearUsuario(nuevoUsuario)
                } catch (_: Exception) {
                    nuevoUsuario
                }
                usuarioDao.insertarUsuario(creado)
                creado
            }

            withContext(Dispatchers.Main) {
                redireccionarPorRol(usuarioFinal.rol.toString())
            }
        }
    }

    private fun procesarLogin(email: String, pass: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val usuarioDao = db.usuarioDao()

            try {
                val usuariosApi = RetrofitClient.apiService.obtenerUsuarios()
                for (u in usuariosApi) {
                    usuarioDao.insertarUsuario(u.copy(correo = u.correo.trim().lowercase()))
                }
            } catch (_: Exception) {
                // Si falla la red, valida contra lo que ya esté guardado en Room
            }

            val usuario = usuarioDao.obtenerPorEmail(email)

            withContext(Dispatchers.Main) {
                if (usuario != null && usuario.password == pass) {
                    redireccionarPorRol(usuario.rol.toString())
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Correo o contraseña incorrectos",
                        Toast.LENGTH_SHORT
                    ).show()
                    mostrarPantallaLogin()
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