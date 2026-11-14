package com.example.mcmovilcare.repository

import com.example.mcmovilcare.data.local.EquipoDao
import com.example.mcmovilcare.data.local.OrdenTrabajoDao
import com.example.mcmovilcare.model.Equipo
import com.example.mcmovilcare.model.OrdenTrabajo
import kotlinx.coroutines.flow.Flow

class TecnicoRepository(
    private val equipoDao: EquipoDao,
    private val ordenTrabajoDao: OrdenTrabajoDao
) {
    val obtenerOrdenesAsignadas: Flow<List<OrdenTrabajo>> = ordenTrabajoDao.obtenerOrdenes()

    val obtenerEquiposEnTaller: Flow<List<Equipo>> = equipoDao.obtenerEquipos()

    suspend fun actualizarDiagnosticoOEstado(ordenId: String, nuevoEstado: String) {
        ordenTrabajoDao.actualizarEstadoOrden(ordenId, nuevoEstado)
    }

    suspend fun guardarActualizacionOrden(orden: OrdenTrabajo) {
        ordenTrabajoDao.insertarOrden(orden)
    }
}