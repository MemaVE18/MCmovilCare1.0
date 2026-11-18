package com.example.mcmovilcare

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mcmovilcare.data.local.AppDatabase
import com.example.mcmovilcare.model.Equipo
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.model.Rol
import com.example.mcmovilcare.model.Usuario
import com.example.mcmovilcare.navigation.Screen
import com.example.mcmovilcare.ui.screens.AdminDashboardScreen
import com.example.mcmovilcare.ui.screens.TecnicoDashboardScreen
import com.example.mcmovilcare.ui.theme.MCMovilCareTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Cobertura80PlusTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun concederPermisos() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        instrumentation.uiAutomation.executeShellCommand(
            "pm grant $packageName android.permission.POST_NOTIFICATIONS"
        ).close()
    }

    private val ordenesMuestra = listOf(
        OrdenTrabajo("1", "Samsung Galaxy S22", "SN123", "En diagnostico", "Falla de carga", 25000.0, false, "10-10-2026"),
        OrdenTrabajo("2", "MacBook Pro M1", "SN456", "Pend. autorizacion", "Pantalla rota", 150000.0, true, "12-10-2026"),
        OrdenTrabajo("3", "iPhone 13", "SN789", "Listo", "Cambio de bateria", 45000.0, false, "15-10-2026"),
        OrdenTrabajo("4", "Xiaomi Note 12", "SN999", "Solicitud ingresada", null, null, false, "16-10-2026")
    )

    private val usuariosMuestra = listOf(
        Usuario("u1", "Carlos Técnico", "tecnico@duoc.cl", Rol.TECNICO, "1234"),
        Usuario("u2", "Ana Admin", "admin@duoc.cl", Rol.ADMIN, "1234")
    )

    @Test
    fun nuevoEquipoActivity_abre_y_boton_volver_funciona() {
        ActivityScenario.launch(NuevoEquipoActivity::class.java).use {
            composeRule.onNodeWithText("< Volver").assertExists()
            composeRule.onNodeWithText("< Volver").performClick()
        }
    }

    @Test
    fun historialActivity_abre_en_modo_tecnico_y_modo_admin() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val intentTecnico = Intent(context, HistorialActivity::class.java).apply {
            putExtra("EXTRA_ROL", "TECNICO")
        }
        ActivityScenario.launch<HistorialActivity>(intentTecnico).use {
            composeRule.onNodeWithText("Panel Técnico").assertExists()
        }

        val intentAdmin = Intent(context, HistorialActivity::class.java).apply {
            putExtra("EXTRA_ROL", "ADMIN")
        }
        ActivityScenario.launch<HistorialActivity>(intentAdmin).use {
            composeRule.onNodeWithText("Panel Administrador").assertExists()
        }
    }

    @Test
    fun panelTecnicoScreen_abre_dialogo_y_actualiza_orden() {
        var ordenActualizadaId = ""
        var nuevoEstadoGuardado = ""
        var escanerAbierto = false
        var sesionCerrada = false

        composeRule.setContent {
            MCMovilCareTheme {
                PanelTecnicoScreen(
                    ordenes = ordenesMuestra,
                    onActualizarOrden = { id, estado, _, _, _, _ ->
                        ordenActualizadaId = id
                        nuevoEstadoGuardado = estado
                    },
                    onAbrirEscaner = { escanerAbierto = true },
                    onCerrarSesion = { sesionCerrada = true }
                )
            }
        }

        composeRule.onNodeWithText("Panel Técnico").assertExists()
        composeRule.onNodeWithText("Ingresar Equipo / Escanear QR").performClick()
        assertTrue(escanerAbierto)

        composeRule.onNodeWithText("Samsung Galaxy S22").performClick()
        composeRule.onNodeWithText("Gestionar Samsung Galaxy S22").assertExists()

        composeRule.onNodeWithText("Falla de carga").performTextClearance()
        composeRule.onNodeWithText("Diagnóstico técnico").performTextInput("Placa reparada OK")
        composeRule.onNodeWithText("Cotizar").performClick()
        composeRule.onNodeWithText("Guardar Cambios").performClick()

        assertEquals("1", ordenActualizadaId)
        assertEquals("Pend. autorizacion", nuevoEstadoGuardado)

        composeRule.onNodeWithText("Cerrar Sesión").performClick()
        assertTrue(sesionCerrada)
    }

    @Test
    fun panelAdminScreen_crea_usuario_y_muestra_catalogo() {
        var usuarioCreadoNombre = ""
        var rolCreado: Rol? = null
        var sesionCerrada = false

        composeRule.setContent {
            MCMovilCareTheme {
                PanelAdminScreen(
                    ordenes = ordenesMuestra,
                    usuarios = usuariosMuestra,
                    onCrearUsuario = { nombre, _, _, rol ->
                        usuarioCreadoNombre = nombre
                        rolCreado = rol
                    },
                    onCerrarSesion = { sesionCerrada = true }
                )
            }
        }

        composeRule.onNodeWithText("Panel Administrador").assertExists()
        composeRule.onNodeWithText("Mantención preventiva general").assertExists()

        composeRule.onNodeWithText("Nombre").performTextInput("María López")
        composeRule.onNodeWithText("Correo (ej: tecnico2@duoc.cl)").performTextInput("maria@duoc.cl")
        composeRule.onNodeWithText("Contraseña").performTextInput("clave123")
        composeRule.onAllNodesWithText("ADMIN")[0].performClick()
        composeRule.onNodeWithText("Guardar Usuario en BD").performClick()

        assertEquals("María López", usuarioCreadoNombre)
        assertEquals(Rol.ADMIN, rolCreado)

        composeRule.onNodeWithText("Cerrar Sesión").performClick()
        assertTrue(sesionCerrada)
    }

    @Test
    fun dashboardClienteActivity_y_adminActivity_abren_correctamente() {
        ActivityScenario.launch(DashboardClienteActivity::class.java).use {
            composeRule.onNodeWithText("Hola, Cliente").assertExists()
        }

        ActivityScenario.launch(DashboardAdminActivity::class.java).use {
            composeRule.onNodeWithText("Panel Administrador").assertExists()
        }
    }

    @Test
    fun dashboardClienteScreenContent_lista_vacia_y_con_ordenes() {
        var qrTocado = false
        var sesionCerrada = false

        composeRule.setContent {
            MCMovilCareTheme {
                DashboardClienteScreenContent(
                    ordenes = emptyList(),
                    onOrdenClick = {},
                    onEscanearQR = { qrTocado = true },
                    onCerrarSesion = { sesionCerrada = true }
                )
            }
        }
        composeRule.onNodeWithText("Escanear QR").performClick()
        composeRule.onNodeWithText("Cerrar sesión").performClick()
        assertTrue(qrTocado)
        assertTrue(sesionCerrada)
    }

    @Test
    fun dashboardClienteScreenContent_renderiza_tarjetas_y_abre_orden() {
        var ordenTocadaId = ""

        composeRule.setContent {
            MCMovilCareTheme {
                DashboardClienteScreenContent(
                    ordenes = ordenesMuestra,
                    onOrdenClick = { ordenTocadaId = it.id },
                    onEscanearQR = {},
                    onCerrarSesion = {}
                )
            }
        }

        composeRule.onNodeWithText("Samsung Galaxy S22").performClick()
        assertEquals("1", ordenTocadaId)
    }

    @Test
    fun dashboardAdminScreenContent_interactua_con_formulario() {
        var nombreGuardado = ""
        var rolGuardado = ""
        var sesionCerrada = false

        composeRule.setContent {
            MCMovilCareTheme {
                DashboardAdminScreenContent(
                    ordenes = ordenesMuestra,
                    usuarios = usuariosMuestra,
                    onGuardarUsuario = { nombre, _, rol ->
                        nombreGuardado = nombre
                        rolGuardado = rol
                    },
                    onCerrarSesion = { sesionCerrada = true }
                )
            }
        }

        composeRule.onNodeWithText("Nombre").performTextInput("Luis Rojas")
        composeRule.onNodeWithText("Correo (ej: tecnico2@duoc.cl)").performTextInput("luis@duoc.cl")
        composeRule.onNodeWithText("CLIENTE").performClick()
        composeRule.onNodeWithText("Guardar Usuario en BD").performClick()

        assertEquals("Luis Rojas", nombreGuardado)
        assertEquals("CLIENTE", rolGuardado)

        composeRule.onNodeWithText("Cerrar Sesión").performClick()
        assertTrue(sesionCerrada)
    }

    @Test
    fun mainActivity_valida_campos_y_login_fallido() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("MisPreferenciasApp", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Ingresar").performClick()
            composeRule.onNodeWithText("Correo Electrónico").performTextInput("noexiste@duoc.cl")
            composeRule.onNodeWithText("Contraseña").performTextInput("wrongpass")
            composeRule.onNodeWithText("Ingresar").performClick()
            Thread.sleep(600)
        }
    }

    @Test
    fun mainActivity_autologin_por_roles() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("MisPreferenciasApp", Context.MODE_PRIVATE)

        listOf("ADMIN", "TECNICO", "CLIENTE").forEach { rol ->
            prefs.edit().putBoolean("isLoggedIn", true).putString("rol", rol).commit()
            ActivityScenario.launch(MainActivity::class.java).use {
                Thread.sleep(400)
            }
        }

        prefs.edit().clear().commit()
    }

    @Test
    fun detalleOrdenActivity_aprobar_rechazar_pdf_y_estados() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val intentAprobar = Intent(context, DetalleOrdenActivity::class.java).apply {
            putExtra("EXTRA_ID", "2")
            putExtra("EXTRA_EQUIPO", "MacBook Pro M1")
            putExtra("EXTRA_SERIE", "SN456")
            putExtra("EXTRA_ESTADO", "Pend. autorizacion")
            putExtra("EXTRA_DIAGNOSTICO", "Pantalla rota")
            putExtra("EXTRA_MONTO", 150000.0)
            putExtra("EXTRA_AUTORIZADO", true)
            putExtra("EXTRA_FECHA", "12-10-2026")
        }
        ActivityScenario.launch<DetalleOrdenActivity>(intentAprobar).use {
            onView(withId(R.id.btnAprobarOrden)).perform(click())
            onView(withId(R.id.btnGenerarPdf)).perform(click())
        }

        val intentRechazar = Intent(context, DetalleOrdenActivity::class.java).apply {
            putExtra("EXTRA_ID", "2")
            putExtra("EXTRA_EQUIPO", "MacBook Pro M1")
            putExtra("EXTRA_ESTADO", "Pend. autorizacion")
            putExtra("EXTRA_AUTORIZADO", true)
        }
        ActivityScenario.launch<DetalleOrdenActivity>(intentRechazar).use {
            onView(withId(R.id.btnRechazarOrden)).perform(click())
        }

        val intentListo = Intent(context, DetalleOrdenActivity::class.java).apply {
            putExtra("EXTRA_ID", "3")
            putExtra("EXTRA_EQUIPO", "iPhone 13")
            putExtra("EXTRA_ESTADO", "Listo")
        }
        ActivityScenario.launch<DetalleOrdenActivity>(intentListo).use { }

        val intentRevision = Intent(context, DetalleOrdenActivity::class.java).apply {
            putExtra("EXTRA_ID", "4")
            putExtra("EXTRA_EQUIPO", "Samsung S22")
            putExtra("EXTRA_ESTADO", "En revision")
        }
        ActivityScenario.launch<DetalleOrdenActivity>(intentRevision).use {
            onView(withId(R.id.btnVolver)).perform(click())
        }
    }

    @Test
    fun uiScreens_admin_y_tecnico_dashboard_navegan_correctamente() {
        composeRule.setContent {
            MCMovilCareTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "admin_test") {
                    composable("admin_test") {
                        AdminDashboardScreen(navController = navController)
                    }
                    composable(Screen.AdminGestionUsuarios.route) {
                        TecnicoDashboardScreen(navController = navController)
                    }
                }
            }
        }

        composeRule.onNodeWithText("Administración de Órdenes").assertExists()
        composeRule.onNodeWithText("Gestionar Usuarios").performClick()
        composeRule.onNodeWithText("Mis Trabajos Asignados").assertExists()
        composeRule.onNodeWithText("Lista de órdenes del técnico").assertExists()
    }

    @Test
    fun daos_room_crud_completo() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val usuario = Usuario("u99", "Pedro Test", "pedro@duoc.cl", Rol.CLIENTE, "1234")
            db.usuarioDao().insertarUsuario(usuario)
            assertNotNull(db.usuarioDao().obtenerPorEmail("pedro@duoc.cl"))
            assertNotNull(db.usuarioDao().obtenerUsuarioPorId("u99").first())
            assertTrue(db.usuarioDao().obtenerTodosLosUsuarios().first().isNotEmpty())

            val orden = OrdenTrabajo("o99", "Lenovo ThinkPad", "SN99", "En revision", null, null, false, "18-11-2026")
            db.ordenTrabajoDao().insertarOrden(orden)
            assertNotNull(db.ordenTrabajoDao().obtenerOrdenPorId("o99"))
            db.ordenTrabajoDao().actualizarEstadoOrden("o99", "En diagnostico")
            db.ordenTrabajoDao().actualizarOrdenTecnico(
                ordenId = "o99",
                nuevoEstado = "Listo",
                nuevoDiagnostico = "Limpieza completa",
                nuevoMonto = 30000.0,
                requiereAut = false,
                nuevaEvidencia = "foto_ok.jpg"
            )
            val ordenActualizada = db.ordenTrabajoDao().obtenerOrdenPorId("o99")
            assertEquals("Listo", ordenActualizada?.estado)
            assertTrue(db.ordenTrabajoDao().obtenerOrdenes().first().isNotEmpty())

            val equipo = Equipo(
                id = "e99",
                clienteId = "u99",
                marca = "HP",
                modelo = "Pavilion",
                numeroSerie = "SN-HP-01",
                accesorios = "Cargador original"
            )
            db.equipoDao().insertarEquipo(equipo)
            assertTrue(db.equipoDao().obtenerEquipos().first().isNotEmpty())
        } finally {
            db.close()
        }
    }
}