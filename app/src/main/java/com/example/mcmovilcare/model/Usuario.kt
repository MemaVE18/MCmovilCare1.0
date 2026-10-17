package com.example.mcmovilcare.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Rol{ CLIENTE, TECNICO, ADMIN }

@Entity(tableName = "usuario")
data class Usuario(
        @PrimaryKey
        val id: String,
        val nombre: String,
        val correo: String,
        val rol: Rol
        )