package com.example.mcmovilcare.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.mcmovilcare.model.Equipo
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.model.Usuario

@Database(
    entities = [Usuario::class, Equipo::class, OrdenTrabajo::class],
    version = 2, // es la version 2 de la app, me dio paja desintalar
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun equipoDao(): EquipoDao
    abstract fun ordenTrabajoDao(): OrdenTrabajoDao
}