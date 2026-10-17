package com.example.mcmovilcare.data.local

import androidx.room.*
import com.example.mcmovilcare.model.Equipo
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipoDao {
    @Query("SELECT * FROM equipo")
    fun obtenerEquipos(): Flow<List<Equipo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarEquipo(equipo: Equipo)
}