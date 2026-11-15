package com.example.mcmovilcare

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.model.Rol
import com.example.mcmovilcare.model.Usuario
import com.example.mcmovilcare.ui.screens.NuevoEquipoScreen
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

        // Al inicio el botón debe estar deshabilitado
        composeRule.onNodeWithText("Guardar Equipo").assertIsNotEnabled()

        // El usuario llena los campos
        composeRule.onNodeWithText("Código / Serie del Equipo").performTextInput("SN999")
        composeRule.onNodeWithText("Marca del Equipo (Ej: Samsung)").performTextInput("Lenovo")
        composeRule.onNodeWithText("Modelo (Ej: Galaxy S22)").performTextInput("ThinkPad")

        // Ahora sí está habilitado y se pincha
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
}