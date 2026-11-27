package com.example.mcmovilcare.repository

import com.example.mcmovilcare.ApiService
import com.example.mcmovilcare.RetrofitClient
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
    private val ordenTrabajoDao: OrdenTrabajoDao,
    private val apiService: ApiService = RetrofitClient.apiService
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

    suspend fun obtenerOrdenPorId(ordenId: String): OrdenTrabajo? {
        return ordenTrabajoDao.obtenerOrdenPorId(ordenId)
    }

    suspend fun cambiarEstadoOrden(ordenId: String, nuevoEstado: String) {
        tecnico.actualizarDiagnosticoOEstado(ordenId, nuevoEstado)
        try {
            val orden = ordenTrabajoDao.obtenerOrdenPorId(ordenId)
            if (orden != null) {
                val requiereAut = if (nuevoEstado == "Aprobado" || nuevoEstado == "Rechazado") {
                    false
                } else {
                    orden.requiereAutorizacion
                }
                val ordenSincronizada = orden.copy(
                    estado = nuevoEstado,
                    requiereAutorizacion = requiereAut
                )
                ordenTrabajoDao.insertarOrden(ordenSincronizada)
                apiService.actualizarOrden(ordenId, ordenSincronizada)
            }
        } catch (_: Exception) {
            // Se conserva la actualización local en Room si la API no responde
        }
    }

    suspend fun actualizarOrdenTecnico(
        ordenId: String,
        nuevoEstado: String,
        nuevoDiagnostico: String,
        nuevoMonto: Double?,
        requiereAut: Boolean,
        nuevaEvidencia: String
    ): Result<OrdenTrabajo?> {
        return try {
            ordenTrabajoDao.actualizarOrdenTecnico(
                ordenId = ordenId,
                nuevoEstado = nuevoEstado,
                nuevoDiagnostico = nuevoDiagnostico,
                nuevoMonto = nuevoMonto,
                requiereAut = requiereAut,
                nuevaEvidencia = nuevaEvidencia
            )
            val actualizada = ordenTrabajoDao.obtenerOrdenPorId(ordenId)
            if (actualizada != null) {
                try {
                    apiService.actualizarOrden(ordenId, actualizada)
                } catch (_: Exception) {
                }
            }
            Result.success(actualizada)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registrarUsuario(usuario: Usuario): Result<Usuario> {
        val normalizado = usuario.copy(
            nombre = usuario.nombre.trim(),
            correo = usuario.correo.trim().lowercase()
        )
        return try {
            val creadoRemoto = apiService.crearUsuario(normalizado)
            admin.guardarUsuario(creadoRemoto)
            Result.success(creadoRemoto)
        } catch (e: Exception) {
            admin.guardarUsuario(normalizado)
            Result.failure(e)
        }
    }

    suspend fun registrarOrden(orden: OrdenTrabajo): Result<OrdenTrabajo> {
        return try {
            val creadaRemota = apiService.crearOrden(orden)
            cliente.crearSolicitudOrden(creadaRemota)
            Result.success(creadaRemota)
        } catch (e: Exception) {
            cliente.crearSolicitudOrden(orden)
            Result.failure(e)
        }
    }

    suspend fun sincronizarOrdenesDesdeApi(): Result<List<OrdenTrabajo>> {
        return try {
            val remotas = apiService.obtenerOrdenes()
            remotas.forEach { cliente.crearSolicitudOrden(it) }
            Result.success(remotas)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sincronizarUsuariosDesdeApi(): Result<List<Usuario>> {
        return try {
            val remotos = apiService.obtenerUsuarios()
            remotos.forEach { u ->
                admin.guardarUsuario(u.copy(correo = u.correo.trim().lowercase()))
            }
            Result.success(remotos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun autenticarUsuario(email: String, password: String): Usuario? {
        val correoNormalizado = email.trim().lowercase()
        if (correoNormalizado.isBlank() || password.isBlank()) return null
        sincronizarUsuariosDesdeApi()
        val encontrado = usuarioDao.obtenerPorEmail(correoNormalizado)
        return if (encontrado != null && encontrado.password == password) encontrado else null
    }
}