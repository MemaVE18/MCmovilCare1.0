package com.example.mcmovilcare.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SeccionBotonesAprobacion(
    ordenId: String,
    onAprobar: (String) -> Unit,
    onRechazar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var presionado by remember { mutableStateOf(false) }

    val colorAnimado by animateColorAsState(
        targetValue = if (presionado) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 400),
        label = "EfectoColorAprobar"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Botón Rechazar
        OutlinedButton(
            onClick = { onRechazar(ordenId) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(text = "Rechazar")
        }

        Button(
            onClick = {
                presionado = !presionado
                onAprobar(ordenId)
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorAnimado
            )
        ) {
            Text(text = "Aprobar")
        }
    }
}