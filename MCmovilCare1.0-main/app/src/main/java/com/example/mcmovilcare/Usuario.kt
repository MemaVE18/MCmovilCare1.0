package com.example.mcmovilcare

enum class Rol{ CLIENTE, TECNICO, ADMIN }

data class Usuario(
        val id: String,
        val nombre: String,
        val correo: String,
        val rol: Rol
        )