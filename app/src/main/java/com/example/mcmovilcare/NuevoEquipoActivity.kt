package com.example.mcmovilcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.mcmovilcare.ui.screens.ScanQrScreen

class NuevoEquipoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScanQrScreen(
                onCodigoDetectado = { codigo ->
                    // Al detectar el QR, cierra la pantalla y vuelve atrás
                    finish()
                }
            )
        }
    }
}