package com.example.mcmovilcare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mcmovilcare.model.Equipo
import com.example.mcmovilcare.model.EquipoFormUiState
import com.example.mcmovilcare.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class EquipoViewModel(
    private val repository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EquipoFormUiState())
    val uiState: StateFlow<EquipoFormUiState> = _uiState.asStateFlow()

    fun onMarcaChanged(nuevaMarca: String) {
        _uiState.update { state ->
            val error = if (nuevaMarca.isBlank()) "La marca es requerida" else null
            state.copy(
                marca = nuevaMarca,
                errorMarca = error,
                esValido = error == null && state.modelo.isNotBlank() && state.numeroSerie.isNotBlank()
            )
        }
    }

    fun onModeloChanged(nuevoModelo: String) {
        _uiState.update { state ->
            val error = if (nuevoModelo.isBlank()) "El modelo es requerido" else null
            state.copy(
                modelo = nuevoModelo,
                errorModelo = error,
                esValido = state.marca.isNotBlank() && error == null && state.numeroSerie.isNotBlank()
            )
        }
    }

    fun onSerieChanged(nuevaSerie: String) {
        _uiState.update { state ->
            val error = if (nuevaSerie.isBlank()) "El número de serie es requerido" else null
            state.copy(
                numeroSerie = nuevaSerie,
                errorSerie = error,
                esValido = state.marca.isNotBlank() && state.modelo.isNotBlank() && error == null
            )
        }
    }

    fun guardarEquipo(clienteId: String) {
        val currentState = _uiState.value
        if (!currentState.esValido) return

        viewModelScope.launch {
            val nuevoEquipo = Equipo(
                id = UUID.randomUUID().toString(),
                clienteId = clienteId,
                marca = currentState.marca,
                modelo = currentState.modelo,
                numeroSerie = currentState.numeroSerie,
                accesorios = TODO()
            )
            repository.guardarEquipo(nuevoEquipo)
            _uiState.update { it.copy(guardadoExitoso = true) }
        }
    }
}