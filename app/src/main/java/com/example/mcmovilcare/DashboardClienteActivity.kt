package com.example.mcmovilcare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.room.Room
import com.example.mcmovilcare.data.local.AppDatabase
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.repository.AppRepository
import com.example.mcmovilcare.ui.components.CyberBackground
import com.example.mcmovilcare.ui.theme.DigitalGhostFont
import com.example.mcmovilcare.ui.theme.MCMovilCareTheme
import com.example.mcmovilcare.viewmodel.OrdenTrabajoViewModel
import java.text.NumberFormat
import java.util.Locale

class DashboardClienteActivity : ComponentActivity() {

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

                DashboardClienteScreenContent(
                    ordenes = listaOrdenes,
                    onOrdenClick = { orden -> abrirDetalleOrden(orden) },
                    onEscanearQR = {
                        startActivity(Intent(this, NuevoEquipoActivity::class.java))
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

    private fun abrirDetalleOrden(orden: OrdenTrabajo) {
        val intent = Intent(this, DetalleOrdenActivity::class.java).apply {
            putExtra("EXTRA_ID", orden.id)
            putExtra("EXTRA_EQUIPO", orden.equipoModelo)
            putExtra("EXTRA_SERIE", orden.numeroSerie)
            putExtra("EXTRA_ESTADO", orden.estado)
            putExtra("EXTRA_DIAGNOSTICO", orden.diagnostico ?: "Sin diagnostico registrado aun")
            putExtra("EXTRA_FECHA", orden.fechaIngreso)
            if (orden.cotizacionMonto != null) {
                putExtra("EXTRA_MONTO", orden.cotizacionMonto)
            }
            putExtra("EXTRA_AUTORIZADO", orden.requiereAutorizacion)
            putExtra("EXTRA_GARANTIA", orden.garantia ?: "Sin garantia activa")
            putExtra("EXTRA_MANTENCION", orden.proximaMantencion ?: "No programada")
            putExtra("EXTRA_EVIDENCIA", orden.evidencia ?: "Sin evidencias adjuntas")
        }
        startActivity(intent)
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
fun DashboardClienteScreenContent(
    ordenes: List<OrdenTrabajo>,
    onOrdenClick: (OrdenTrabajo) -> Unit,
    onEscanearQR: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    CyberBackground(isDashboard = true) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Hola, Cliente",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = DigitalGhostFont,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "MCMovilCare // SECTION-9",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = DigitalGhostFont),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        Button(
                            onClick = onEscanearQR,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Escanear QR",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Escanear QR",
                                fontFamily = DigitalGhostFont,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
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
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tus Ordenes de Trabajo",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DigitalGhostFont,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    TextButton(onClick = onCerrarSesion) {
                        Text(
                            "Cerrar sesion",
                            fontFamily = DigitalGhostFont,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                if (ordenes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tienes ordenes registradas",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = DigitalGhostFont),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(ordenes) { orden ->
                            TarjetaOrdenItem(
                                orden = orden,
                                onClick = { onOrdenClick(orden) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaOrdenItem(
    orden: OrdenTrabajo,
    onClick: () -> Unit
) {
    val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

    val (badgeBg, badgeFg) = when (orden.estado) {
        "En diagnostico" -> Color(0xFF332200) to Color(0xFFFFB74D)
        "Pend. autorizacion" -> Color(0xFF3B122B) to Color(0xFFFF4081)
        "Aprobado", "Listo" -> Color(0xFF00332C) to Color(0xFF00E5FF)
        "Rechazado" -> Color(0xFF3B1212) to Color(0xFFFF5252)
        else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Spacer(modifier = Modifier.width(10.dp))
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
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = DigitalGhostFont),
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

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = orden.diagnostico ?: "Sin diagnostico registrado aun",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = DigitalGhostFont),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                if (orden.cotizacionMonto != null && orden.cotizacionMonto > 0) {
                    Text(
                        text = formatoMoneda.format(orden.cotizacionMonto),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DigitalGhostFont,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}