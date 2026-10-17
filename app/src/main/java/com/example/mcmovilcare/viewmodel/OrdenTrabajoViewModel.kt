package com.example.mcmovilcare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mcmovilcare.repository.AppRepository
import kotlinx.coroutines.launch

class OrdenTrabajoViewModel(
    private val repository: AppRepository
) : ViewModel() {

    fun aprobarOrden(ordenId: String) {
        viewModelScope.launch {
            repository.cambiarEstadoOrden(ordenId, "Aprobado")
        }
    }

    fun rechazarOrden(ordenId: String) {
        viewModelScope.launch {
            repository.cambiarEstadoOrden(ordenId, "Rechazado")
        }
    }
}