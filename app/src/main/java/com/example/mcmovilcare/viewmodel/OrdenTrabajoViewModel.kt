package com.example.mcmovilcare.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mcmovilcare.ApiService
import com.example.mcmovilcare.RetrofitClient
import com.example.mcmovilcare.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OrdenTrabajoViewModel(
    private val repository: AppRepository,
    private val apiService: ApiService = RetrofitClient.apiService
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMensaje = MutableStateFlow<String?>(null)
    val errorMensaje: StateFlow<String?> = _errorMensaje.asStateFlow()

    private val _ultimaSincronizacionCount = MutableStateFlow(0)
    val ultimaSincronizacionCount: StateFlow<Int> = _ultimaSincronizacionCount.asStateFlow()

    fun cargarEquiposDesdeApi() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMensaje.value = null
            try {
                val ordenesApi = apiService.obtenerOrdenes()
                for (o in ordenesApi) {
                    repository.guardarOrden(o)
                }
                _ultimaSincronizacionCount.value = ordenesApi.size
                Log.d("API_REST", "Ordenes sincronizadas desde la API: ${ordenesApi.size}")
            } catch (e: Exception) {
                _errorMensaje.value = e.message ?: "Error de red al sincronizar órdenes"
                Log.e("API_REST", "Error de red: ${e.message}")
            } finally {
                _isLoading.value = false
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
            _errorMensaje.value = null
            repository.cambiarEstadoOrden(ordenId, nuevoEstado)
            try {
                val ordenActual = repository.obtenerOrdenes.first().find { it.id == ordenId }
                if (ordenActual != null) {
                    val ordenActualizada = ordenActual.copy(
                        estado = nuevoEstado,
                        requiereAutorizacion = false
                    )
                    apiService.actualizarOrden(ordenId, ordenActualizada)
                    repository.guardarOrden(ordenActualizada)
                }
            } catch (e: Exception) {
                _errorMensaje.value = e.message ?: "No se pudo sincronizar estado con API"
                Log.e("API_REST", "No se pudo sincronizar estado con API: ${e.message}")
            }
        }
    }
}