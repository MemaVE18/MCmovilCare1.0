package com.example.mcmovilcare.repository

import com.example.mcmovilcare.data.local.EquipoDao
import com.example.mcmovilcare.data.local.OrdenTrabajoDao
import com.example.mcmovilcare.data.local.UsuarioDao
import com.example.mcmovilcare.model.Equipo
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.model.Usuario
import kotlinx.coroutines.flow.Flow

class ClienteRepository(
    private val usuarioDao: UsuarioDao,
    private val equipoDao: EquipoDao,
    private val ordenTrabajoDao: OrdenTrabajoDao
) {
    fun obtenerPerfilCliente(id: String): Flow<Usuario?> = usuarioDao.obtenerUsuarioPorId(id)

    val obtenerMisEquipos: Flow<List<Equipo>> = equipoDao.obtenerEquipos()

    suspend fun registrarEquipo(equipo: Equipo) {
        equipoDao.insertarEquipo(equipo)
    }

    val obtenerMisOrdenes: Flow<List<OrdenTrabajo>> = ordenTrabajoDao.obtenerOrdenes()

    suspend fun crearSolicitudOrden(orden: OrdenTrabajo) {
        ordenTrabajoDao.insertarOrden(orden)
    }

    suspend fun aprobarORechazarPresupuesto(ordenId: String, nuevoEstado: String) {
        ordenTrabajoDao.actualizarEstadoOrden(ordenId, nuevoEstado)
    }
}