package com.example.mcmovilcare

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mcmovilcare.util.NotificacionHelper
import com.example.mcmovilcare.util.PdfGenerator

class DetalleOrdenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle_orden)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btnVolver)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Recuperar datos
        val id = intent.getStringExtra("EXTRA_ID") ?: "N/A"
        val equipo = intent.getStringExtra("EXTRA_EQUIPO") ?: "Equipo"
        val serie = intent.getStringExtra("EXTRA_SERIE") ?: "Sin serie"
        val estado = intent.getStringExtra("EXTRA_ESTADO") ?: "Desconocido"
        val diagnostico = intent.getStringExtra("EXTRA_DIAGNOSTICO") ?: "Sin diagnóstico registrado aún"
        val monto = intent.getDoubleExtra("EXTRA_MONTO", -1.0)
        val autorizado = intent.getBooleanExtra("EXTRA_AUTORIZADO", false)
        val fecha = intent.getStringExtra("EXTRA_FECHA") ?: "Sin fecha"

        // Mostrar los datos en la UI
        findViewById<TextView>(R.id.tvDetalleID).text = "ID Orden: $id"
        findViewById<TextView>(R.id.tvDetalleEquipo).text = equipo
        findViewById<TextView>(R.id.tvDetalleSerie).text = "S/N: $serie"
        findViewById<TextView>(R.id.tvDetalleEstado).text = "Estado: $estado"
        findViewById<TextView>(R.id.tvDetalleDiagnostico).text = "Diagnostico:\n$diagnostico"
        findViewById<TextView>(R.id.tvDetalleFecha).text = "Fecha de ingreso: $fecha"

        val tvCosto = findViewById<TextView>(R.id.tvDetalleCosto)
        if (monto != -1.0) {
            tvCosto.text = "Costo de reparacion: $$monto"
        } else {
            tvCosto.text = "Costo: Pendiente de cotizacion"
            tvCosto.setTextColor(Color.GRAY)
        }

        val tvAutorizado = findViewById<TextView>(R.id.tvDetalleAutorizado)
        if (autorizado) {
            tvAutorizado.text = "Autorizado por el cliente: SI"
            tvAutorizado.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            tvAutorizado.text = "Autorizado por el cliente: NO"
            tvAutorizado.setTextColor(Color.parseColor("#F44336"))
        }

        // Disparar notificación de prueba
        NotificacionHelper.enviarNotificacion(
            this,
            "Consulta de Orden #$id",
            "Estás revisando la orden del equipo $equipo."
        )

        // Evento Botón PDF
        findViewById<Button>(R.id.btnGenerarPdf).setOnClickListener {
            val pdfGenerator = PdfGenerator()
            pdfGenerator.generarPdfOrden(
                this,
                "Orden de Servicio #$id - Equipo: $equipo - Estado: $estado"
            )
        }

        // Botón Volver
        findViewById<Button>(R.id.btnVolver).setOnClickListener {
            finish()
        }
    }
}