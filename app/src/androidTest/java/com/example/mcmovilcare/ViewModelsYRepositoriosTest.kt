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
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsYRepositoriosTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeUsuarioDao: FakeUsuarioDao
    private lateinit var fakeEquipoDao: FakeEquipoDao
    private lateinit var fakeOrdenDao: FakeOrdenTrabajoDao
    private lateinit var fakeApiService: FakeApiService
    private lateinit var repository: AppRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeUsuarioDao = FakeUsuarioDao()
        fakeEquipoDao = FakeEquipoDao()
        fakeOrdenDao = FakeOrdenTrabajoDao()
        fakeApiService = FakeApiService()
        RetrofitClient.setApiServiceForTesting(fakeApiService)
        repository = AppRepository(fakeUsuarioDao, fakeEquipoDao, fakeOrdenDao, fakeApiService)
    }

    @After
    fun tearDown() {
        RetrofitClient.setApiServiceForTesting(null)
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

        val vm = OrdenTrabajoViewModel(repository, fakeApiService)
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
        assertEquals("admin_gestion_usuarios_screen", Screen.AdminGestionUsuarios.route)
        assertEquals("tecnico_dashboard_screen", Screen.TecnicoDashboard.route)
        assertEquals("tecnico_detalle_orden_screen/42", Screen.TecnicoDetalleOrden.createRoute(42))
    }

    @Test
    fun prioridad1_equipoViewModel_no_guarda_con_espacios_en_blanco_o_campos_incompletos() = runTest {
        val vm = EquipoViewModel(repository)

        vm.onMarcaChanged("   ")
        vm.onModeloChanged("Galaxy S24")
        vm.onSerieChanged("SN-999")
        assertFalse(vm.uiState.value.esValido)
        assertEquals("La marca es requerida", vm.uiState.value.errorMarca)

        vm.guardarEquipo("cli_1")
        assertFalse(vm.uiState.value.guardadoExitoso)
        assertTrue(repository.obtenerEquipos.first().isEmpty())
    }

    @Test
    fun prioridad2_registro_actualizacion_y_autenticacion_de_usuarios() = runTest {
        val nuevo = Usuario("u10", "  Luis Gomez  ", "  LUIS@DUOC.CL ", Rol.TECNICO, "clave99")
        val resultado = repository.registrarUsuario(nuevo)

        assertTrue(resultado.isSuccess)
        assertEquals(1, fakeApiService.usuariosCreados.size)
        assertEquals("luis@duoc.cl", fakeApiService.usuariosCreados.first().correo)

        val perfilCliente = repository.cliente.obtenerPerfilCliente("u10").first()
        assertEquals("Luis Gomez", perfilCliente?.nombre)

        val actualizado = nuevo.copy(nombre = "Luis Gomez Actualizado", correo = "luis@duoc.cl", rol = Rol.ADMIN)
        repository.guardarUsuario(actualizado)
        assertEquals(Rol.ADMIN, repository.obtenerUsuario("u10").first()?.rol)

        assertNotNull(repository.autenticarUsuario("LUIS@duoc.cl", "clave99"))
        assertNull(repository.autenticarUsuario("luis@duoc.cl", "clave_erronea"))
        assertNull(repository.autenticarUsuario("", "clave99"))
    }

    @Test
    fun prioridad3_creacion_y_almacenamiento_de_multiples_equipos() = runTest {
        val eq1 = Equipo("eq1", "cli_1", "Apple", "iPhone 15", "SN-A1", "Caja")
        val eq2 = Equipo("eq2", "cli_2", "Lenovo", "ThinkPad X1", "SN-B2", "Cargador")

        repository.cliente.registrarEquipo(eq1)
        repository.guardarEquipo(eq2)

        val equiposAdmin = repository.admin.obtenerTodosLosEquipos.first()
        val equiposTecnico = repository.tecnico.obtenerEquiposEnTaller.first()

        assertEquals(2, equiposAdmin.size)
        assertEquals(2, equiposTecnico.size)
        assertEquals("ThinkPad X1", equiposAdmin.last().modelo)
    }

    @Test
    fun prioridad4_creacion_y_actualizacion_completa_de_ordenes() = runTest {
        val orden = OrdenTrabajo(
            id = "ord-100",
            equipoModelo = "Samsung Galaxy S22",
            numeroSerie = "SN-220",
            estado = "Solicitud ingresada",
            fechaIngreso = "20-11-2026"
        )

        val resCreacion = repository.registrarOrden(orden)
        assertTrue(resCreacion.isSuccess)
        assertEquals(1, fakeApiService.ordenesCreadas.size)

        val resAct = repository.actualizarOrdenTecnico(
            ordenId = "ord-100",
            nuevoEstado = "Pend. autorizacion",
            nuevoDiagnostico = "Cambio de pin de carga",
            nuevoMonto = 35000.0,
            requiereAut = true,
            nuevaEvidencia = "Foto microscopio pin sulfatado"
        )
        assertTrue(resAct.isSuccess)

        val guardada = repository.obtenerOrdenPorId("ord-100")
        assertNotNull(guardada)
        assertEquals("Pend. autorizacion", guardada?.estado)
        assertEquals("Cambio de pin de carga", guardada?.diagnostico)
        assertEquals(35000.0, guardada?.cotizacionMonto)
        assertTrue(guardada?.requiereAutorizacion == true)
        assertEquals("Foto microscopio pin sulfatado", guardada?.evidencia)
        assertEquals(1, fakeApiService.ordenesActualizadas.size)
    }

    @Test
    fun prioridad5_aprobacion_y_rechazo_limpian_requiereAutorizacion_y_sincronizan_api() = runTest {
        val ordenCotizada = OrdenTrabajo(
            id = "ord-55",
            equipoModelo = "MacBook Air M2",
            numeroSerie = "SN-M2",
            estado = "Pend. autorizacion",
            diagnostico = "Teclado dañado",
            cotizacionMonto = 120000.0,
            requiereAutorizacion = true,
            fechaIngreso = "21-11-2026"
        )
        repository.guardarOrden(ordenCotizada)

        val vm = OrdenTrabajoViewModel(repository, fakeApiService)
        vm.aprobarOrden("ord-55")

        val postAprobacion = repository.obtenerOrdenPorId("ord-55")
        assertEquals("Aprobado", postAprobacion?.estado)
        assertFalse(postAprobacion?.requiereAutorizacion ?: true)
        assertEquals("Aprobado", fakeApiService.ordenesActualizadas.last().second.estado)

        vm.rechazarOrden("ord-55")
        val postRechazo = repository.obtenerOrdenPorId("ord-55")
        assertEquals("Rechazado", postRechazo?.estado)
        assertFalse(postRechazo?.requiereAutorizacion ?: true)
    }

    @Test
    fun prioridad6_gestion_de_estados_por_tecnico_y_administrador() = runTest {
        val orden = OrdenTrabajo("ord-77", "iPhone 13", "SN-77", "Solicitud ingresada", fechaIngreso = "22-11-2026")
        repository.guardarOrden(orden)

        repository.tecnico.actualizarDiagnosticoOEstado("ord-77", "En diagnostico")
        assertEquals("En diagnostico", repository.tecnico.obtenerOrdenesAsignadas.first().first().estado)

        repository.admin.gestionarEstadoGlobalOrden("ord-77", "Listo")
        assertEquals("Listo", repository.admin.obtenerTodasLasOrdenes.first().first().estado)

        repository.cambiarEstadoOrden("ord-77", "Entregado")
        assertEquals("Entregado", repository.obtenerOrdenPorId("ord-77")?.estado)
    }

    @Test
    fun prioridad7_manejo_de_errores_http_y_red_en_api() = runTest {
        val ordenLocal = OrdenTrabajo("loc-1", "HP Envy", "SN-HP1", "En diagnostico", fechaIngreso = "23-11-2026")
        repository.guardarOrden(ordenLocal)

        val errorBody = ResponseBody.create(MediaType.parse("application/json"), "{\"error\":\"Server Error\"}")
        fakeApiService.excepcionALanzar = HttpException(Response.error<Any>(500, errorBody))

        val vm = OrdenTrabajoViewModel(repository, fakeApiService)
        vm.cargarEquiposDesdeApi()

        assertFalse(vm.isLoading.value)
        assertNotNull(vm.errorMensaje.value)
        assertEquals(1, repository.obtenerOrdenes.first().size)
        assertEquals("HP Envy", repository.obtenerOrdenPorId("loc-1")?.equipoModelo)

        vm.aprobarOrden("loc-1")
        assertEquals("Aprobado", repository.obtenerOrdenPorId("loc-1")?.estado)
        assertNotNull(vm.errorMensaje.value)

        val resUser = repository.registrarUsuario(Usuario("u-off", "Offline", "off@duoc.cl", Rol.CLIENTE))
        assertTrue(resUser.isFailure)
        assertEquals("Offline", repository.obtenerUsuario("u-off").first()?.nombre)
    }

    @Test
    fun prioridad8_respuestas_vacias_y_datos_inexistentes() = runTest {
        fakeApiService.ordenesRemotas = emptyList()
        fakeApiService.usuariosRemotos = emptyList()

        val vm = OrdenTrabajoViewModel(repository, fakeApiService)
        vm.cargarEquiposDesdeApi()

        assertNull(vm.errorMensaje.value)
        assertEquals(0, vm.ultimaSincronizacionCount.value)
        assertTrue(repository.obtenerOrdenes.first().isEmpty())

        assertNull(repository.obtenerOrdenPorId("ID_QUE_NO_EXISTE"))
        assertNull(repository.obtenerUsuario("USER_INEXISTENTE").first())
        assertNull(repository.autenticarUsuario("noexiste@duoc.cl", "1234"))
    }

    @Test
    fun prioridad9_errores_de_persistencia_en_dao() = runTest {
        fakeEquipoDao.lanzarErrorEnInsert = true
        val vm = EquipoViewModel(repository)

        vm.onMarcaChanged("Dell")
        vm.onModeloChanged("XPS 13")
        vm.onSerieChanged("SN-DELL-1")
        vm.guardarEquipo("cli_1")

        assertFalse(vm.uiState.value.guardadoExitoso)
        assertNotNull(vm.errorPersistencia.value)

        fakeOrdenDao.lanzarErrorEnUpdate = true
        val resOrden = repository.actualizarOrdenTecnico(
            ordenId = "1",
            nuevoEstado = "Listo",
            nuevoDiagnostico = "OK",
            nuevoMonto = 10000.0,
            requiereAut = false,
            nuevaEvidencia = "OK"
        )
        assertTrue(resOrden.isFailure)
    }

    @Test
    fun prioridad10_sincronizacion_local_y_remota_completa() = runTest {
        fakeApiService.ordenesRemotas = listOf(
            OrdenTrabajo("rem-1", "iPad Pro", "SN-IPAD", "En diagnostico", fechaIngreso = "24-11-2026"),
            OrdenTrabajo("rem-2", "Galaxy Tab S9", "SN-TAB", "Listo", "Cambio pantalla", 95000.0, false, "25-11-2026")
        )
        fakeApiService.usuariosRemotos = listOf(
            Usuario("u-rem-1", "Camila", "  CAMILA@DUOC.CL ", Rol.ADMIN, "admin2026")
        )

        val vm = OrdenTrabajoViewModel(repository, fakeApiService)
        vm.cargarEquiposDesdeApi()

        assertEquals(2, vm.ultimaSincronizacionCount.value)
        assertEquals(2, repository.obtenerOrdenes.first().size)
        assertEquals("iPad Pro", repository.obtenerOrdenPorId("rem-1")?.equipoModelo)

        val syncUsuarios = repository.sincronizarUsuariosDesdeApi()
        assertTrue(syncUsuarios.isSuccess)
        val autenticado = repository.autenticarUsuario("camila@duoc.cl", "admin2026")
        assertNotNull(autenticado)
        assertEquals(Rol.ADMIN, autenticado?.rol)
    }

    private class FakeApiService : ApiService {
        var ordenesRemotas: List<OrdenTrabajo> = emptyList()
        var usuariosRemotos: List<Usuario> = emptyList()
        var excepcionALanzar: Exception? = null

        val ordenesCreadas = mutableListOf<OrdenTrabajo>()
        val ordenesActualizadas = mutableListOf<Pair<String, OrdenTrabajo>>()
        val usuariosCreados = mutableListOf<Usuario>()

        override suspend fun obtenerOrdenes(): List<OrdenTrabajo> {
            excepcionALanzar?.let { throw it }
            return ordenesRemotas
        }

        override suspend fun crearOrden(orden: OrdenTrabajo): OrdenTrabajo {
            excepcionALanzar?.let { throw it }
            ordenesCreadas.add(orden)
            return orden
        }

        override suspend fun actualizarOrden(id: String, orden: OrdenTrabajo): OrdenTrabajo {
            excepcionALanzar?.let { throw it }
            ordenesActualizadas.add(id to orden)
            return orden
        }

        override suspend fun obtenerUsuarios(): List<Usuario> {
            excepcionALanzar?.let { throw it }
            return usuariosRemotos
        }

        override suspend fun crearUsuario(usuario: Usuario): Usuario {
            excepcionALanzar?.let { throw it }
            usuariosCreados.add(usuario)
            return usuario
        }
    }

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
        var lanzarErrorEnInsert = false

        override fun obtenerEquipos(): Flow<List<Equipo>> = lista
        override suspend fun insertarEquipo(equipo: Equipo) {
            if (lanzarErrorEnInsert) throw IOException("Error simulado de escritura en tabla equipo")
            lista.value = lista.value.filterNot { it.id == equipo.id } + equipo
        }
    }

    private class FakeOrdenTrabajoDao : OrdenTrabajoDao {
        private val lista = MutableStateFlow<List<OrdenTrabajo>>(emptyList())
        var lanzarErrorEnUpdate = false

        override fun obtenerOrdenes(): Flow<List<OrdenTrabajo>> = lista
        override suspend fun obtenerOrdenPorId(ordenId: String): OrdenTrabajo? =
            lista.value.find { it.id == ordenId }
        override suspend fun insertarOrden(orden: OrdenTrabajo) {
            lista.value = lista.value.filterNot { it.id == orden.id } + orden
        }
        override suspend fun actualizarEstadoOrden(ordenId: String, nuevoEstado: String) {
            if (lanzarErrorEnUpdate) throw IOException("Error simulado de actualización en Room")
            lista.value = lista.value.map { if (it.id == ordenId) it.copy(estado = nuevoEstado) else it }
        }
        override suspend fun actualizarOrdenTecnico(
            ordenId: String, nuevoEstado: String, nuevoDiagnostico: String,
            nuevoMonto: Double?, requiereAut: Boolean, nuevaEvidencia: String
        ) {
            if (lanzarErrorEnUpdate) throw IOException("Error simulado de actualización técnica en Room")
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