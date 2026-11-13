package com.example.mcmovilcare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.mcmovilcare.data.local.AppDatabase
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.model.Rol
import com.example.mcmovilcare.model.Usuario
import com.example.mcmovilcare.ui.theme.MCMovilCareTheme
import com.example.mcmovilcare.util.NotificacionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

class HistorialActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val rolActual = intent.getStringExtra("EXTRA_ROL") ?: "TECNICO"

        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "mcmovilcare_db"
        )
            .enableMultiInstanceInvalidation()
            .fallbackToDestructiveMigration()
            .build()

        lifecycleScope.launch(Dispatchers.IO) {
            val ordenesActuales = database.ordenTrabajoDao().obtenerOrdenes().first()
            if (ordenesActuales.isEmpty()) {
                val mockOrdenes = listOf(
                    OrdenTrabajo("1", "Samsung Galaxy S22", "SN123", "En diagnostico", null, null, false, "10-10-2026"),
                    OrdenTrabajo("2", "MacBook Pro M1", "SN456", "Pend. autorizacion", "Pantalla rota", 150000.0, true, "12-10-2026"),
                    OrdenTrabajo("3", "iPhone 13", "SN789", "Listo", "Cambio de bateria", 45000.0, false, "15-10-2026")
                )
                mockOrdenes.forEach { database.ordenTrabajoDao().insertarOrden(it) }
            }
        }

        val ejecutarCierreSesion: () -> Unit = {
            val sharedPreferences = getSharedPreferences("MisPreferenciasApp", Context.MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()

            val intent = Intent(this@HistorialActivity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        setContent {
            MCMovilCareTheme {
                val listaOrdenes by database.ordenTrabajoDao().obtenerOrdenes().collectAsState(initial = emptyList())
                val listaUsuarios by database.usuarioDao().obtenerTodosLosUsuarios().collectAsState(initial = emptyList())

                if (rolActual == "ADMIN") {
                    PanelAdminScreen(
                        ordenes = listaOrdenes,
                        usuarios = listaUsuarios,
                        onCrearUsuario = { nombre, correo, rol ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                val nuevo = Usuario(
                                    id = UUID.randomUUID().toString().take(5),
                                    nombre = nombre,
                                    correo = correo.lowercase().trim(),
                                    rol = rol
                                )
                                database.usuarioDao().insertarUsuario(nuevo)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@HistorialActivity, "Usuario guardado con éxito", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onCerrarSesion = ejecutarCierreSesion
                    )
                } else {
                    PanelTecnicoScreen(
                        ordenes = listaOrdenes,
                        onActualizarOrden = { ordenId, estado, diag, monto, requiereAut, evidencia ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                database.ordenTrabajoDao().actualizarOrdenTecnico(
                                    ordenId = ordenId,
                                    nuevoEstado = estado,
                                    nuevoDiagnostico = diag,
                                    nuevoMonto = monto,
                                    requiereAut = requiereAut,
                                    nuevaEvidencia = evidencia
                                )
                                withContext(Dispatchers.Main) {
                                    NotificacionHelper.enviarNotificacion(
                                        this@HistorialActivity,
                                        "Orden #$ordenId Actualizada",
                                        "Nuevo estado: $estado"
                                    )
                                    Toast.makeText(this@HistorialActivity, "Orden #$ordenId actualizada", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onAbrirEscaner = {
                            startActivity(Intent(this@HistorialActivity, NuevoEquipoActivity::class.java))
                        },
                        onCerrarSesion = ejecutarCierreSesion
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelTecnicoScreen(
    ordenes: List<OrdenTrabajo>,
    onActualizarOrden: (String, String, String, Double?, Boolean, String) -> Unit,
    onAbrirEscaner: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    var ordenSeleccionada by remember { mutableStateOf<OrdenTrabajo?>(null) }
    val formatoMoneda = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Panel Técnico",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Gestión e inspección de órdenes",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onCerrarSesion) {
                        Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                Button(
                    onClick = onAbrirEscaner,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Ingresar Equipo / Escanear QR",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            item {
                Text(
                    text = "Órdenes de Trabajo (${ordenes.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(ordenes) { orden ->
                val (badgeBg, badgeFg) = when (orden.estado) {
                    "En diagnostico" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
                    "Pend. autorizacion", "Aprobado" -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
                    "Listo" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                    else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                }

                Card(
                    onClick = { ordenSeleccionada = orden },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                        text = orden.equipoModelo,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "S/N: ${orden.numeroSerie}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
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
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF0F0F0))

                        Text(
                            text = "Diagnóstico: ${orden.diagnostico ?: "Sin registro aún"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray
                        )

                        Text(
                            text = "Evidencia: ${orden.evidencia ?: "Sin evidencia registrada"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        if (orden.cotizacionMonto != null) {
                            Text(
                                text = "Cotización: ${formatoMoneda.format(orden.cotizacionMonto)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "Toca para actualizar diagnóstico y evidencia",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    ordenSeleccionada?.let { orden ->
        var diag by remember { mutableStateOf(orden.diagnostico ?: "") }
        var montoTexto by remember { mutableStateOf(orden.cotizacionMonto?.let { String.format(Locale.US, "%.0f", it) } ?: "") }
        var evidencia by remember { mutableStateOf(orden.evidencia ?: "Foto frontal y test de hardware OK") }
        var estadoElegido by remember { mutableStateOf(orden.estado) }

        AlertDialog(
            onDismissRequest = { ordenSeleccionada = null },
            title = {
                Text(
                    "Gestionar ${orden.equipoModelo}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = diag,
                        onValueChange = { diag = it },
                        label = { Text("Diagnóstico técnico") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = montoTexto,
                        onValueChange = { montoTexto = it },
                        label = { Text("Monto cotización ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = evidencia,
                        onValueChange = { evidencia = it },
                        label = { Text("Evidencia / Nota fotográfica") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text("Cambiar Estado:", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = estadoElegido == "En diagnostico",
                            onClick = { estadoElegido = "En diagnostico" },
                            label = { Text("Diag.", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = estadoElegido == "Pend. autorizacion",
                            onClick = { estadoElegido = "Pend. autorizacion" },
                            label = { Text("Cotizar", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = estadoElegido == "Listo",
                            onClick = { estadoElegido = "Listo" },
                            label = { Text("Listo", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val montoNum = montoTexto.toDoubleOrNull()
                        val requiereAut = estadoElegido == "Pend. autorizacion"
                        onActualizarOrden(orden.id, estadoElegido, diag, montoNum, requiereAut, evidencia)
                        ordenSeleccionada = null
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Guardar Cambios")
                }
            },
            dismissButton = {
                TextButton(onClick = { ordenSeleccionada = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelAdminScreen(
    ordenes: List<OrdenTrabajo>,
    usuarios: List<Usuario>,
    onCrearUsuario: (String, String, Rol) -> Unit,
    onCerrarSesion: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var rolSeleccionado by remember { mutableStateOf(Rol.TECNICO) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Panel Administrador",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Gestión global del sistema",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onCerrarSesion) {
                        Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Registrar Perfil de Usuario",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = nombre,
                            onValueChange = { nombre = it },
                            label = { Text("Nombre") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = correo,
                            onValueChange = { correo = it },
                            label = { Text("Correo (ej: tecnico2@duoc.cl)") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Text(
                            text = "Seleccionar Rol:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.Gray
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Rol.values().forEach { rol ->
                                FilterChip(
                                    selected = (rolSeleccionado == rol),
                                    onClick = { rolSeleccionado = rol },
                                    label = { Text(rol.name, fontWeight = FontWeight.SemiBold) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (nombre.isNotBlank() && correo.isNotBlank()) {
                                    onCrearUsuario(nombre, correo, rolSeleccionado)
                                    nombre = ""
                                    correo = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Guardar Usuario en BD", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Catálogo Activo (Servicios y Repuestos)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        ItemCatalogoHistorial(titulo = "Mantención preventiva general", precio = 25000.0)
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        ItemCatalogoHistorial(titulo = "Cambio de batería certificada", precio = 45000.0)
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        ItemCatalogoHistorial(titulo = "Reemplazo módulo pantalla", precio = 150000.0)
                    }
                }
            }

            item {
                Text(
                    text = "Perfiles en Base de Datos (${usuarios.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            items(usuarios) { u ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(u.nombre, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(u.correo, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = u.rol.name,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Supervisión de Órdenes (${ordenes.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            items(ordenes) { o ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Orden #${o.id} • ${o.equipoModelo}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("S/N: ${o.numeroSerie}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = o.estado,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemCatalogoHistorial(titulo: String, precio: Double) {
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
            Text(text = titulo, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
        }
        Text(
            text = formatoMoneda.format(precio),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}