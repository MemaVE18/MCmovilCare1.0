package com.example.mcmovilcare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.mcmovilcare.data.local.AppDatabase
import com.example.mcmovilcare.model.OrdenTrabajo
import com.example.mcmovilcare.repository.AppRepository
import com.example.mcmovilcare.util.NotificacionHelper
import com.example.mcmovilcare.viewmodel.OrdenTrabajoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        val rvOrdenes = findViewById<RecyclerView>(R.id.rvOrdenes)
        rvOrdenes.layoutManager = LinearLayoutManager(this)

        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "mcmovilcare_db"
        )
            .enableMultiInstanceInvalidation()
            .build()

        val repository = AppRepository(
            database.usuarioDao(),
            database.equipoDao(),
            database.ordenTrabajoDao()
        )
        lifecycleScope.launch(Dispatchers.IO) {
            val actuales = repository.obtenerOrdenes.first()
            if (actuales.isEmpty()) {
                val mockOrdenes = listOf(
                    OrdenTrabajo("1", "Samsung Galaxy S22", "SN123", "En diagnostico", null, null, false, "10-10-2026"),
                    OrdenTrabajo("2", "MacBook Pro M1", "SN456", "Pend. autorizacion", "Pantalla rota", 150000.0, true, "12-10-2026"),
                    OrdenTrabajo("3", "iPhone 13", "SN789", "Listo", "Cambio de bateria", 45000.0, false, "15-10-2026")
                )
                mockOrdenes.forEach { repository.guardarOrden(it) }
            }

            repository.obtenerOrdenes.collect { listaOrdenes ->
                withContext(Dispatchers.Main) {
                    rvOrdenes.adapter = OrdenAdapter(listaOrdenes) { ordenSeleccionada ->
                        val intent = Intent(this@DashboardClienteActivity, DetalleOrdenActivity::class.java).apply {
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
                            putExtra("EXTRA_GARANTIA", ordenSeleccionada.garantia ?: "Sin garantia activa")
                            putExtra("EXTRA_MANTENCION", ordenSeleccionada.proximaMantencion ?: "No programada")
                            putExtra("EXTRA_EVIDENCIA", ordenSeleccionada.evidencia ?: "Sin evidencias adjuntas")
                        }
                        startActivity(intent)
                    }
                }
            }
        }

        val btnEscanearQR = findViewById<Button>(R.id.btnEscanearQR)
        btnEscanearQR.setOnClickListener {
            val intent = Intent(this, NuevoEquipoActivity::class.java)
            startActivity(intent)
        }

        val tvLogout = findViewById<TextView>(R.id.tvLogout)
        tvLogout.setOnClickListener {

            val sharedPreferences = getSharedPreferences("MisPreferenciasApp", android.content.Context.MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()

            val intent = android.content.Intent(this, MainActivity::class.java)

            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        NotificacionHelper.enviarNotificacion(
            this,
            "MCmovilCare",
            "Tu equipo Samsung Galaxy S22 cambio a estado: Listo"
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
        viewModel.cargarEquiposDesdeApi()
    }
}