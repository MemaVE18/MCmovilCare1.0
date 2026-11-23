package com.example.mcmovilcare.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mcmovilcare.RetrofitClient
import com.example.mcmovilcare.repository.AppRepository
import kotlinx.coroutines.flow.first
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
        actualizarEstadoEnLocalYApi(ordenId, "Aprobado")
    }

    fun rechazarOrden(ordenId: String) {
        actualizarEstadoEnLocalYApi(ordenId, "Rechazado")
    }

    private fun actualizarEstadoEnLocalYApi(ordenId: String, nuevoEstado: String) {
        viewModelScope.launch {
            repository.cambiarEstadoOrden(ordenId, nuevoEstado)
            try {
                val ordenActual = repository.obtenerOrdenes.first().find { it.id == ordenId }
                if (ordenActual != null) {
                    val ordenActualizada = ordenActual.copy(
                        estado = nuevoEstado,
                        requiereAutorizacion = false
                    )
                    RetrofitClient.apiService.actualizarOrden(ordenId, ordenActualizada)
                    repository.guardarOrden(ordenActualizada)
                }
            } catch (e: Exception) {
                Log.e("API_REST", "No se pudo sincronizar estado con API: ${e.message}")
            }
        }
    }
}