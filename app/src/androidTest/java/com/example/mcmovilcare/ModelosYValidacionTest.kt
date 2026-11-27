package com.example.mcmovilcare

import com.example.mcmovilcare.model.Equipo
import com.example.mcmovilcare.model.EquipoFormUiState
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.model.Rol
import com.example.mcmovilcare.model.Usuario
import com.example.mcmovilcare.util.SkuCatalogo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
        assertEquals("15-04-2027", orden.proximaMantencion)
        assertEquals("Equipo ingresado sin danos adicionales en carcasa", orden.evidencia)
    }

    @Test
    fun equipo_guarda_datos_basicos() {
        val eq = Equipo("1", "cli_1", "Apple", "MacBook Air", "SN777", "Cargador")
        assertEquals("1", eq.id)
        assertEquals("cli_1", eq.clienteId)
        assertEquals("Apple", eq.marca)
        assertEquals("MacBook Air", eq.modelo)
        assertEquals("SN777", eq.numeroSerie)
        assertEquals("Cargador", eq.accesorios)
    }

    @Test
    fun equipoFormUiState_inicia_invalido() {
        val state = EquipoFormUiState()
        assertFalse(state.esValido)
        assertFalse(state.guardadoExitoso)
        assertTrue(state.marca.isEmpty())
        assertNull(state.errorMarca)
        assertNull(state.errorModelo)
        assertNull(state.errorSerie)
    }

    @Test
    fun skuCatalogo_detecta_codigo_exacto_prefijo_y_desconocido() {
        val exacto = SkuCatalogo.buscarPorCodigo("8806094937121")
        assertEquals("Samsung" to "Galaxy S22", exacto)

        val prefijo = SkuCatalogo.buscarPorCodigo("sku-app-iphone15")
        assertEquals("Apple" to "IPHONE15", prefijo)

        assertNull(SkuCatalogo.buscarPorCodigo("CODIGO-INEXISTENTE"))
    }

    @Test
    fun skuCatalogo_cubre_todos_los_codigos_exactos_y_prefijos_personalizados() {
        assertEquals("Apple" to "iPhone 14", SkuCatalogo.buscarPorCodigo("0194253408345"))
        assertEquals("Apple" to "MacBook Pro M1", SkuCatalogo.buscarPorCodigo("0195949035944"))
        assertEquals("Lenovo" to "ThinkPad", SkuCatalogo.buscarPorCodigo("0196802345612"))
        assertEquals("Xiaomi" to "Redmi Note 12", SkuCatalogo.buscarPorCodigo("6941812701234"))

        assertEquals("Samsung" to "GALAXY S24 ULTRA", SkuCatalogo.buscarPorCodigo("  sku-sam-galaxy-s24-ultra  "))
        assertEquals("Lenovo" to "YOGA 7", SkuCatalogo.buscarPorCodigo("SKU-LEN-YOGA-7"))
        assertEquals("Xiaomi" to "POCO X6", SkuCatalogo.buscarPorCodigo("SKU-XIA-POCO-X6"))
        assertEquals("HP" to "OMEN 16", SkuCatalogo.buscarPorCodigo("SKU-HP-OMEN-16"))
        assertEquals("Asus" to "ROG STRIX", SkuCatalogo.buscarPorCodigo("SKU-ASUS-ROG-STRIX"))

        assertNull(SkuCatalogo.buscarPorCodigo("SKU-INCOMPLETO"))
        assertNull(SkuCatalogo.buscarPorCodigo(""))
    }

    @Test
    fun rol_enum_contiene_los_tres_perfiles_del_sistema() {
        val roles = Rol.values().toList()
        assertEquals(3, roles.size)
        assertTrue(roles.containsAll(listOf(Rol.CLIENTE, Rol.TECNICO, Rol.ADMIN)))
    }
}