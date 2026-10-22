package com.example.mcmovilcare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mcmovilcare.model.OrdenTrabajo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    ordenes: List<OrdenTrabajo>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Historial de Reparaciones",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (ordenes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(text = "No hay órdenes registradas.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ordenes) { orden ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = orden.equipoModelo, style = MaterialTheme.typography.titleMedium)
                            Text(text = "Estado: ${orden.estado}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Fecha: ${orden.fechaIngreso}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}