package com.example.mcmovilcare.data.local

import androidx.room.*
import com.example.mcmovilcare.model.OrdenTrabajo
import kotlinx.coroutines.flow.Flow

@Dao
interface OrdenTrabajoDao {

    @Query("SELECT * FROM ordenes_trabajo")
    fun obtenerOrdenes(): Flow<List<OrdenTrabajo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOrden(orden: OrdenTrabajo)

    @Query("UPDATE ordenes_trabajo SET estado = :nuevoEstado WHERE id = :ordenId")
    suspend fun actualizarEstadoOrden(ordenId: String, nuevoEstado: String)
}