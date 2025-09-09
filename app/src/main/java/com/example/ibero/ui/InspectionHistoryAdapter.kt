package com.example.ibero.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ibero.R
import com.example.ibero.data.HistoricalInspection

// Se agrega el parámetro clickListener al constructor
class InspectionHistoryAdapter(
    private val inspections: MutableList<HistoricalInspection>,
    private val clickListener: (HistoricalInspection) -> Unit
) : RecyclerView.Adapter<InspectionHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val resultTextView: TextView = view.findViewById(R.id.text_result)
        val failTextView: TextView = view.findViewById(R.id.text_fail)
        val metrosTextView: TextView = view.findViewById(R.id.text_metros)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inspection_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val inspection = inspections[position]

        // Enlazar los datos a las vistas. Usamos las columnas R, S y T.
        holder.resultTextView.text = "Tipo de Calidad: ${inspection.tipoCalidad}"
        holder.metrosTextView.text = "Metros: ${inspection.metrosDeTela}"

        // Muestra la falla solo si existe
        if (inspection.tipoDeFalla.isNullOrBlank()) {
            holder.failTextView.visibility = View.GONE
        } else {
            holder.failTextView.visibility = View.VISIBLE
            holder.failTextView.text = "Tipo de Falla: ${inspection.tipoDeFalla}"
        }

        // Se configura el listener de clics en el itemView del ViewHolder
        holder.itemView.setOnClickListener {
            clickListener(inspection)
        }
    }

    override fun getItemCount(): Int = inspections.size

    // Se corrigió este método para que reciba directamente una lista de HistoricalInspection.
    // Esto hace que la llamada desde ContinuarCargaActivity sea compatible.
    fun updateList(newList: List<HistoricalInspection>) {
        inspections.clear()
        inspections.addAll(newList)
        notifyDataSetChanged()
    }
}
