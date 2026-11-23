package com.example.mcmovilcare

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.performTextClearance
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.not
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ActivityScenario
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.model.Rol
import com.example.mcmovilcare.model.Usuario
import com.example.mcmovilcare.navigation.Screen
import com.example.mcmovilcare.ui.components.SeccionBotonesAprobacion
import com.example.mcmovilcare.ui.screens.AdminDashboardScreen
import com.example.mcmovilcare.ui.screens.HistorialScreen
import com.example.mcmovilcare.ui.screens.NuevoEquipoScreen
import com.example.mcmovilcare.ui.screens.ScanQrScreen
import com.example.mcmovilcare.ui.screens.TecnicoDashboardScreen
import com.example.mcmovilcare.ui.theme.MCMovilCareTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UiFlujoUsuarioTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val ordenesPrueba = listOf(
        OrdenTrabajo("1", "Samsung Galaxy S22", "SN123", "En diagnostico", null, null, false, "10-10-2026"),
        OrdenTrabajo("2", "MacBook Pro M1", "SN456", "Pend. autorizacion", "Pantalla rota", 150000.0, true, "12-10-2026")
    )

    @Test
    fun cliente_ve_ordenes_y_pincha_una_orden() {
        var ordenClickeada: OrdenTrabajo? = null
        var sesionCerrada = false

        composeRule.setContent {
            MCMovilCareTheme {
                DashboardClienteScreenContent(
                    ordenes = ordenesPrueba,
                    onOrdenClick = { ordenClickeada = it },
                    onEscanearQR = {},
                    onCerrarSesion = { sesionCerrada = true }
                )
            }
        }

        composeRule.onNodeWithText("MacBook Pro M1").assertExists().performClick()
        assertEquals("2", ordenClickeada?.id)

        composeRule.onNodeWithText("Cerrar sesión").performClick()
        assertTrue(sesionCerrada)
    }

    @Test
    fun nuevoEquipo_valida_campos_y_guarda_al_pinchar() {
        var serieGuardada = ""
        var marcaGuardada = ""
        var modeloGuardado = ""

        composeRule.setContent {
            MCMovilCareTheme {
                NuevoEquipoScreen(
                    onAbrirEscaner = {},
                    onGuardarEquipo = { serie, marca, modelo ->
                        serieGuardada = serie
                        marcaGuardada = marca
                        modeloGuardado = modelo
                    }
                )
            }
        }

        composeRule.onNodeWithText("Guardar Equipo").assertIsNotEnabled()

        composeRule.onNodeWithText("Código / Serie del Equipo").performTextInput("SN999")
        composeRule.onNodeWithText("Marca del Equipo (Ej: Samsung)").performTextInput("Lenovo")
        composeRule.onNodeWithText("Modelo (Ej: Galaxy S22)").performTextInput("ThinkPad")

        composeRule.onNodeWithText("Guardar Equipo").assertIsEnabled().performClick()

        assertEquals("SN999", serieGuardada)
        assertEquals("Lenovo", marcaGuardada)
        assertEquals("ThinkPad", modeloGuardado)
    }

    @Test
    fun admin_crea_usuario_pinchando_rol_y_guardar() {
        var nombreCreado = ""
        var correoCreado = ""
        var passCreada = ""
        var rolCreado: Rol? = null

        composeRule.setContent {
            MCMovilCareTheme {
                PanelAdminScreen(
                    ordenes = ordenesPrueba,
                    usuarios = listOf(Usuario("1", "Admin", "admin@duoc.cl", Rol.ADMIN, "1234")),
                    onCrearUsuario = { n, c, p, r ->
                        nombreCreado = n
                        correoCreado = c
                        passCreada = p
                        rolCreado = r
                    },
                    onCerrarSesion = {}
                )
            }
        }

        composeRule.onNodeWithText("Nombre").performTextInput("Juan Perez")
        composeRule.onNodeWithText("Correo (ej: tecnico2@duoc.cl)").performTextInput("juan@duoc.cl")
        composeRule.onNodeWithText("Contraseña").performTextInput("clave123")
        composeRule.onNodeWithText("CLIENTE").performClick()
        composeRule.onNodeWithText("Guardar Usuario en BD").performClick()

        assertEquals("Juan Perez", nombreCreado)
        assertEquals("juan@duoc.cl", correoCreado)
        assertEquals("clave123", passCreada)
        assertEquals(Rol.CLIENTE, rolCreado)
    }

    @Test
    fun tecnico_pincha_orden_cambia_estado_a_listo_y_guarda() {
        var estadoFinal = ""

        composeRule.setContent {
            MCMovilCareTheme {
                PanelTecnicoScreen(
                    ordenes = ordenesPrueba,
                    onActualizarOrden = { _, estado, _, _, _, _ -> estadoFinal = estado },
                    onAbrirEscaner = {},
                    onCerrarSesion = {}
                )
            }
        }

        composeRule.onNodeWithText("Samsung Galaxy S22").performClick()
        composeRule.onNodeWithText("Listo").performClick()
        composeRule.onNodeWithText("Guardar Cambios").performClick()

        assertEquals("Listo", estadoFinal)
    }

    @Test
    fun botones_aprobacion_renderizan_y_responden() {
        var aprobado = false
        var rechazado = false

        composeRule.setContent {
            MCMovilCareTheme {
                SeccionBotonesAprobacion(
                    ordenId = "1",
                    onAprobar = { aprobado = true },
                    onRechazar = { rechazado = true }
                )
            }
        }
        composeRule.onNodeWithText("Aprobar").performClick()
        composeRule.onNodeWithText("Rechazar").performClick()
        assertEquals(true, aprobado)
        assertEquals(true, rechazado)
    }

    @Test
    fun scanQrScreen_permite_ingreso_manual() {
        var codigo = ""

        composeRule.setContent {
            MCMovilCareTheme {
                ScanQrScreen(onCodigoDetectado = { codigo = it })
            }
        }
        composeRule.onNodeWithText("Código de equipo / Serie").performTextInput("QR-777")
        composeRule.onNodeWithText("Confirmar Código").performClick()
        assertEquals("QR-777", codigo)
    }

    @Test
    fun historialScreen_renderiza_ordenes() {
        composeRule.setContent {
            MCMovilCareTheme {
                HistorialScreen(ordenes = ordenesPrueba)
            }
        }
        composeRule.onNodeWithText("Historial de Reparaciones").assertExists()
        composeRule.onNodeWithText("MacBook Pro M1").assertExists()
    }

    @Test
    fun dashboardAdminContent_renderiza_ordenes_y_catalogo() {
        composeRule.setContent {
            MCMovilCareTheme {
                DashboardAdminScreenContent(
                    ordenes = ordenesPrueba,
                    usuarios = listOf(Usuario("1", "Admin", "admin@duoc.cl", Rol.ADMIN, "1234")),
                    onGuardarUsuario = { _, _, _ -> },
                    onCerrarSesion = {}
                )
            }
        }
        composeRule.onNodeWithText("Panel Administrador").assertExists()
        composeRule.onNodeWithText("Orden #2 • MacBook Pro M1").assertExists()
    }

    @Test
    fun nuevoEquipoActivity_abre_y_boton_volver_funciona() {
        ActivityScenario.launch(NuevoEquipoActivity::class.java).use {
            composeRule.onNodeWithText("< Volver").assertExists()
            composeRule.onNodeWithText("< Volver").performClick()
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
    fun panelTecnicoScreen_edita_diagnostico_y_cotiza() {
        val ordenConDiagnostico = listOf(
            OrdenTrabajo("1", "Samsung Galaxy S22", "SN123", "En diagnostico", "Falla de carga", 25000.0, false, "10-10-2026")
        )
        var ordenIdGuardada = ""
        var estadoGuardado = ""
        var diagnosticoGuardado = ""

        composeRule.setContent {
            MCMovilCareTheme {
                PanelTecnicoScreen(
                    ordenes = ordenConDiagnostico,
                    onActualizarOrden = { id, estado, diag, _, _, _ ->
                        ordenIdGuardada = id
                        estadoGuardado = estado
                        diagnosticoGuardado = diag
                    },
                    onAbrirEscaner = {},
                    onCerrarSesion = {}
                )
            }
        }

        composeRule.onNodeWithText("Samsung Galaxy S22").performClick()
        composeRule.onNodeWithText("Gestionar Samsung Galaxy S22").assertExists()
        composeRule.onNodeWithText("Falla de carga").performTextClearance()
        composeRule.onNodeWithText("Diagnóstico técnico").performTextInput("Placa reparada OK")
        composeRule.onNodeWithText("Cotizar").performClick()
        composeRule.onNodeWithText("Guardar Cambios").performClick()

        assertEquals("1", ordenIdGuardada)
        assertEquals("Pend. autorizacion", estadoGuardado)
        assertEquals("Placa reparada OK", diagnosticoGuardado)
    }

    @Test
    fun dashboardClienteScreenContent_lista_vacia_dispara_acciones() {
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
    fun nuevoEquipo_autocompleta_marca_y_modelo_al_ingresar_sku() {
        var serieGuardada = ""
        var marcaGuardada = ""
        var modeloGuardado = ""

        composeRule.setContent {
            MCMovilCareTheme {
                NuevoEquipoScreen(
                    onAbrirEscaner = {},
                    onGuardarEquipo = { serie, marca, modelo ->
                        serieGuardada = serie
                        marcaGuardada = marca
                        modeloGuardado = modelo
                    }
                )
            }
        }

        composeRule.onNodeWithText("Código / Serie del Equipo").performTextInput("8806094937121")
        composeRule.onNodeWithText("Guardar Equipo").assertIsEnabled().performClick()

        assertEquals("8806094937121", serieGuardada)
        assertEquals("Samsung", marcaGuardada)
        assertEquals("Galaxy S22", modeloGuardado)
    }
}