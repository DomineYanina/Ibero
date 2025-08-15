package com.example.ibero.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ibero.R
import com.example.ibero.data.Inspection

class CurrentSessionInspectionAdapter(
    private val inspections: MutableList<Inspection>
) : RecyclerView.Adapter<CurrentSessionInspectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val calidadTextView: TextView = view.findViewById(R.id.text_view_item_calidad)
        val fallaTextView: TextView = view.findViewById(R.id.text_view_item_falla) // Nueva referencia
        val metrosTextView: TextView = view.findViewById(R.id.text_view_item_metros)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_current_session_inspection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val inspection = inspections[position]

        holder.calidadTextView.text = "Calidad: ${inspection.tipoCalidad}"
        holder.metrosTextView.text = "Metros: ${inspection.metrosDeTela}"

        // Lógica para mostrar u ocultar el tipo de falla
        if (inspection.tipoCalidad == "Segunda" && inspection.tipoDeFalla != null) {
            holder.fallaTextView.visibility = View.VISIBLE
            holder.fallaTextView.text = "Falla: ${inspection.tipoDeFalla}"
        } else {
            holder.fallaTextView.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = inspections.size

    fun updateList(newList: List<Inspection>) {
        inspections.clear()
        inspections.addAll(newList)
        notifyDataSetChanged()
    }
}