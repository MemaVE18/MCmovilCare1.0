package com.example.mcmovilcare.data.local

import androidx.room.*
import com.example.mcmovilcare.model.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Query("SELECT * FROM usuario WHERE id = :id")
    fun obtenerUsuarioPorId(id: String): Flow<Usuario?>

    @Query("SELECT * FROM usuario WHERE correo = :correo LIMIT 1")
    suspend fun obtenerPorEmail(correo: String): Usuario?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarUsuario(usuario: Usuario)

    @Query("SELECT * FROM usuario")
    fun obtenerTodosLosUsuarios(): Flow<List<Usuario>>
}