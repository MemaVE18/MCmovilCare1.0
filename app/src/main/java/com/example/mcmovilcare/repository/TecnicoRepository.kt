package com.example.mcmovilcare.repository

import com.example.mcmovilcare.data.local.EquipoDao
import com.example.mcmovilcare.data.local.OrdenTrabajoDao
import com.example.mcmovilcare.model.Equipo
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.network.DiagnosticoRagRequest
import com.example.mcmovilcare.network.DiagnosticoRagResponse
import com.example.mcmovilcare.network.NuevoCasoRagRequest
import com.example.mcmovilcare.network.RagRetrofitClient
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

    suspend fun consultarAsistenteRag(
        categoria: String,
        marca: String,
        modelo: String,
        ean: String?,
        sintoma: String
    ): Result<DiagnosticoRagResponse> = runCatching {
        RagRetrofitClient.api.obtenerDiagnostico(
            DiagnosticoRagRequest(
                categoria = categoria.uppercase(),
                marca = marca.trim(),
                modelo = modelo.trim(),
                ean = ean?.takeIf { it.isNotBlank() },
                sintoma = sintoma.trim()
            )
        )
    }

    suspend fun indexarOrdenCerradaEnRag(
        ordenCodigo: String,
        categoria: String,
        marca: String,
        modelo: String,
        ean: String?,
        sintoma: String,
        diagnostico: String,
        solucionAplicada: String,
        esDevolucion: Boolean,
        skus: List<String>
    ): Result<Unit> = runCatching {
        RagRetrofitClient.api.registrarCasoCerrado(
            NuevoCasoRagRequest(
                ordenCodigo = ordenCodigo,
                categoria = categoria.uppercase(),
                marca = marca,
                modelo = modelo,
                ean = ean,
                sintoma = sintoma,
                diagnostico = diagnostico,
                solucionAplicada = solucionAplicada,
                estadoFinal = if (esDevolucion) "DEVUELTO_SIN_REPARACION" else "REPARADO",
                repuestosSku = skus
            )
        )
    }
}