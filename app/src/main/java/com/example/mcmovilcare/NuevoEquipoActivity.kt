package com.example.mcmovilcare

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.mcmovilcare.data.local.AppDatabase
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.ui.components.CyberBackground
import com.example.mcmovilcare.ui.theme.DigitalGhostFont
import com.example.mcmovilcare.ui.theme.MCMovilCareTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class NuevoEquipoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "mcmovilcare_db"
        )
            .fallbackToDestructiveMigration()
            .build()

        setContent {
            MCMovilCareTheme {
                RegistrarEquipoScreen(
                    onVolver = { finish() },
                    onGuardar = { serie, marca, modelo ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            val fechaHoy = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                            val nuevaOrden = OrdenTrabajo(
                                id = UUID.randomUUID().toString().take(5).uppercase(),
                                equipoModelo = "$marca $modelo".trim(),
                                numeroSerie = serie.trim(),
                                estado = "En diagnostico",
                                diagnostico = "Pendiente de revision tecnica",
                                cotizacionMonto = null,
                                requiereAutorizacion = false,
                                fechaIngreso = fechaHoy
                            )
                            database.ordenTrabajoDao().insertarOrden(nuevaOrden)

                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@NuevoEquipoActivity, "Equipo guardado en sistema", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarEquipoScreen(
    onVolver: () -> Unit,
    onGuardar: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    var codigoSerie by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val skuDetectado = "SKU-${UUID.randomUUID().toString().take(6).uppercase()}"
            codigoSerie = skuDetectado
            if (marca.isBlank()) marca = "Samsung"
            if (modelo.isBlank()) modelo = "Galaxy S24"
            Toast.makeText(context, "SKU capturado: $skuDetectado", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch()
        } else {
            Toast.makeText(context, "Se requiere permiso de camara para escanear", Toast.LENGTH_SHORT).show()
        }
    }

    CyberBackground(isDashboard = true) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "REGISTRAR NUEVO EQUIPO",
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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = codigoSerie,
                    onValueChange = { codigoSerie = it },
                    label = { Text("Codigo / Serie del Equipo", fontFamily = DigitalGhostFont) },
                    textStyle = TextStyle(fontFamily = DigitalGhostFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val tienePermiso = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (tienePermiso) {
                            cameraLauncher.launch()
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    )
                ) {
                    Text(
                        "Abrir Camara / Escanear QR o SKU",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = DigitalGhostFont,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = marca,
                    onValueChange = { marca = it },
                    label = { Text("Marca del Equipo (Ej: Samsung)", fontFamily = DigitalGhostFont) },
                    textStyle = TextStyle(fontFamily = DigitalGhostFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = modelo,
                    onValueChange = { modelo = it },
                    label = { Text("Modelo (Ej: Galaxy S22)", fontFamily = DigitalGhostFont) },
                    textStyle = TextStyle(fontFamily = DigitalGhostFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        when {
                            codigoSerie.isBlank() -> Toast.makeText(context, "Ingresa o escanea el SKU/Serie", Toast.LENGTH_SHORT).show()
                            marca.isBlank() -> Toast.makeText(context, "Ingresa la marca del equipo", Toast.LENGTH_SHORT).show()
                            modelo.isBlank() -> Toast.makeText(context, "Ingresa el modelo del equipo", Toast.LENGTH_SHORT).show()
                            else -> onGuardar(codigoSerie, marca, modelo)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        "Guardar Equipo en Sistema",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = DigitalGhostFont,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}