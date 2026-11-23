package com.example.mcmovilcare

import com.example.mcmovilcare.model.Equipo
import com.example.mcmovilcare.model.EquipoFormUiState
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.model.Rol
import com.example.mcmovilcare.model.Usuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.mcmovilcare.util.SkuCatalogo

class ModelosYValidacionTest {

    @Test
    fun usuario_tiene_password_por_defecto_y_rol_correcto() {
        val u = Usuario("1", "Carlos", "carlos@duoc.cl", Rol.TECNICO)
        assertEquals("1234", u.password)
        assertEquals(Rol.TECNICO, u.rol)
    }

    @Test
    fun ordenTrabajo_valores_por_defecto_son_correctos() {
        val orden = OrdenTrabajo(
            id = "10",
            equipoModelo = "iPhone 14",
            numeroSerie = "SN10",
            estado = "En diagnostico",
            fechaIngreso = "15-11-2026"
        )
        assertNull(orden.diagnostico)
        assertNull(orden.cotizacionMonto)
        assertFalse(orden.requiereAutorizacion)
        assertEquals("Garantia 90 dias vigente", orden.garantia)
    }

    @Test
    fun equipo_guarda_datos_basicos() {
        val eq = Equipo("1", "cli_1", "Apple", "MacBook Air", "SN777", "Cargador")
        assertEquals("Apple", eq.marca)
        assertEquals("SN777", eq.numeroSerie)
    }

    @Test
    fun equipoFormUiState_inicia_invalido() {
        val state = EquipoFormUiState()
        assertFalse(state.esValido)
        assertFalse(state.guardadoExitoso)
        assertTrue(state.marca.isEmpty())
    }

    @Test
    fun skuCatalogo_detecta_codigo_exacto_prefijo_y_desconocido() {
        val exacto = SkuCatalogo.buscarPorCodigo("8806094937121")
        assertEquals("Samsung" to "Galaxy S22", exacto)

        val prefijo = SkuCatalogo.buscarPorCodigo("sku-app-iphone15")
        assertEquals("Apple" to "IPHONE15", prefijo)

        assertNull(SkuCatalogo.buscarPorCodigo("CODIGO-INEXISTENTE"))
    }
}