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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.mcmovilcare.data.local.AppDatabase
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.model.Rol
import com.example.mcmovilcare.model.Usuario
import com.example.mcmovilcare.network.DiagnosticoRagRequest
import com.example.mcmovilcare.network.DiagnosticoRagResponse
import com.example.mcmovilcare.network.NuevoCasoRagRequest
import com.example.mcmovilcare.network.RagRetrofitClient
import com.example.mcmovilcare.ui.components.CyberBackground
import com.example.mcmovilcare.ui.theme.DigitalGhostFont
import com.example.mcmovilcare.ui.theme.MCMovilCareTheme
import com.example.mcmovilcare.util.NotificacionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
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
                        onCrearUsuario = { nombre, correo, pass, rol ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                val nuevo = Usuario(
                                    id = UUID.randomUUID().toString().take(5),
                                    nombre = nombre,
                                    correo = correo.lowercase().trim(),
                                    rol = rol,
                                    password = pass.ifBlank { "1234" }
                                )
                                try {
                                    val creadoApi = RetrofitClient.apiService.crearUsuario(nuevo)
                                    database.usuarioDao().insertarUsuario(creadoApi)
                                } catch (_: Exception) {
                                    database.usuarioDao().insertarUsuario(nuevo)
                                }
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@HistorialActivity, "Usuario guardado con exito", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onCerrarSesion = ejecutarCierreSesion
                    )
                } else {
                    PanelTecnicoScreen(
                        ordenes = listaOrdenes,
                        onActualizarOrden = { ordenId, estado, diag, monto, requiereAut, evidencia, solucionRag, skusRag ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                val ordenPrevia = database.ordenTrabajoDao().obtenerOrdenPorId(ordenId)

                                database.ordenTrabajoDao().actualizarOrdenTecnico(
                                    ordenId = ordenId,
                                    nuevoEstado = estado,
                                    nuevoDiagnostico = diag,
                                    nuevoMonto = monto,
                                    requiereAut = requiereAut,
                                    nuevaEvidencia = evidencia
                                )

                                val ordenActualizada = database.ordenTrabajoDao().obtenerOrdenPorId(ordenId)
                                if (ordenActualizada != null) {
                                    try {
                                        RetrofitClient.apiService.actualizarOrden(ordenId, ordenActualizada)
                                    } catch (_: Exception) {
                                    }

                                    val esCierre = estado.equals("Listo", ignoreCase = true) ||
                                            estado.equals("Devuelto sin reparacion", ignoreCase = true)

                                    if (esCierre) {
                                        val (categoria, marca, modelo) = parsearEquipoRag(ordenActualizada.equipoModelo)
                                        val listaSkus = skusRag.split(",")
                                            .map { it.trim() }
                                            .filter { it.isNotBlank() }

                                        try {
                                            RagRetrofitClient.api.registrarCasoCerrado(
                                                NuevoCasoRagRequest(
                                                    ordenCodigo = "OT-${ordenActualizada.id}",
                                                    categoria = categoria,
                                                    marca = marca,
                                                    modelo = modelo,
                                                    ean = null,
                                                    sintoma = ordenPrevia?.diagnostico?.takeIf { it.isNotBlank() }
                                                        ?: diag.ifBlank { "Falla reportada en ingreso" },
                                                    diagnostico = diag.ifBlank { "Diagnostico tecnico en taller" },
                                                    solucionAplicada = solucionRag.ifBlank {
                                                        if (estado == "Listo") "Reparacion completada en taller"
                                                        else "Equipo devuelto sin reparacion tras evaluacion"
                                                    },
                                                    estadoFinal = if (estado == "Listo") "REPARADO" else "DEVUELTO_SIN_REPARACION",
                                                    repuestosSku = listaSkus
                                                )
                                            )
                                        } catch (_: Exception) {
                                        }
                                    }
                                }

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

private fun parsearEquipoRag(equipoModelo: String): Triple<String, String, String> {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelTecnicoScreen(
    ordenes: List<OrdenTrabajo>,
    onActualizarOrden: (String, String, String, Double?, Boolean, String, String, String) -> Unit,
    onAbrirEscaner: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var ordenSeleccionada by remember { mutableStateOf<OrdenTrabajo?>(null) }

    var ordenRag by remember { mutableStateOf<OrdenTrabajo?>(null) }
    var cargandoRag by remember { mutableStateOf(false) }
    var respuestaRag by remember { mutableStateOf<DiagnosticoRagResponse?>(null) }
    var errorRag by remember { mutableStateOf<String?>(null) }

    val formatoMoneda = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")) }

    val lanzarConsultaRag: (OrdenTrabajo, String) -> Unit = { orden, sintomaCustom ->
        ordenRag = orden
        respuestaRag = null
        errorRag = null
        cargandoRag = true

        val (categoria, marca, modelo) = parsearEquipoRag(orden.equipoModelo)
        val sintomaConsulta = sintomaCustom.takeIf { it.isNotBlank() }
            ?: orden.diagnostico?.takeIf { it.isNotBlank() }
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
                    errorRag = e.localizedMessage ?: "No se pudo conectar al servidor RAG (34.228.42.153:8080)"
                    cargandoRag = false
                }
            }
        }
    }

    CyberBackground(isDashboard = true) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Panel Tecnico",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = DigitalGhostFont,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "MCMovilCare // DIAGNOSTIC-UNIT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = DigitalGhostFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = onCerrarSesion) {
                            Text(
                                "Cerrar Sesion",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = DigitalGhostFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
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
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Ingresar Equipo / Escanear QR",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = DigitalGhostFont,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                item {
                    Text(
                        text = "Ordenes de Trabajo (${ordenes.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DigitalGhostFont,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(ordenes) { orden ->
                    val (badgeBg, badgeFg) = when (orden.estado) {
                        "En diagnostico" -> Color(0xFF332200) to Color(0xFFFFB74D)
                        "Pend. autorizacion", "Aprobado" -> Color(0xFF3B122B) to Color(0xFFFF4081)
                        "Listo" -> Color(0xFF00332C) to Color(0xFF38BDF8)
                        "Devuelto sin reparacion" -> Color(0xFF3B1219) to Color(0xFFFF5252)
                        else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Card(
                        onClick = { ordenSeleccionada = orden },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = DigitalGhostFont,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = "S/N: ${orden.numeroSerie}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = DigitalGhostFont,
                                                fontWeight = FontWeight.Bold
                                            ),
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

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                            Text(
                                text = "Diagnostico: ${orden.diagnostico ?: "Sin registro aun"}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = DigitalGhostFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Evidencia: ${orden.evidencia ?: "Sin evidencia registrada"}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = DigitalGhostFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (orden.cotizacionMonto != null) {
                                Text(
                                    text = "Cotizacion: ${formatoMoneda.format(orden.cotizacionMonto)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = DigitalGhostFont,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Toca la tarjeta para editar",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = DigitalGhostFont,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                FilledTonalButton(
                                    onClick = { lanzarConsultaRag(orden, orden.diagnostico ?: "") },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "Asistente IA (RAG)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = DigitalGhostFont,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOGO 1: EDITAR ORDEN
    if (ordenSeleccionada != null) {
        val orden = ordenSeleccionada!!
        var diag by remember { mutableStateOf(orden.diagnostico ?: "") }
        var montoTexto by remember { mutableStateOf(orden.cotizacionMonto?.let { String.format(Locale.US, "%.0f", it) } ?: "") }
        var evidencia by remember { mutableStateOf(orden.evidencia ?: "Foto frontal y test de hardware OK") }
        var solucionRag by remember { mutableStateOf("") }
        var skusRag by remember { mutableStateOf("") }
        var estadoElegido by remember { mutableStateOf(orden.estado) }

        AlertDialog(
            onDismissRequest = { ordenSeleccionada = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    "Gestionar ${orden.equipoModelo}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = DigitalGhostFont,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 430.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { lanzarConsultaRag(orden, diag) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "Consultar Asistente IA con este sintoma",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = DigitalGhostFont,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    OutlinedTextField(
                        value = diag,
                        onValueChange = { diag = it },
                        label = { Text("Diagnostico tecnico / Sintoma", fontFamily = DigitalGhostFont) },
                        textStyle = TextStyle(fontFamily = DigitalGhostFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = montoTexto,
                        onValueChange = { montoTexto = it },
                        label = { Text("Monto cotizacion ($)", fontFamily = DigitalGhostFont) },
                        textStyle = TextStyle(fontFamily = DigitalGhostFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = evidencia,
                        onValueChange = { evidencia = it },
                        label = { Text("Evidencia / Nota fotografica", fontFamily = DigitalGhostFont) },
                        textStyle = TextStyle(fontFamily = DigitalGhostFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        "Cambiar Estado:",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = DigitalGhostFont,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = estadoElegido == "En diagnostico",
                            onClick = { estadoElegido = "En diagnostico" },
                            label = {
                                Text(
                                    "Diag.",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = DigitalGhostFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = estadoElegido == "Pend. autorizacion",
                            onClick = { estadoElegido = "Pend. autorizacion" },
                            label = {
                                Text(
                                    "Cotizar",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = DigitalGhostFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = estadoElegido == "Listo",
                            onClick = { estadoElegido = "Listo" },
                            label = {
                                Text(
                                    "Listo",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = DigitalGhostFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = estadoElegido == "Devuelto sin reparacion",
                            onClick = { estadoElegido = "Devuelto sin reparacion" },
                            label = {
                                Text(
                                    "Devuelto",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = DigitalGhostFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (estadoElegido == "Listo" || estadoElegido == "Devuelto sin reparacion") {
                        OutlinedTextField(
                            value = solucionRag,
                            onValueChange = { solucionRag = it },
                            label = { Text("Solucion aplicada / Motivo devolucion (IA)", fontFamily = DigitalGhostFont) },
                            textStyle = TextStyle(fontFamily = DigitalGhostFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = skusRag,
                            onValueChange = { skusRag = it },
                            label = { Text("SKUs repuestos (ej: REP-PANT-MOD)", fontFamily = DigitalGhostFont) },
                            textStyle = TextStyle(fontFamily = DigitalGhostFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val montoNum = montoTexto.toDoubleOrNull()
                        val requiereAut = estadoElegido == "Pend. autorizacion"
                        onActualizarOrden(
                            orden.id,
                            estadoElegido,
                            diag,
                            montoNum,
                            requiereAut,
                            evidencia,
                            solucionRag,
                            skusRag
                        )
                        ordenSeleccionada = null
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "Guardar Cambios",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = DigitalGhostFont,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { ordenSeleccionada = null }) {
                    Text(
                        "Cancelar",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = DigitalGhostFont,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        )
    }

    // DIALOGO 2: RESPUESTA DEL ASISTENTE TECNICO IA (RAG)
    if (ordenRag != null) {
        val orden = ordenRag!!
        AlertDialog(
            onDismissRequest = { ordenRag = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Column {
                    Text(
                        text = "ASISTENTE IA // DIAGNOSTICO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = DigitalGhostFont,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = orden.equipoModelo,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DigitalGhostFont,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (cargandoRag) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "> CONSULTANDO BASE VECTORIAL PGVECTOR...",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = DigitalGhostFont,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                )

                                Text(
                                    text = "Comparando sintomas y casos previos en taller",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = DigitalGhostFont,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    errorRag?.let { err ->
                        Text(
                            text = "Error conectando al RAG: $err",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = DigitalGhostFont,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    respuestaRag?.let { rag ->
                        Text(
                            text = "MODO: ${rag.modoSeguro.uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = DigitalGhostFont,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        rag.advertenciaDevolucion?.takeIf { it.isNotBlank() }?.let { alerta ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = "Alerta historial: $alerta",
                                    modifier = Modifier.padding(10.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = DigitalGhostFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        if (rag.causasSeguras.isNotEmpty()) {
                            Text(
                                text = "Causas probables:",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = DigitalGhostFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            rag.causasSeguras.forEach {
                                Text(
                                    text = "- $it",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = DigitalGhostFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        if (rag.pasosSeguros.isNotEmpty()) {
                            Text(
                                text = "Pasos de revision en meson:",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = DigitalGhostFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            rag.pasosSeguros.forEachIndexed { idx, paso ->
                                Text(
                                    text = "${idx + 1}. $paso",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = DigitalGhostFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        if (rag.solucionesSeguras.isNotEmpty()) {
                            Text(
                                text = "Soluciones aplicadas en taller:",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = DigitalGhostFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            rag.solucionesSeguras.forEach {
                                Text(
                                    text = "- $it",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = DigitalGhostFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Aviso: Estas soluciones provienen de casos previos en otros equipos. No son 100% infalibles y deben usarse solo como referencia tecnica.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = DigitalGhostFont,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (rag.skusSeguros.isNotEmpty()) {
                            Text(
                                text = "SKUs sugeridos: ${rag.skusSeguros.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = DigitalGhostFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { ordenRag = null }) {
                    Text(
                        "CERRAR",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = DigitalGhostFont,
                            fontWeight = FontWeight.Bold
                        )
                    )
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
    onCrearUsuario: (String, String, String, Rol) -> Unit,
    onCerrarSesion: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rolSeleccionado by remember { mutableStateOf(Rol.TECNICO) }

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
                                text = "MCMovilCare // SECTION-9 COMMAND",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = DigitalGhostFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = onCerrarSesion) {
                            Text(
                                "Cerrar Sesion",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = DigitalGhostFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
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
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                                value = nombre,
                                onValueChange = { nombre = it },
                                label = { Text("Nombre", fontFamily = DigitalGhostFont) },
                                textStyle = TextStyle(fontFamily = DigitalGhostFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = correo,
                                onValueChange = { correo = it },
                                label = { Text("Correo (ej: tecnico2@duoc.cl)", fontFamily = DigitalGhostFont) },
                                textStyle = TextStyle(fontFamily = DigitalGhostFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Contrasena", fontFamily = DigitalGhostFont) },
                                textStyle = TextStyle(fontFamily = DigitalGhostFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                visualTransformation = PasswordVisualTransformation(),
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
                                Rol.values().forEach { rol ->
                                    FilterChip(
                                        selected = (rolSeleccionado == rol),
                                        onClick = { rolSeleccionado = rol },
                                        label = {
                                            Text(
                                                rol.name,
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontFamily = DigitalGhostFont,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (nombre.isNotBlank() && correo.isNotBlank()) {
                                        onCrearUsuario(nombre, correo, password, rolSeleccionado)
                                        nombre = ""
                                        correo = ""
                                        password = ""
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "Guardar Usuario en BD",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = DigitalGhostFont,
                                        fontWeight = FontWeight.Bold
                                    )
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
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                            ItemCatalogoHistorial(titulo = "Mantencion preventiva general", precio = 25000.0)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            ItemCatalogoHistorial(titulo = "Cambio de bateria certificada", precio = 45000.0)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            ItemCatalogoHistorial(titulo = "Reemplazo modulo pantalla", precio = 150000.0)
                        }
                    }
                }

                item {
                    Text(
                        text = "Perfiles en Base de Datos (${usuarios.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DigitalGhostFont,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                items(usuarios) { u ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    u.nombre,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = DigitalGhostFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    u.correo,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = DigitalGhostFont,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = u.rol.name,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = DigitalGhostFont,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
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

                items(ordenes) { o ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Orden #${o.id} • ${o.equipoModelo}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = DigitalGhostFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    "S/N: ${o.numeroSerie}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = DigitalGhostFont,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = o.estado,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = DigitalGhostFont,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
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
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = DigitalGhostFont,
                    fontWeight = FontWeight.Bold
                ),
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