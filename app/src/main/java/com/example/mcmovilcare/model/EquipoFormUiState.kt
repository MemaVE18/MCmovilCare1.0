package com.example.mcmovilcare.model

data class EquipoFormUiState(
    val marca: String = "",
    val modelo: String = "",
    val numeroSerie: String = "",
    val errorMarca: String? = null,
    val errorModelo: String? = null,
    val errorSerie: String? = null,
    val esValido: Boolean = false,
    val guardadoExitoso: Boolean = false
)