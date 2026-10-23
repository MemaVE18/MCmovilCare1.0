package com.example.mcmovilcare

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.room.Room
import com.example.mcmovilcare.data.local.AppDatabase
import com.example.mcmovilcare.repository.AppRepository
import com.example.mcmovilcare.util.NotificacionHelper
import com.example.mcmovilcare.util.PdfGenerator
import com.example.mcmovilcare.viewmodel.OrdenTrabajoViewModel

class DetalleOrdenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle_orden)

        // CORRECCIÓN: Aplicar insets al layout principal para evitar que btnVolver se aplaste
        val rootView = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar Room y ViewModel para aprobar o rechazar
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "mcmovilcare_db"
        ).fallbackToDestructiveMigration().build()

        val repository = AppRepository(
            database.usuarioDao(),
            database.equipoDao(),
            database.ordenTrabajoDao()
        )

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OrdenTrabajoViewModel(repository) as T
            }

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return OrdenTrabajoViewModel(repository) as T
            }
        }
        val viewModel = ViewModelProvider(this, factory)[OrdenTrabajoViewModel::class.java]

        // Recuperar datos del Intent
        val id = intent.getStringExtra("EXTRA_ID") ?: "N/A"
        val equipo = intent.getStringExtra("EXTRA_EQUIPO") ?: "Equipo"
        val serie = intent.getStringExtra("EXTRA_SERIE") ?: "Sin serie"
        val estado = intent.getStringExtra("EXTRA_ESTADO") ?: "Desconocido"
        val diagnostico = intent.getStringExtra("EXTRA_DIAGNOSTICO") ?: "Sin diagnostico registrado aun"
        val monto = intent.getDoubleExtra("EXTRA_MONTO", -1.0)
        val requiereAutorizacion = intent.getBooleanExtra("EXTRA_AUTORIZADO", false)
        val fecha = intent.getStringExtra("EXTRA_FECHA") ?: "Sin fecha"
        val garantia = intent.getStringExtra("EXTRA_GARANTIA") ?: "Garantia 90 dias vigente"
        val mantencion = intent.getStringExtra("EXTRA_MANTENCION") ?: "15-04-2027"
        val evidencia = intent.getStringExtra("EXTRA_EVIDENCIA") ?: "Sin evidencias adjuntas"

        // Referencias UI
        val tvEstado = findViewById<TextView>(R.id.tvDetalleEstado)
        val tvAutorizado = findViewById<TextView>(R.id.tvDetalleAutorizado)
        val layoutAprobacion = findViewById<LinearLayout>(R.id.layoutAprobacion)
        val btnAprobar = findViewById<Button>(R.id.btnAprobarOrden)
        val btnRechazar = findViewById<Button>(R.id.btnRechazarOrden)

        // Mostrar los datos en la UI
        findViewById<TextView>(R.id.tvDetalleID).text = "ID Orden: $id"
        findViewById<TextView>(R.id.tvDetalleEquipo).text = equipo
        findViewById<TextView>(R.id.tvDetalleSerie).text = "S/N: $serie"
        tvEstado.text = "Estado: $estado"
        findViewById<TextView>(R.id.tvDetalleDiagnostico).text = "Diagnostico:\n$diagnostico"
        findViewById<TextView>(R.id.tvDetalleFecha).text = "Fecha de ingreso: $fecha"
        findViewById<TextView>(R.id.tvDetalleEvidencia).text = "Evidencia tecnica: $evidencia"
        findViewById<TextView>(R.id.tvDetalleGarantia).text = "Garantia: $garantia"
        findViewById<TextView>(R.id.tvDetalleMantencion).text = "Proxima mantencion preventiva: $mantencion"

        val tvCosto = findViewById<TextView>(R.id.tvDetalleCosto)
        if (monto != -1.0) {
            tvCosto.text = "Costo de reparacion: $$monto"
        } else {
            tvCosto.text = "Costo: Pendiente de cotizacion"
            tvCosto.setTextColor(Color.GRAY)
        }

        // Logica de autorizacion de cotizacion
        if (requiereAutorizacion && estado != "Aprobado" && estado != "Rechazado") {
            tvAutorizado.text = "Cotizacion pendiente de tu autorizacion"
            tvAutorizado.setTextColor(Color.parseColor("#FF9800"))
            layoutAprobacion.visibility = View.VISIBLE
        } else if (estado == "Aprobado" || estado == "Listo") {
            tvAutorizado.text = "Autorizado por el cliente: SI"
            tvAutorizado.setTextColor(Color.parseColor("#4CAF50"))
            layoutAprobacion.visibility = View.GONE
        } else {
            tvAutorizado.text = "Requiere autorizacion: NO / En revision"
            tvAutorizado.setTextColor(Color.GRAY)
            layoutAprobacion.visibility = View.GONE
        }

        // Accion Aprobar Cotizacion
        btnAprobar.setOnClickListener {
            viewModel.aprobarOrden(id)
            tvEstado.text = "Estado: Aprobado"
            tvAutorizado.text = "Autorizado por el cliente: SI"
            tvAutorizado.setTextColor(Color.parseColor("#4CAF50"))
            layoutAprobacion.visibility = View.GONE
            NotificacionHelper.enviarNotificacion(
                this,
                "Cotizacion Aprobada",
                "Has aprobado la reparacion de tu $equipo."
            )
            Toast.makeText(this, "Cotizacion aprobada con exito", Toast.LENGTH_SHORT).show()
        }

        // Accion Rechazar Cotizacion
        btnRechazar.setOnClickListener {
            viewModel.rechazarOrden(id)
            tvEstado.text = "Estado: Rechazado"
            tvAutorizado.text = "Autorizado por el cliente: NO (Rechazado)"
            tvAutorizado.setTextColor(Color.parseColor("#F44336"))
            layoutAprobacion.visibility = View.GONE
            Toast.makeText(this, "Cotizacion rechazada", Toast.LENGTH_SHORT).show()
        }

        // Disparar notificacion de consulta
        NotificacionHelper.enviarNotificacion(
            this,
            "Consulta de Orden #$id",
            "Estas revisando la orden del equipo $equipo."
        )

        // Evento Boton PDF
        findViewById<Button>(R.id.btnGenerarPdf).setOnClickListener {
            val pdfGenerator = PdfGenerator()
            pdfGenerator.generarPdfOrden(
                this,
                "Orden #$id\nEquipo: $equipo\n${tvEstado.text}"
            )
        }

        // Boton Volver
        findViewById<Button>(R.id.btnVolver).setOnClickListener {
            finish()
        }
    }
}