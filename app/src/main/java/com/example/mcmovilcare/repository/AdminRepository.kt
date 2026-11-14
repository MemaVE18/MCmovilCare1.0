package com.example.mcmovilcare.repository

import com.example.mcmovilcare.data.local.EquipoDao
import com.example.mcmovilcare.data.local.OrdenTrabajoDao
import com.example.mcmovilcare.data.local.UsuarioDao
import com.example.mcmovilcare.model.Equipo
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.model.Usuario
import kotlinx.coroutines.flow.Flow

class AdminRepository(
    private val usuarioDao: UsuarioDao,
    private val equipoDao: EquipoDao,
    private val ordenTrabajoDao: OrdenTrabajoDao
) {
    val obtenerTodosLosUsuarios: Flow<List<Usuario>> get() = usuarioDao.obtenerTodosLosUsuarios()

    fun obtenerUsuarioPorId(id: String): Flow<Usuario?> = usuarioDao.obtenerUsuarioPorId(id)

    suspend fun guardarUsuario(usuario: Usuario) {
        usuarioDao.insertarUsuario(usuario)
    }

    val obtenerTodosLosEquipos: Flow<List<Equipo>> = equipoDao.obtenerEquipos()

    val obtenerTodasLasOrdenes: Flow<List<OrdenTrabajo>> = ordenTrabajoDao.obtenerOrdenes()

    suspend fun gestionarEstadoGlobalOrden(ordenId: String, nuevoEstado: String) {
        ordenTrabajoDao.actualizarEstadoOrden(ordenId, nuevoEstado)
    }
}