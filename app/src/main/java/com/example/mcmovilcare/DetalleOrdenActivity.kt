package com.example.mcmovilcare

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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

        // Recuperar TODOS los datos que vienen desde el Dashboard
        val id = intent.getStringExtra("EXTRA_ID")
        val equipo = intent.getStringExtra("EXTRA_EQUIPO")
        val serie = intent.getStringExtra("EXTRA_SERIE")
        val estado = intent.getStringExtra("EXTRA_ESTADO")
        val diagnostico = intent.getStringExtra("EXTRA_DIAGNOSTICO")
        val monto = intent.getDoubleExtra("EXTRA_MONTO", -1.0)
        val autorizado = intent.getBooleanExtra("EXTRA_AUTORIZADO", false)
        val fecha = intent.getStringExtra("EXTRA_FECHA")

        // Mostrar los datos en los TextViews
        findViewById<TextView>(R.id.tvDetalleID).text = "ID Orden: $id"
        findViewById<TextView>(R.id.tvDetalleEquipo).text = equipo
        findViewById<TextView>(R.id.tvDetalleSerie).text = "S/N: $serie"
        findViewById<TextView>(R.id.tvDetalleEstado).text = "Estado: $estado"
        findViewById<TextView>(R.id.tvDetalleDiagnostico).text = "Diagnostico:\n$diagnostico"
        findViewById<TextView>(R.id.tvDetalleFecha).text = "Fecha de ingreso: $fecha"

        // Manejar el costo (si es -1.0, significa que no habia cotizacion)
        val tvCosto = findViewById<TextView>(R.id.tvDetalleCosto)
        if (monto != -1.0) {
            tvCosto.text = "Costo de reparacion: $$monto"
        } else {
            tvCosto.text = "Costo: Pendiente de cotizacion"
            tvCosto.setTextColor(android.graphics.Color.GRAY) // Cambia a gris si no hay costo
        }

        // Manejar el estado de autorizacion
        val tvAutorizado = findViewById<TextView>(R.id.tvDetalleAutorizado)
        if (autorizado) {
            tvAutorizado.text = "Autorizado por el cliente: SI"
            tvAutorizado.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Verde
        } else {
            tvAutorizado.text = "Autorizado por el cliente: NO"
            tvAutorizado.setTextColor(android.graphics.Color.parseColor("#F44336")) // Rojo
        }

        findViewById<Button>(R.id.btnVolver).setOnClickListener {
            finish()
        }
    }
}