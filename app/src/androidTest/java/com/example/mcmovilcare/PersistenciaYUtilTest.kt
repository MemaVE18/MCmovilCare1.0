package com.example.mcmovilcare

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
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
import com.example.mcmovilcare.util.NotificacionHelper
import com.example.mcmovilcare.util.PdfGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistenciaYUtilTest {

    @Before
    fun configurarEntorno() {
        RetrofitClient.setApiServiceForTesting(object : ApiService {
            override suspend fun obtenerOrdenes(): List<OrdenTrabajo> = emptyList()
            override suspend fun crearOrden(orden: OrdenTrabajo): OrdenTrabajo = orden
            override suspend fun actualizarOrden(id: String, orden: OrdenTrabajo): OrdenTrabajo = orden
            override suspend fun obtenerUsuarios(): List<Usuario> = emptyList()
            override suspend fun crearUsuario(usuario: Usuario): Usuario = usuario
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val packageName = instrumentation.targetContext.packageName
            instrumentation.uiAutomation.executeShellCommand(
                "pm grant $packageName android.permission.POST_NOTIFICATIONS"
            ).close()
        }
    }

    @After
    fun limpiarEntorno() {
        RetrofitClient.setApiServiceForTesting(null)
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

    @Test
    fun detalleOrdenActivity_muestra_estado_listo_sin_cotizacion_y_genera_pdf() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intentListo = Intent(context, DetalleOrdenActivity::class.java).apply {
            putExtra("EXTRA_ID", "3")
            putExtra("EXTRA_EQUIPO", "iPhone 13")
            putExtra("EXTRA_SERIE", "SN789")
            putExtra("EXTRA_ESTADO", "Listo")
            putExtra("EXTRA_AUTORIZADO", false)
        }
        ActivityScenario.launch<DetalleOrdenActivity>(intentListo).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("Costo: Pendiente de cotizacion", activity.findViewById<TextView>(R.id.tvDetalleCosto).text.toString())
                assertEquals("Autorizado por el cliente: SI", activity.findViewById<TextView>(R.id.tvDetalleAutorizado).text.toString())
                assertEquals(View.GONE, activity.findViewById<LinearLayout>(R.id.layoutAprobacion).visibility)
                activity.findViewById<Button>(R.id.btnGenerarPdf).performClick()
            }
        }
    }

    @Test
    fun ordenAdapter_vincula_datos_con_y_sin_monto_y_dispara_click() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val lista = listOf(
            OrdenTrabajo("1", "Galaxy S22", "SN1", "En diagnostico", null, null, false, "10-10-2026"),
            OrdenTrabajo("2", "iPad Air", "SN2", "Listo", "Bateria nueva", 55000.0, false, "11-10-2026")
        )
        var seleccionada: OrdenTrabajo? = null
        val adapter = OrdenAdapter(lista) { seleccionada = it }

        assertEquals(2, adapter.itemCount)

        val parent = FrameLayout(context)
        val vh0 = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(vh0, 0)
        assertEquals("Galaxy S22", vh0.tvEquipo.text.toString())
        assertEquals(View.GONE, vh0.tvMonto.visibility)

        val vh1 = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(vh1, 1)
        assertEquals("iPad Air", vh1.tvEquipo.text.toString())
        assertEquals(View.VISIBLE, vh1.tvMonto.visibility)
        assertEquals("$55000.0", vh1.tvMonto.text.toString())

        vh1.itemView.performClick()
        assertEquals("2", seleccionada?.id)
    }
}