package com.example.mcmovilcare.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.room.Room
import com.example.mcmovilcare.data.local.AppDatabase
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.network.DiagnosticoRagRequest
import com.example.mcmovilcare.network.DiagnosticoRagResponse
import com.example.mcmovilcare.network.NuevoCasoRagRequest
import com.example.mcmovilcare.network.RagRetrofitClient
import com.example.mcmovilcare.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TecnicoDashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val repository = remember {
        val db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "mcmovilcare_db"
        )
            .enableMultiInstanceInvalidation()
            .fallbackToDestructiveMigration()
            .build()
        AppRepository(db.usuarioDao(), db.equipoDao(), db.ordenTrabajoDao())
    }

    val ordenes by repository.obtenerOrdenes.collectAsState(initial = emptyList())

    var ordenSeleccionadaRag by remember { mutableStateOf<OrdenTrabajo?>(null) }
    var cargandoRag by remember { mutableStateOf(false) }
    var respuestaRag by remember { mutableStateOf<DiagnosticoRagResponse?>(null) }
    var errorRag by remember { mutableStateOf<String?>(null) }

    var ordenEditarEstado by remember { mutableStateOf<OrdenTrabajo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mis Trabajos Asignados", fontWeight = FontWeight.Bold)
                        Text(
                            "Taller Técnico • Asistente IA RAG",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (ordenes.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay órdenes registradas aún.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(ordenes) { orden ->
                    TarjetaOrdenTecnico(
                        orden = orden,
                        onConsultarRag = {
                            ordenSeleccionadaRag = orden
                            respuestaRag = null
                            errorRag = null
                            cargandoRag = true

                            val (categoria, marca, modelo) = parsearEquipo(orden.equipoModelo)
                            val sintomaConsulta = orden.diagnostico?.takeIf { it.isNotBlank() }
                                ?: "Revision tecnica general y falla reportada por cliente"

                            scope.launch(Dispatchers.IO) {
                                try {
                                    val res = RagRetrofitClient.api.obtenerDiagnostico(
                                        DiagnosticoRagRequest(
                                            categoria = categoria,
                                            marca = marca,
                                            modelo = modelo,
                                            ean = null,
                                            sintoma = sintomaConsulta
                                        )
                                    )
                                    withContext(Dispatchers.Main) {
                                        respuestaRag = res
                                        cargandoRag = false
                                    }
                                } catch (e: HttpException) {
                                    val detalle = try {
                                        e.response()?.errorBody()?.string()?.take(200)
                                    } catch (_: Exception) {
                                        null
                                    }
                                    withContext(Dispatchers.Main) {
                                        errorRag = "HTTP ${e.code()}: ${detalle ?: e.message()}"
                                        cargandoRag = false
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        errorRag = e.localizedMessage ?: "Error conectando al servidor RAG"
                                        cargandoRag = false
                                    }
                                }
                            }
                        },
                        onActualizarEstado = {
                            ordenEditarEstado = orden
                        }
                    )
                }
            }
        }
    }

    // Diálogo 1: Resultado del Asistente Técnico IA (RAG)
    ordenSeleccionadaRag?.let { orden ->
        AlertDialog(
            onDismissRequest = { ordenSeleccionadaRag = null },
            title = {
                Text("Asistente IA • Orden #${orden.id}", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Equipo: ${orden.equipoModelo}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (cargandoRag) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text("Consultando historial técnico en pgvector...")
                        }
                    }

                    errorRag?.let { err ->
                        Text("Error: $err", color = MaterialTheme.colorScheme.error)
                    }

                    respuestaRag?.let { rag ->
                        Text(
                            text = "Modo: ${rag.modoSeguro}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        rag.advertenciaDevolucion?.takeIf { it.isNotBlank() }?.let { alerta ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = "⚠ Alerta historial: $alerta",
                                    modifier = Modifier.padding(10.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        if (rag.causasSeguras.isNotEmpty()) {
                            Text("Causas probables:", fontWeight = FontWeight.Bold)
                            rag.causasSeguras.forEach {
                                Text("• $it", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (rag.pasosSeguros.isNotEmpty()) {
                            Text("Pasos de revisión en mesón:", fontWeight = FontWeight.Bold)
                            rag.pasosSeguros.forEachIndexed { idx, paso ->
                                Text("${idx + 1}. $paso", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (rag.solucionesSeguras.isNotEmpty()) {
                            Text("Soluciones aplicadas en taller:", fontWeight = FontWeight.Bold)
                            rag.solucionesSeguras.forEach {
                                Text("✓ $it", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (rag.skusSeguros.isNotEmpty()) {
                            Text(
                                text = "SKUs sugeridos: ${rag.skusSeguros.joinToString(", ")}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { ordenSeleccionadaRag = null }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Diálogo 2: Cambiar Estado / Cerrar o Devolver Orden + Indexar en RAG
    ordenEditarEstado?.let { orden ->
        DialogoActualizarOrdenTecnico(
            orden = orden,
            onDismiss = { ordenEditarEstado = null },
            onGuardar = { nuevoEstado, diagTexto, solucionTexto, skusTexto ->
                scope.launch(Dispatchers.IO) {
                    val ordenActualizada = orden.copy(
                        estado = nuevoEstado,
                        diagnostico = diagTexto.ifBlank { orden.diagnostico }
                    )
                    repository.guardarOrden(ordenActualizada)

                    val esCierre = nuevoEstado.equals("Listo", ignoreCase = true) ||
                            nuevoEstado.equals("Devuelto sin reparación", ignoreCase = true)

                    if (esCierre) {
                        val (categoria, marca, modelo) = parsearEquipo(orden.equipoModelo)
                        val listaSkus = skusTexto.split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }

                        try {
                            RagRetrofitClient.api.registrarCasoCerrado(
                                NuevoCasoRagRequest(
                                    ordenCodigo = "OT-${orden.id}",
                                    categoria = categoria,
                                    marca = marca,
                                    modelo = modelo,
                                    ean = null,
                                    sintoma = orden.diagnostico ?: "Falla reportada en ingreso",
                                    diagnostico = diagTexto.ifBlank { "Diagnostico tecnico en taller" },
                                    solucionAplicada = solucionTexto.ifBlank {
                                        if (nuevoEstado == "Listo") "Reparacion completada en taller"
                                        else "Devuelto sin reparacion tras evaluacion tecnica"
                                    },
                                    estadoFinal = if (nuevoEstado == "Listo") "REPARADO" else "DEVUELTO_SIN_REPARACION",
                                    repuestosSku = listaSkus
                                )
                            )
                        } catch (_: Exception) {
                            // No bloquea el cierre local si no hay red
                        }
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            if (esCierre) "Orden actualizada e indexada en RAG" else "Orden actualizada",
                            Toast.LENGTH_SHORT
                        ).show()
                        ordenEditarEstado = null
                    }
                }
            }
        )
    }
}

@Composable
private fun TarjetaOrdenTecnico(
    orden: OrdenTrabajo,
    onConsultarRag: () -> Unit,
    onActualizarEstado: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Orden #${orden.id} • ${orden.equipoModelo}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Text("S/N: ${orden.numeroSerie}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("Estado actual: ${orden.estado}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Falla / Diagnóstico: ${orden.diagnostico ?: "Pendiente de revisión"}",
                style = MaterialTheme.typography.bodySmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConsultarRag,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Asistente IA (RAG)", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onActualizarEstado,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Gestionar Orden", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun DialogoActualizarOrdenTecnico(
    orden: OrdenTrabajo,
    onDismiss: () -> Unit,
    onGuardar: (String, String, String, String) -> Unit
) {
    val estados = listOf("En diagnostico", "Pend. autorizacion", "Listo", "Devuelto sin reparación")
    var estadoSel by remember { mutableStateOf(orden.estado) }
    var diagnosticoInput by remember { mutableStateOf(orden.diagnostico ?: "") }
    var solucionInput by remember { mutableStateOf("") }
    var skusInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gestionar Orden #${orden.id}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Estado:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                estados.forEach { est ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = estadoSel == est, onClick = { estadoSel = est })
                        Text(est, style = MaterialTheme.typography.bodySmall)
                    }
                }

                OutlinedTextField(
                    value = diagnosticoInput,
                    onValueChange = { diagnosticoInput = it },
                    label = { Text("Diagnóstico técnico") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = solucionInput,
                    onValueChange = { solucionInput = it },
                    label = { Text("Solución aplicada / Motivo devolución") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = skusInput,
                    onValueChange = { skusInput = it },
                    label = { Text("SKUs usados (ej: REP-PANT-MOD)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onGuardar(estadoSel, diagnosticoInput, solucionInput, skusInput) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun parsearEquipo(equipoModelo: String): Triple<String, String, String> {
    val lower = equipoModelo.lowercase()
    val categoria = when {
        listOf("macbook", "notebook", "thinkpad", "ideapad", "asus tuf", "pavilion", "inspiron", "laptop")
            .any { lower.contains(it) } -> "NOTEBOOK"
        listOf("rtx", "gtx", "radeon", "rx ", "gpu", "tarjeta")
            .any { lower.contains(it) } -> "GPU"
        else -> "SMARTPHONE"
    }
    val partes = equipoModelo.trim().split(" ", limit = 2)
    val marca = partes.getOrElse(0) { "Generico" }
    val modelo = partes.getOrElse(1) { equipoModelo }
    return Triple(categoria, marca, modelo)
}