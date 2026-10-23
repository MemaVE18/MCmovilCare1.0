package com.example.mcmovilcare.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ordenes_trabajo")
data class OrdenTrabajo(
    @PrimaryKey val id: String,
    val equipoModelo: String,
    val numeroSerie: String,
    val estado: String,
    val diagnostico: String? = null,
    val cotizacionMonto: Double? = null,
    val requiereAutorizacion: Boolean = false,
    val fechaIngreso: String,
    val garantia: String? = "Garantia 90 dias vigente",
    val proximaMantencion: String? = "15-04-2027",
    val evidencia: String? = "Equipo ingresado sin danos adicionales en carcasa"
)