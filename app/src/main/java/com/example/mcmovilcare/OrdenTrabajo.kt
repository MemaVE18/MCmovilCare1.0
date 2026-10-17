package com.example.mcmovilcare

data class OrdenTrabajo(
    val id: String,
    val equipoModelo: String,
    val numeroSerie: String,
    val estado: String,             // "En diagnóstico", "Pendiente de autorización", "Listo"
    val diagnostico: String?,
    val cotizacionMonto: Double?,   // LLEVA '?' porque al inicio puede no haber cotización
    val requiereAutorizacion: Boolean,
    val fechaIngreso: String
)