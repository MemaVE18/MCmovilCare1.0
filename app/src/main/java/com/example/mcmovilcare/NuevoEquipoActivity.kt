package com.example.mcmovilcare

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.mcmovilcare.ui.screens.ScanQrScreen
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions

class NuevoEquipoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ScanQrScreen(
                onCodigoDetectado = { codigo ->
                    // Al detectar el QR desde la UI Compose
                    Toast.makeText(this, "Código detectado: $codigo", Toast.LENGTH_SHORT).show()
                    finish()
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
                Toast.makeText(this, "Código: $codigoLeido", Toast.LENGTH_SHORT).show()
                onResultado(codigoLeido)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al escanear: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}