package com.example.mcmovilcare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mcmovilcare.model.OrdenTrabajo

class OrdenAdapter(
    private val listaOrdenes: List<OrdenTrabajo>,
    private val onVerDetalleClick: (OrdenTrabajo) -> Unit
) : RecyclerView.Adapter<OrdenAdapter.OrdenViewHolder>() {

    class OrdenViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEquipo: TextView = view.findViewById(R.id.tvEquipo)
        val tvEstado: TextView = view.findViewById(R.id.tvEstado)
        val tvSerie: TextView = view.findViewById(R.id.tvSerie)
        val tvDiagnostico: TextView = view.findViewById(R.id.tvDiagnostico)
        val tvMonto: TextView = view.findViewById(R.id.tvMonto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrdenViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_orden, parent, false)
        return OrdenViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrdenViewHolder, position: Int) {
        val orden = listaOrdenes[position]
        holder.tvEquipo.text = orden.equipoModelo
        holder.tvEstado.text = orden.estado
        holder.tvSerie.text = "S/N: ${orden.numeroSerie}"
        holder.tvDiagnostico.text = orden.diagnostico ?: "Sin diagnostico registrado aun."

        // Logica para mostrar u ocultar el costo
        if (orden.cotizacionMonto != null) {
            holder.tvMonto.visibility = View.VISIBLE
            holder.tvMonto.text = "$${orden.cotizacionMonto}"
        } else {
            holder.tvMonto.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onVerDetalleClick(orden)
        }
    }

    override fun getItemCount() = listaOrdenes.size
}