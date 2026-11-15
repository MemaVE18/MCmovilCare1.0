package com.example.mcmovilcare

import com.example.mcmovilcare.data.local.EquipoDao
import com.example.mcmovilcare.data.local.OrdenTrabajoDao
import com.example.mcmovilcare.data.local.UsuarioDao
import com.example.mcmovilcare.model.Equipo
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.model.Rol
import com.example.mcmovilcare.model.Usuario
import com.example.mcmovilcare.navigation.Screen
import com.example.mcmovilcare.repository.AppRepository
import com.example.mcmovilcare.viewmodel.EquipoViewModel
import com.example.mcmovilcare.viewmodel.OrdenTrabajoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsYRepositoriosTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeUsuarioDao: FakeUsuarioDao
    private lateinit var fakeEquipoDao: FakeEquipoDao
    private lateinit var fakeOrdenDao: FakeOrdenTrabajoDao
    private lateinit var repository: AppRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeUsuarioDao = FakeUsuarioDao()
        fakeEquipoDao = FakeEquipoDao()
        fakeOrdenDao = FakeOrdenTrabajoDao()
        repository = AppRepository(fakeUsuarioDao, fakeEquipoDao, fakeOrdenDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun equipoViewModel_valida_campos_y_guarda_equipo() = runTest {
        val vm = EquipoViewModel(repository)

        vm.onMarcaChanged("")
        assertNotNull(vm.uiState.value.errorMarca)
        assertFalse(vm.uiState.value.esValido)

        vm.onMarcaChanged("Samsung")
        vm.onModeloChanged("")
        assertNotNull(vm.uiState.value.errorModelo)

        vm.onModeloChanged("S23")
        vm.onSerieChanged("")
        assertNotNull(vm.uiState.value.errorSerie)

        vm.onSerieChanged("SN-001")
        assertNull(vm.uiState.value.errorSerie)
        assertTrue(vm.uiState.value.esValido)

        vm.guardarEquipo("cli_1")
        assertTrue(vm.uiState.value.guardadoExitoso)
        assertEquals(1, repository.obtenerEquipos.first().size)
    }

    @Test
    fun ordenTrabajoViewModel_aprueba_y_rechaza_ordenes() = runTest {
        val orden = OrdenTrabajo("1", "MacBook Pro", "SN1", "Pend. autorizacion", fechaIngreso = "15-11-2026")
        repository.guardarOrden(orden)

        val vm = OrdenTrabajoViewModel(repository)
        vm.aprobarOrden("1")
        assertEquals("Aprobado", fakeOrdenDao.obtenerOrdenPorId("1")?.estado)

        vm.rechazarOrden("1")
        assertEquals("Rechazado", fakeOrdenDao.obtenerOrdenPorId("1")?.estado)
    }

    @Test
    fun appRepository_y_roles_gestionan_usuarios_y_ordenes() = runTest {
        val user = Usuario("u1", "Ana", "ana@duoc.cl", Rol.ADMIN, "1234")
        repository.guardarUsuario(user)
        assertEquals("Ana", repository.obtenerUsuario("u1").first()?.nombre)
        assertEquals(1, repository.obtenerUsuarios.first().size)

        val orden = OrdenTrabajo("o1", "iPhone", "SN9", "En diagnostico", fechaIngreso = "15-11-2026")
        repository.cliente.crearSolicitudOrden(orden)
        repository.admin.gestionarEstadoGlobalOrden("o1", "Listo")
        assertEquals("Listo", repository.obtenerOrdenes.first().first().estado)

        repository.cliente.aprobarORechazarPresupuesto("o1", "Aprobado")
        assertEquals("Aprobado", fakeOrdenDao.obtenerOrdenPorId("o1")?.estado)

        repository.tecnico.guardarActualizacionOrden(orden.copy(estado = "Entregado"))
        assertEquals("Entregado", fakeOrdenDao.obtenerOrdenPorId("o1")?.estado)
    }

    @Test
    fun screen_rutas_son_correctas() {
        assertEquals("admin_dashboard_screen", Screen.AdminDashboard.route)
        assertEquals("tecnico_detalle_orden_screen/42", Screen.TecnicoDetalleOrden.createRoute(42))
    }

    // --- DAOs falsos en memoria ---
    private class FakeUsuarioDao : UsuarioDao {
        private val lista = MutableStateFlow<List<Usuario>>(emptyList())
        override fun obtenerUsuarioPorId(id: String): Flow<Usuario?> =
            lista.map { it.find { u -> u.id == id } }
        override suspend fun obtenerPorEmail(correo: String): Usuario? =
            lista.value.find { it.correo == correo }
        override suspend fun insertarUsuario(usuario: Usuario) {
            lista.value = lista.value.filterNot { it.id == usuario.id } + usuario
        }
        override fun obtenerTodosLosUsuarios(): Flow<List<Usuario>> = lista
    }

    private class FakeEquipoDao : EquipoDao {
        private val lista = MutableStateFlow<List<Equipo>>(emptyList())
        override fun obtenerEquipos(): Flow<List<Equipo>> = lista
        override suspend fun insertarEquipo(equipo: Equipo) {
            lista.value = lista.value.filterNot { it.id == equipo.id } + equipo
        }
    }

    private class FakeOrdenTrabajoDao : OrdenTrabajoDao {
        private val lista = MutableStateFlow<List<OrdenTrabajo>>(emptyList())
        override fun obtenerOrdenes(): Flow<List<OrdenTrabajo>> = lista
        override suspend fun obtenerOrdenPorId(ordenId: String): OrdenTrabajo? =
            lista.value.find { it.id == ordenId }
        override suspend fun insertarOrden(orden: OrdenTrabajo) {
            lista.value = lista.value.filterNot { it.id == orden.id } + orden
        }
        override suspend fun actualizarEstadoOrden(ordenId: String, nuevoEstado: String) {
            lista.value = lista.value.map { if (it.id == ordenId) it.copy(estado = nuevoEstado) else it }
        }
        override suspend fun actualizarOrdenTecnico(
            ordenId: String, nuevoEstado: String, nuevoDiagnostico: String,
            nuevoMonto: Double?, requiereAut: Boolean, nuevaEvidencia: String
        ) {
            lista.value = lista.value.map {
                if (it.id == ordenId) it.copy(
                    estado = nuevoEstado, diagnostico = nuevoDiagnostico,
                    cotizacionMonto = nuevoMonto, requiereAutorizacion = requiereAut,
                    evidencia = nuevaEvidencia
                ) else it
            }
        }
    }
}