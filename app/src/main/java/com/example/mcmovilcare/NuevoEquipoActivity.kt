package com.example.mcmovilcare

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.mcmovilcare.data.local.AppDatabase
import com.example.mcmovilcare.model.Equipo
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.ui.screens.NuevoEquipoScreen
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class NuevoEquipoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "mcmovilcare_db"
        )
            .enableMultiInstanceInvalidation() // Sincroniza listas entre pantallas
            .fallbackToDestructiveMigration()
            .build()

        setContent {
            Column(modifier = Modifier.fillMaxSize()) {
                // Botón Volver agregado en la parte superior
                Button(
                    onClick = { finish() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E4B8B)),
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                ) {
                    Text("< Volver", color = Color.White)
                }

                // Contenedor para la pantalla original de registro
                Box(modifier = Modifier.weight(1f)) {
                    NuevoEquipoScreen(
                        onAbrirEscaner = { onCodigoLeido ->
                            abrirEscanerQR { codigo ->
                                onCodigoLeido(codigo)
                            }
                        },
                        onGuardarEquipo = { codigoSerie, marcaNombre, modelo ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                val nuevoId = UUID.randomUUID().toString().take(5)

                                // 1. Guardar el equipo en Room
                                val nuevoEquipo = Equipo(
                                    id = nuevoId,
                                    clienteId = "cliente_1",
                                    marca = marcaNombre,
                                    modelo = modelo,
                                    numeroSerie = codigoSerie,
                                    accesorios = "Cargador y bolso"
                                )
                                database.equipoDao().insertarEquipo(nuevoEquipo)

                                // 2. Generar la solicitud / orden de trabajo inicial en Room
                                val nuevaOrden = OrdenTrabajo(
                                    id = nuevoId,
                                    equipoModelo = "$marcaNombre $modelo".trim(),
                                    numeroSerie = codigoSerie,
                                    estado = "Solicitud ingresada",
                                    diagnostico = "Pendiente de revision por tecnico",
                                    cotizacionMonto = null,
                                    requiereAutorizacion = false,
                                    fechaIngreso = "22-10-2026"
                                )
                                database.ordenTrabajoDao().insertarOrden(nuevaOrden)

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@NuevoEquipoActivity,
                                        "Equipo y solicitud registrados: $marcaNombre $modelo",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    finish()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun abrirEscanerQR(onResultado: (String) -> Unit) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_ALL_FORMATS)
            .build()
        val scanner = GmsBarcodeScanning.getClient(this, options)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val codigoLeido = barcode.rawValue ?: ""
                Toast.makeText(this, "Codigo escaneado con éxito", Toast.LENGTH_SHORT).show()
                onResultado(codigoLeido)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al escanear: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}