package com.example.mcmovilcare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TecnicoDashboardScreen(navController: NavController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Mis Trabajos Asignados") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text("Lista de órdenes del técnico")
        }
    }
}