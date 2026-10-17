package com.example.mcmovilcare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScanQrScreen(
    onCodigoDetectado: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var codigoManual by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Escanear Código QR del Equipo",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Vista previa
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "Vista previa de la cámara")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(16.dp))

        // Entrada manual
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