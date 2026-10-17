package com.example.mcmovilcare.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equipo")
class Equipo (
    @PrimaryKey
    val id: String,
    val clienteId: String,
    val marca: String,
    val modelo: String,
    val numeroSerie: String,
    val accesorios: String
){}