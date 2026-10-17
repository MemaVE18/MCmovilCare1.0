package com.example.mcmovilcare.repository

import com.example.mcmovilcare.data.local.EquipoDao
import com.example.mcmovilcare.data.local.OrdenTrabajoDao
import com.example.mcmovilcare.data.local.UsuarioDao
import com.example.mcmovilcare.model.Equipo
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.model.Usuario
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val usuarioDao: UsuarioDao,
    private val equipoDao: EquipoDao,
    private val ordenTrabajoDao: OrdenTrabajoDao
) {
    // --- Operaciones de Usuario ---
    val obtenerUsuarios: Flow<List<Usuario>> get() = usuarioDao.obtenerTodosLosUsuarios()

    fun obtenerUsuario(id: String): Flow<Usuario?> = usuarioDao.obtenerUsuarioPorId(id)

    suspend fun guardarUsuario(usuario: Usuario) {
        usuarioDao.insertarUsuario(usuario)
    }

    // --- Operaciones de Equipos ---
    val obtenerEquipos: Flow<List<Equipo>> = equipoDao.obtenerEquipos()

    suspend fun guardarEquipo(equipo: Equipo) {
        equipoDao.insertarEquipo(equipo)
    }

    // --- Operaciones de Órdenes de Trabajo ---
    val obtenerOrdenes: Flow<List<OrdenTrabajo>> = ordenTrabajoDao.obtenerOrdenes()

    suspend fun guardarOrden(orden: OrdenTrabajo) {
        ordenTrabajoDao.insertarOrden(orden)
    }

    suspend fun cambiarEstadoOrden(ordenId: String, nuevoEstado: String) {
        ordenTrabajoDao.actualizarEstadoOrden(ordenId, nuevoEstado)
    }
}