package com.example.mcmovilcare.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@Composable
fun ScanQrScreen(
    onCodigoDetectado: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var codigoManual by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Escanear QR o SKU del Equipo",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Box(contentAlignment = Alignment.Center) {
                Button(
                    onClick = {
                        val options = GmsBarcodeScannerOptions.Builder()
                            .setBarcodeFormats(
                                Barcode.FORMAT_ALL_FORMATS
                            )
                            .build()
                        val scanner = GmsBarcodeScanning.getClient(context, options)
                        scanner.startScan()
                            .addOnSuccessListener { barcode ->
                                val valor = barcode.rawValue.orEmpty()
                                if (valor.isNotBlank()) {
                                    onCodigoDetectado(valor)
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "Error al escanear: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                ) {
                    Text("Abrir Escáner Cámara (QR / SKU)")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "¿No puedes escanear? Ingrésalo manualmente:",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = codigoManual,
            onValueChange = { codigoManual = it },
            label = { Text("Código de equipo / Serie") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (codigoManual.isNotBlank()) {
                    onCodigoDetectado(codigoManual)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = codigoManual.isNotBlank()
        ) {
            Text("Confirmar Código")
        }
    }
}