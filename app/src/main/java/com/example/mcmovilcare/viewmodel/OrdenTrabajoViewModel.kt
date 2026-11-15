package com.example.mcmovilcare.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mcmovilcare.RetrofitClient
import com.example.mcmovilcare.repository.AppRepository
import kotlinx.coroutines.launch

class OrdenTrabajoViewModel(
    private val repository: AppRepository
) : ViewModel() {

    fun cargarEquiposDesdeApi() {
        viewModelScope.launch {
            try {
                val ordenesApi = RetrofitClient.apiService.obtenerOrdenes()
                for (o in ordenesApi) {
                    repository.guardarOrden(o)
                }
                Log.d("API_REST", "Ordenes sincronizadas desde la API: ${ordenesApi.size}")
            } catch (e: Exception) {
                Log.e("API_REST", "Error de red: ${e.message}")
            }
        }
    }

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