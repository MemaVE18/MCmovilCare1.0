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
    val cliente = ClienteRepository(usuarioDao, equipoDao, ordenTrabajoDao)
    val tecnico = TecnicoRepository(equipoDao, ordenTrabajoDao)
    val admin = AdminRepository(usuarioDao, equipoDao, ordenTrabajoDao)

    val obtenerUsuarios: Flow<List<Usuario>> get() = admin.obtenerTodosLosUsuarios

    fun obtenerUsuario(id: String): Flow<Usuario?> = admin.obtenerUsuarioPorId(id)

    suspend fun guardarUsuario(usuario: Usuario) {
        admin.guardarUsuario(usuario)
    }

    val obtenerEquipos: Flow<List<Equipo>> = cliente.obtenerMisEquipos

    suspend fun guardarEquipo(equipo: Equipo) {
        cliente.registrarEquipo(equipo)
    }

    val obtenerOrdenes: Flow<List<OrdenTrabajo>> = admin.obtenerTodasLasOrdenes

    suspend fun guardarOrden(orden: OrdenTrabajo) {
        cliente.crearSolicitudOrden(orden)
    }

    suspend fun cambiarEstadoOrden(ordenId: String, nuevoEstado: String) {
        tecnico.actualizarDiagnosticoOEstado(ordenId, nuevoEstado)
    }
}