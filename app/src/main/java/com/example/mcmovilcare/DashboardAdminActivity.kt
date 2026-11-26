package com.example.mcmovilcare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.room.Room
import com.example.mcmovilcare.data.local.AppDatabase
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.model.Rol
import com.example.mcmovilcare.model.Usuario
import com.example.mcmovilcare.repository.AppRepository
import com.example.mcmovilcare.ui.components.CyberBackground
import com.example.mcmovilcare.ui.theme.DigitalGhostFont
import com.example.mcmovilcare.ui.theme.MCMovilCareTheme
import com.example.mcmovilcare.viewmodel.OrdenTrabajoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

class DashboardAdminActivity : ComponentActivity() {

    private lateinit var repository: AppRepository
    private lateinit var viewModel: OrdenTrabajoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "mcmovilcare_db"
        )
            .enableMultiInstanceInvalidation()
            .fallbackToDestructiveMigration()
            .build()

        repository = AppRepository(
            database.usuarioDao(),
            database.equipoDao(),
            database.ordenTrabajoDao()
        )

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OrdenTrabajoViewModel(repository) as T
            }

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return OrdenTrabajoViewModel(repository) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[OrdenTrabajoViewModel::class.java]

        setContent {
            MCMovilCareTheme(darkTheme = true) {
                val listaOrdenes by repository.obtenerOrdenes.collectAsState(initial = emptyList())
                val listaUsuarios by repository.obtenerUsuarios.collectAsState(initial = emptyList())

                DashboardAdminScreenContent(
                    ordenes = listaOrdenes,
                    usuarios = listaUsuarios,
                    onGuardarUsuario = { nombre, correo, rolNombre ->
                        guardarUsuario(nombre, correo, rolNombre)
                    },
                    onCerrarSesion = { cerrarSesion() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.cargarEquiposDesdeApi()
        }
    }

    private fun guardarUsuario(nombre: String, correo: String, rolNombre: String) {
        if (nombre.isBlank() || correo.isBlank()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val rolEnum = try {
            Rol.valueOf(rolNombre)
        } catch (e: Exception) {
            Rol.TECNICO
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val nuevoUsuario = Usuario(
                id = UUID.randomUUID().toString(),
                nombre = nombre,
                correo = correo,
                rol = rolEnum
            )
            repository.guardarUsuario(nuevoUsuario)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DashboardAdminActivity, "Usuario guardado con exito", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cerrarSesion() {
        val sharedPreferences = getSharedPreferences("MisPreferenciasApp", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardAdminScreenContent(
    ordenes: List<OrdenTrabajo>,
    usuarios: List<Usuario>,
    onGuardarUsuario: (String, String, String) -> Unit,
    onCerrarSesion: () -> Unit
) {
    var nombreInput by remember { mutableStateOf("") }
    var correoInput by remember { mutableStateOf("") }
    var rolSeleccionado by remember { mutableStateOf("TECNICO") }

    val roles = listOf("CLIENTE", "TECNICO", "ADMIN")

    CyberBackground(isDashboard = true) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Panel Administrador",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = DigitalGhostFont,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Gestion global del sistema",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = DigitalGhostFont),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = onCerrarSesion) {
                            Text(
                                "Cerrar Sesion",
                                fontFamily = DigitalGhostFont,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Registrar Perfil de Usuario",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = DigitalGhostFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = nombreInput,
                                onValueChange = { nombreInput = it },
                                label = { Text("Nombre", fontFamily = DigitalGhostFont) },
                                textStyle = TextStyle(fontFamily = DigitalGhostFont, fontSize = 14.sp),
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = correoInput,
                                onValueChange = { correoInput = it },
                                label = { Text("Correo (ej: tecnico2@duoc.cl)", fontFamily = DigitalGhostFont) },
                                textStyle = TextStyle(fontFamily = DigitalGhostFont, fontSize = 14.sp),
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            Text(
                                text = "Seleccionar Rol:",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = DigitalGhostFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                roles.forEach { rol ->
                                    FilterChip(
                                        selected = rolSeleccionado == rol,
                                        onClick = { rolSeleccionado = rol },
                                        label = {
                                            Text(
                                                rol,
                                                fontFamily = DigitalGhostFont,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    onGuardarUsuario(nombreInput, correoInput, rolSeleccionado)
                                    nombreInput = ""
                                    correoInput = ""
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    "Guardar Usuario en BD",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = DigitalGhostFont,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Catalogo Activo (Servicios y Repuestos)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = DigitalGhostFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )

                            ItemCatalogo(titulo = "Mantencion preventiva general", precio = 25000.0)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            ItemCatalogo(titulo = "Cambio de bateria certificada", precio = 45000.0)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            ItemCatalogo(titulo = "Reemplazo modulo pantalla", precio = 150000.0)
                        }
                    }
                }

                item {
                    Text(
                        text = "Perfiles en Base de Datos (${usuarios.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DigitalGhostFont,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                item {
                    Text(
                        text = "Supervision de Ordenes (${ordenes.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DigitalGhostFont,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                items(ordenes) { orden ->
                    TarjetaOrdenAdminItem(orden = orden)
                }
            }
        }
    }
}

@Composable
fun ItemCatalogo(titulo: String, precio: Double) {
    val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "CL"))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = DigitalGhostFont),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = formatoMoneda.format(precio),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = DigitalGhostFont,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun TarjetaOrdenAdminItem(orden: OrdenTrabajo) {
    val (badgeBg, badgeFg) = when (orden.estado) {
        "En diagnostico" -> Color(0xFF332200) to Color(0xFFFFB74D)
        "Pend. autorizacion" -> Color(0xFF3B122B) to Color(0xFFFF4081)
        "Aprobado", "Listo" -> Color(0xFF00332C) to Color(0xFF00E5FF)
        "Rechazado" -> Color(0xFF3B1212) to Color(0xFFFF5252)
        else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Orden #${orden.id} • ${orden.equipoModelo}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DigitalGhostFont,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "S/N: ${orden.numeroSerie}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = DigitalGhostFont),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = orden.estado,
                    color = badgeFg,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = DigitalGhostFont,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}