package com.example.mcmovilcare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevoEquipoScreen(
    onAbrirEscaner: ((String) -> Unit) -> Unit,
    onGuardarEquipo: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var codigoEquipo by remember { mutableStateOf("") }
    var nombreEquipo by remember { mutableStateOf("") }
    var modeloEquipo by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Registrar Nuevo Equipo",
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = codigoEquipo,
            onValueChange = { codigoEquipo = it },
            label = { Text("Código / Serie del Equipo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                onAbrirEscaner { codigoLeido ->
                    codigoEquipo = codigoLeido
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Abrir Cámara / Escanear Código QR")
        }

        OutlinedTextField(
            value = nombreEquipo,
            onValueChange = { nombreEquipo = it },
            label = { Text("Marca del Equipo (Ej: Samsung)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = modeloEquipo,
            onValueChange = { modeloEquipo = it },
            label = { Text("Modelo (Ej: Galaxy S22)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (codigoEquipo.isNotBlank() && nombreEquipo.isNotBlank()) {
                    onGuardarEquipo(codigoEquipo, nombreEquipo, modeloEquipo)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = codigoEquipo.isNotBlank() && nombreEquipo.isNotBlank()
        ) {
            Text("Guardar Equipo")
        }
    }
}