package com.example.mcmovilcare

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.mcmovilcare.data.local.AppDatabase
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.model.Rol
import com.example.mcmovilcare.model.Usuario
import com.example.mcmovilcare.util.NotificacionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class HistorialActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rolActual = intent.getStringExtra("EXTRA_ROL") ?: "TECNICO"

        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "mcmovilcare_db"
        ).fallbackToDestructiveMigration().build()

        // Sembrar datos iniciales si aun no existen
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

        setContent {
            MaterialTheme {
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
                                    Toast.makeText(this@HistorialActivity, "Usuario guardado", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onCerrarSesion = { finish() }
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
                        onCerrarSesion = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun PanelTecnicoScreen(
    ordenes: List<OrdenTrabajo>,
    onActualizarOrden: (String, String, String, Double?, Boolean, String) -> Unit,
    onAbrirEscaner: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    var ordenSeleccionada by remember { mutableStateOf<OrdenTrabajo?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Panel Tecnico - Ordenes", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onCerrarSesion) {
                Text("Salir", color = MaterialTheme.colorScheme.error)
            }
        }

        Button(
            onClick = onAbrirEscaner,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Ingresar Equipo / Escanear QR")
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(ordenes) { orden ->
                Card(
                    onClick = { ordenSeleccionada = orden },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${orden.equipoModelo} (S/N: ${orden.numeroSerie})", style = MaterialTheme.typography.titleMedium)
                        Text("Estado actual: ${orden.estado}", color = MaterialTheme.colorScheme.primary)
                        Text("Diagnostico: ${orden.diagnostico ?: "Pendiente"}")
                        Text("Evidencia: ${orden.evidencia ?: "Sin registro"}")
                        if (orden.cotizacionMonto != null) {
                            Text("Cotizacion: $${orden.cotizacionMonto}")
                        }
                        Text("Toca para actualizar estado y evidencia", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    // Dialogo para que el tecnico edite la orden
    ordenSeleccionada?.let { orden ->
        var diag by remember { mutableStateOf(orden.diagnostico ?: "") }
        var montoTexto by remember { mutableStateOf(orden.cotizacionMonto?.toString() ?: "") }
        var evidencia by remember { mutableStateOf(orden.evidencia ?: "Foto frontal y test de hardware OK") }
        var estadoElegido by remember { mutableStateOf(orden.estado) }

        AlertDialog(
            onDismissRequest = { ordenSeleccionada = null },
            title = { Text("Gestionar ${orden.equipoModelo}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = diag,
                        onValueChange = { diag = it },
                        label = { Text("Diagnostico tecnico") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = montoTexto,
                        onValueChange = { montoTexto = it },
                        label = { Text("Monto cotizacion ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = evidencia,
                        onValueChange = { evidencia = it },
                        label = { Text("Evidencia / Nota fotografica") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Cambiar Estado:", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { estadoElegido = "En diagnostico" }, modifier = Modifier.weight(1f)) {
                            Text("Diag.", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(onClick = { estadoElegido = "Pend. autorizacion" }, modifier = Modifier.weight(1f)) {
                            Text("Cotizar", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(onClick = { estadoElegido = "Listo" }, modifier = Modifier.weight(1f)) {
                            Text("Listo", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text("Estado seleccionado: $estadoElegido", color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val montoNum = montoTexto.toDoubleOrNull()
                    val requiereAut = estadoElegido == "Pend. autorizacion"
                    onActualizarOrden(orden.id, estadoElegido, diag, montoNum, requiereAut, evidencia)
                    ordenSeleccionada = null
                }) {
                    Text("Guardar")
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Panel Administrador", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onCerrarSesion) {
                    Text("Salir", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // 1. Crear perfiles
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Registrar Perfil de Usuario", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = correo,
                        onValueChange = { correo = it },
                        label = { Text("Correo (ej: tecnico2@duoc.cl)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Rol.values().forEach { rol ->
                            FilterChip(
                                selected = (rolSeleccionado == rol),
                                onClick = { rolSeleccionado = rol },
                                label = { Text(rol.name) }
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar Usuario en BD")
                    }
                }
            }
        }

        // 2. Catalogo de servicios y repuestos
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Catalogo Activo (Servicios y Repuestos)", style = MaterialTheme.typography.titleMedium)
                    Text("- Mantencion preventiva general: $25.000")
                    Text("- Cambio de bateria certificada: $45.000")
                    Text("- Reemplazo modulo pantalla: $150.000")
                }
            }
        }

        // 3. Usuarios registrados
        item {
            Text("Perfiles en Base de Datos (${usuarios.size})", style = MaterialTheme.typography.titleMedium)
        }
        items(usuarios) { u ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${u.nombre} - ${u.rol}", style = MaterialTheme.typography.bodyLarge)
                    Text(u.correo, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // 4. Supervision de ordenes
        item {
            Text("Supervision de Ordenes (${ordenes.size})", style = MaterialTheme.typography.titleMedium)
        }
        items(ordenes) { o ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Orden #${o.id} - ${o.equipoModelo}", style = MaterialTheme.typography.titleSmall)
                    Text("Estado: ${o.estado} | S/N: ${o.numeroSerie}")
                }
            }
        }
    }
}