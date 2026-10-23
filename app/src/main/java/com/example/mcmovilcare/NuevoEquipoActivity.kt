package com.example.mcmovilcare

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
        ).fallbackToDestructiveMigration().build()

        setContent {
            NuevoEquipoScreen(
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

    fun abrirEscanerQR(onResultado: (String) -> Unit) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_ALL_FORMATS)
            .build()
        val scanner = GmsBarcodeScanning.getClient(this, options)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val codigoLeido = barcode.rawValue ?: ""
                Toast.makeText(this, "Codigo: $codigoLeido", Toast.LENGTH_SHORT).show()
                onResultado(codigoLeido)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al escanear: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}