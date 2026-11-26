package com.example.mcmovilcare

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.room.Room
import com.example.mcmovilcare.data.local.AppDatabase
import com.example.mcmovilcare.repository.AppRepository
import com.example.mcmovilcare.ui.components.CyberBackground
import com.example.mcmovilcare.ui.theme.DigitalGhostFont
import com.example.mcmovilcare.ui.theme.MCMovilCareTheme
import com.example.mcmovilcare.util.NotificacionHelper
import com.example.mcmovilcare.util.PdfGenerator
import com.example.mcmovilcare.viewmodel.OrdenTrabajoViewModel
import java.text.NumberFormat
import java.util.Locale

class DetalleOrdenActivity : ComponentActivity() {
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

        val repository = AppRepository(
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
        val viewModel = ViewModelProvider(this, factory)[OrdenTrabajoViewModel::class.java]

        val id = intent.getStringExtra("EXTRA_ID") ?: "N/A"
        val equipo = intent.getStringExtra("EXTRA_EQUIPO") ?: "Equipo"
        val serie = intent.getStringExtra("EXTRA_SERIE") ?: "Sin serie"
        val estadoInicial = intent.getStringExtra("EXTRA_ESTADO") ?: "Desconocido"
        val diagnostico = intent.getStringExtra("EXTRA_DIAGNOSTICO") ?: "Sin diagnostico registrado aun"
        val monto = intent.getDoubleExtra("EXTRA_MONTO", -1.0)
        val requiereAutorizacion = intent.getBooleanExtra("EXTRA_AUTORIZADO", false)
        val fecha = intent.getStringExtra("EXTRA_FECHA") ?: "Sin fecha"

        setContent {
            MCMovilCareTheme(darkTheme = true) {
                DetalleOrdenScreenContent(
                    id = id,
                    equipo = equipo,
                    serie = serie,
                    estadoInicial = estadoInicial,
                    diagnostico = diagnostico,
                    monto = monto,
                    requiereAutorizacion = requiereAutorizacion,
                    fecha = fecha,
                    onAprobar = {
                        viewModel.aprobarOrden(id)
                        NotificacionHelper.enviarNotificacion(
                            this,
                            "Cotizacion Aprobada (Orden #$id)",
                            "Has aprobado la reparacion de tu $equipo."
                        )
                        Toast.makeText(this, "Cotizacion aprobada con exito", Toast.LENGTH_SHORT).show()
                    },
                    onRechazar = {
                        viewModel.rechazarOrden(id)
                        NotificacionHelper.enviarNotificacion(
                            this,
                            "Cotizacion Rechazada (Orden #$id)",
                            "Has rechazado la reparacion de tu $equipo."
                        )
                        Toast.makeText(this, "Cotizacion rechazada", Toast.LENGTH_SHORT).show()
                    },
                    onGenerarPdf = { estadoActual ->
                        val textoComprobante = buildString {
                            appendLine("Orden #$id")
                            appendLine("Equipo: $equipo")
                            appendLine("S/N: $serie")
                            append("Estado: $estadoActual")
                        }
                        PdfGenerator().generarPdfOrden(this, textoComprobante)
                    },
                    onVolver = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleOrdenScreenContent(
    id: String,
    equipo: String,
    serie: String,
    estadoInicial: String,
    diagnostico: String,
    monto: Double,
    requiereAutorizacion: Boolean,
    fecha: String,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit,
    onGenerarPdf: (String) -> Unit,
    onVolver: () -> Unit
) {
    var estadoActual by remember { mutableStateOf(estadoInicial) }
    val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

    val mostrarBotonesAprobacion = (requiereAutorizacion || estadoActual == "Pend. autorizacion") &&
            estadoActual != "Aprobado" && estadoActual != "Rechazado"

    CyberBackground(isDashboard = true) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "DETALLE DE ORDEN #$id",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = DigitalGhostFont,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onVolver) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
                            text = equipo,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = DigitalGhostFont,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "S/N: $serie  |  Ingreso: $fecha",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = DigitalGhostFont),
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "Estado: $estadoActual",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = DigitalGhostFont,
                                fontWeight = FontWeight.Bold
                            ),
                            color = when (estadoActual) {
                                "Aprobado", "Listo" -> Color(0xFF00E5FF)
                                "Rechazado" -> Color(0xFFFF5252)
                                else -> Color(0xFFFFB74D)
                            }
                        )
                        Text(
                            text = "Diagnostico: $diagnostico",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = DigitalGhostFont)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = if (monto > 0) "Costo de reparacion: ${formatoMoneda.format(monto)}" else "Costo: Pendiente de cotizacion",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = DigitalGhostFont,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (monto > 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (mostrarBotonesAprobacion) {
                    Text(
                        text = "Cotizacion pendiente de tu autorizacion",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = DigitalGhostFont,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFFF9800)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                estadoActual = "Rechazado"
                                onRechazar()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFF5252)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3B1212).copy(alpha = 0.75f),
                                contentColor = Color(0xFFFF5252)
                            )
                        ) {
                            Text(
                                "Rechazar",
                                fontFamily = DigitalGhostFont,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5252)
                            )
                        }

                        Button(
                            onClick = {
                                estadoActual = "Aprobado"
                                onAprobar()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                "Aprobar",
                                fontFamily = DigitalGhostFont,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { onGenerarPdf(estadoActual) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        "Descargar Comprobante PDF",
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
}