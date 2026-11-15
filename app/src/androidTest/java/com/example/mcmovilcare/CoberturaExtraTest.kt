package com.example.mcmovilcare

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mcmovilcare.data.local.AppDatabase
import com.example.mcmovilcare.model.Equipo
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.model.Rol
import com.example.mcmovilcare.model.Usuario
import com.example.mcmovilcare.ui.components.SeccionBotonesAprobacion
import com.example.mcmovilcare.ui.screens.HistorialScreen
import com.example.mcmovilcare.ui.screens.ScanQrScreen
import com.example.mcmovilcare.ui.theme.MCMovilCareTheme
import com.example.mcmovilcare.util.NotificacionHelper
import com.example.mcmovilcare.util.PdfGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoberturaExtraTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val ordenesPrueba = listOf(
        OrdenTrabajo("1", "Samsung Galaxy S22", "SN123", "En diagnostico", null, null, false, "10-10-2026"),
        OrdenTrabajo("2", "MacBook Pro M1", "SN456", "Pend. autorizacion", "Pantalla rota", 150000.0, true, "12-10-2026"),
        OrdenTrabajo("3", "iPhone 13", "SN789", "Listo", "Bateria", 45000.0, false, "15-10-2026")
    )

    @Test
    fun room_daos_reales_insertan_consultan_y_actualizan() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val u = Usuario("u1", "Pedro", "pedro@duoc.cl", Rol.TECNICO, "1234")
        db.usuarioDao().insertarUsuario(u)
        assertEquals("Pedro", db.usuarioDao().obtenerPorEmail("pedro@duoc.cl")?.nombre)
        assertNotNull(db.usuarioDao().obtenerUsuarioPorId("u1").first())
        assertEquals(1, db.usuarioDao().obtenerTodosLosUsuarios().first().size)

        val eq = Equipo("e1", "c1", "HP", "Pavilion", "SN-HP", "Cargador")
        db.equipoDao().insertarEquipo(eq)
        assertEquals(1, db.equipoDao().obtenerEquipos().first().size)

        db.ordenTrabajoDao().insertarOrden(ordenesPrueba[0])
        db.ordenTrabajoDao().actualizarEstadoOrden("1", "Aprobado")
        assertEquals("Aprobado", db.ordenTrabajoDao().obtenerOrdenPorId("1")?.estado)

        db.ordenTrabajoDao().actualizarOrdenTecnico("1", "Listo", "OK", 25000.0, false, "Foto OK")
        assertEquals("Listo", db.ordenTrabajoDao().obtenerOrdenPorId("1")?.estado)
        assertEquals(1, db.ordenTrabajoDao().obtenerOrdenes().first().size)

        db.close()
    }

    @Test
    fun util_notificacion_y_pdf_se_ejecutan() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            NotificacionHelper.enviarNotificacion(context, "Test", "Mensaje de prueba")
            PdfGenerator().generarPdfOrden(context, "Orden #1 Test")
        }
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
    fun activities_main_y_detalle_orden_abren_correctamente() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("MisPreferenciasApp", Context.MODE_PRIVATE).edit().clear().apply()

        ActivityScenario.launch(MainActivity::class.java).use {
        }

        val intent = Intent(context, DetalleOrdenActivity::class.java).apply {
            putExtra("EXTRA_ID", "2")
            putExtra("EXTRA_EQUIPO", "MacBook Pro M1")
            putExtra("EXTRA_SERIE", "SN456")
            putExtra("EXTRA_ESTADO", "Pend. autorizacion")
            putExtra("EXTRA_DIAGNOSTICO", "Pantalla rota")
            putExtra("EXTRA_MONTO", 150000.0)
            putExtra("EXTRA_AUTORIZADO", true)
            putExtra("EXTRA_FECHA", "12-10-2026")
        }
        ActivityScenario.launch<DetalleOrdenActivity>(intent).use {
        }
    }
}