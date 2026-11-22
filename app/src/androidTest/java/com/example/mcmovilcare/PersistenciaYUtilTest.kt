package com.example.mcmovilcare

import android.os.Build
import org.junit.Before
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mcmovilcare.data.local.AppDatabase
import com.example.mcmovilcare.model.Equipo
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.model.Rol
import com.example.mcmovilcare.model.Usuario
import com.example.mcmovilcare.util.NotificacionHelper
import com.example.mcmovilcare.util.PdfGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistenciaYUtilTest {

    @Before
    fun concederPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val packageName = instrumentation.targetContext.packageName
            instrumentation.uiAutomation.executeShellCommand(
                "pm grant $packageName android.permission.POST_NOTIFICATIONS"
            ).close()
        }
    }

    private val ordenesPrueba = listOf(
        OrdenTrabajo("1", "Samsung Galaxy S22", "SN123", "En diagnostico", null, null, false, "10-10-2026")
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
        val archivosPdf = (context.cacheDir.listFiles()?.toList().orEmpty() +
                context.filesDir.listFiles()?.toList().orEmpty() +
                context.getExternalFilesDir(null)?.listFiles()?.toList().orEmpty())
            .filter { it.extension.equals("pdf", ignoreCase = true) && it.length() > 0 }
        assertTrue("Debe haberse generado un PDF no vacío", archivosPdf.isNotEmpty())
    }

    @Test
    fun detalleOrdenActivity_aprueba_y_rechaza_actualizando_vista() {
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
        ActivityScenario.launch<DetalleOrdenActivity>(intentAprobar).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<Button>(R.id.btnAprobarOrden).performClick()
                assertEquals("Estado: Aprobado", activity.findViewById<TextView>(R.id.tvDetalleEstado).text.toString())
                assertEquals("Autorizado por el cliente: SI", activity.findViewById<TextView>(R.id.tvDetalleAutorizado).text.toString())
                assertEquals(View.GONE, activity.findViewById<LinearLayout>(R.id.layoutAprobacion).visibility)
            }
        }

        val intentRechazar = Intent(context, DetalleOrdenActivity::class.java).apply {
            putExtra("EXTRA_ID", "2")
            putExtra("EXTRA_EQUIPO", "MacBook Pro M1")
            putExtra("EXTRA_SERIE", "SN456")
            putExtra("EXTRA_ESTADO", "Pend. autorizacion")
            putExtra("EXTRA_AUTORIZADO", true)
        }
        ActivityScenario.launch<DetalleOrdenActivity>(intentRechazar).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<Button>(R.id.btnRechazarOrden).performClick()
                assertEquals("Estado: Rechazado", activity.findViewById<TextView>(R.id.tvDetalleEstado).text.toString())
                assertEquals("Autorizado por el cliente: NO (Rechazado)", activity.findViewById<TextView>(R.id.tvDetalleAutorizado).text.toString())
                assertEquals(View.GONE, activity.findViewById<LinearLayout>(R.id.layoutAprobacion).visibility)
            }
        }
    }
}