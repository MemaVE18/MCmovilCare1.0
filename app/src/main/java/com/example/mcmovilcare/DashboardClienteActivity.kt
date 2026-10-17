package com.example.mcmovilcare

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DashboardClienteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard_cliente)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1 Datos tipo ejemplo para simular el mockup
        val mockOrdenes = listOf(
            OrdenTrabajo("1", "Samsung Galaxy S22", "SN123", "En diagnostico", null, null, false, "10-10-2026"),
            OrdenTrabajo("2", "MacBook Pro M1", "SN456", "Pend. autorizacion", "Pantalla rota", 150000.0, true, "12-10-2026"),
            OrdenTrabajo("3", "iPhone 13", "SN789", "Listo", "Cambio de bateria", 45000.0, false, "15-10-2026")
        )

        // 2 Configurar el RecyclerView con el clic real
        val rvOrdenes = findViewById<RecyclerView>(R.id.rvOrdenes)
        rvOrdenes.layoutManager = LinearLayoutManager(this)
        rvOrdenes.adapter = OrdenAdapter(mockOrdenes) { ordenSeleccionada ->

            val intent = android.content.Intent(this, DetalleOrdenActivity::class.java).apply {
                putExtra("EXTRA_ID", ordenSeleccionada.id)
                putExtra("EXTRA_EQUIPO", ordenSeleccionada.equipoModelo)
                putExtra("EXTRA_SERIE", ordenSeleccionada.numeroSerie)
                putExtra("EXTRA_ESTADO", ordenSeleccionada.estado)
                putExtra("EXTRA_DIAGNOSTICO", ordenSeleccionada.diagnostico ?: "Sin diagnostico registrado aun")
                putExtra("EXTRA_FECHA", ordenSeleccionada.fechaIngreso)

                if (ordenSeleccionada.cotizacionMonto != null) {
                    putExtra("EXTRA_MONTO", ordenSeleccionada.cotizacionMonto)
                }

                // AQUI ESTA LA CORRECCION: usamos requiereAutorizacion
                putExtra("EXTRA_AUTORIZADO", ordenSeleccionada.requiereAutorizacion)
            }
            startActivity(intent)
        }

        // 3 Configurar el boton de salir
        val tvLogout = findViewById<TextView>(R.id.tvLogout)
        tvLogout.setOnClickListener {
            finish()
        }
    }
}