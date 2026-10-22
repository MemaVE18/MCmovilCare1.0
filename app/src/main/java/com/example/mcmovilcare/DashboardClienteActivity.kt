package com.example.mcmovilcare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.mcmovilcare.util.NotificacionHelper
import com.example.mcmovilcare.repository.AppRepository
import com.example.mcmovilcare.viewmodel.OrdenTrabajoViewModel
import com.example.mcmovilcare.data.local.AppDatabase

// Definicion de la data class local
data class OrdenTrabajo(
    val id: String,
    val equipoModelo: String,
    val numeroSerie: String,
    val estado: String,
    val diagnostico: String? = null,
    val cotizacionMonto: Double? = null,
    val requiereAutorizacion: Boolean = false,
    val fechaIngreso: String
)

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

        // 1. Datos de prueba
        val mockOrdenes = listOf(
            OrdenTrabajo("1", "Samsung Galaxy S22", "SN123", "En diagnostico", null, null, false, "10-10-2026"),
            OrdenTrabajo("2", "MacBook Pro M1", "SN456", "Pend. autorizacion", "Pantalla rota", 150000.0, true, "12-10-2026"),
            OrdenTrabajo("3", "iPhone 13", "SN789", "Listo", "Cambio de bateria", 45000.0, false, "15-10-2026")
        )

        // 2. Configurar el RecyclerView
        val rvOrdenes = findViewById<RecyclerView>(R.id.rvOrdenes)
        rvOrdenes.layoutManager = LinearLayoutManager(this)
        rvOrdenes.adapter = OrdenAdapter(mockOrdenes) { ordenSeleccionada ->
            val intent = Intent(this, DetalleOrdenActivity::class.java).apply {
                putExtra("EXTRA_ID", ordenSeleccionada.id)
                putExtra("EXTRA_EQUIPO", ordenSeleccionada.equipoModelo)
                putExtra("EXTRA_SERIE", ordenSeleccionada.numeroSerie)
                putExtra("EXTRA_ESTADO", ordenSeleccionada.estado)
                putExtra("EXTRA_DIAGNOSTICO", ordenSeleccionada.diagnostico ?: "Sin diagnostico registrado aun")
                putExtra("EXTRA_FECHA", ordenSeleccionada.fechaIngreso)

                if (ordenSeleccionada.cotizacionMonto != null) {
                    putExtra("EXTRA_MONTO", ordenSeleccionada.cotizacionMonto)
                }

                putExtra("EXTRA_AUTORIZADO", ordenSeleccionada.requiereAutorizacion)
            }
            startActivity(intent)
        }

        // 3. Boton Escaner QR
        val btnEscanearQR = findViewById<Button>(R.id.btnEscanearQR)
        btnEscanearQR.setOnClickListener {
            val intent = Intent(this, NuevoEquipoActivity::class.java)
            startActivity(intent)
        }

        // 4. Configurar el boton de salir
        val tvLogout = findViewById<TextView>(R.id.tvLogout)
        tvLogout.setOnClickListener {
            finish()
        }

        // 5. Probar notificacion
        NotificacionHelper.enviarNotificacion(
            this,
            "MCmovilCare",
            "Tu equipo Samsung Galaxy S22 cambio a estado: Listo"
        )

        // 6. Inicializacion correcta construyendo Room directamente
        val factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                // Creamos la base de datos directo con el builder de Room
                val database = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    "mcmovilcare_db"
                ).fallbackToDestructiveMigration().build()

                // Pasamos los DAOs
                val repository = AppRepository(
                    database.usuarioDao(),
                    database.equipoDao(),
                    database.ordenTrabajoDao()
                )

                return OrdenTrabajoViewModel(repository) as T
            }
        }

        val viewModel = ViewModelProvider(this, factory)[OrdenTrabajoViewModel::class.java]

        viewModel.cargarEquiposDesdeApi()
    }
}