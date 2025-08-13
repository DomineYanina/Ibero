package com.example.ibero.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ibero.R
import com.example.ibero.data.Inspection
import java.text.SimpleDateFormat
import java.util.Locale

class InspectionHistoryAdapter(private val onItemClick: (Inspection) -> Unit) :
    ListAdapter<Inspection, InspectionHistoryAdapter.InspectionViewHolder>(InspectionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InspectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inspection_history, parent, false)
        return InspectionViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: InspectionViewHolder, position: Int) {
        val currentInspection = getItem(position)
        holder.bind(currentInspection)
    }

    class InspectionViewHolder(
        itemView: View,
        private val onItemClick: (Inspection) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val textArticle: TextView = itemView.findViewById(R.id.text_article_ref)
        private val textDate: TextView = itemView.findViewById(R.id.text_date)
        private val textResult: TextView = itemView.findViewById(R.id.text_result)

        fun bind(inspection: Inspection) {
            // Mapeamos los campos del objeto Inspection a las vistas.
            textArticle.text = inspection.articulo

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            textDate.text = dateFormat.format(inspection.fecha)

            val resultText = if (inspection.tipoCalidad == "Segunda" && inspection.tipoDeFalla != null) {
                "Calidad: ${inspection.tipoCalidad} - Falla: ${inspection.tipoDeFalla}"
            } else {
                "Calidad: ${inspection.tipoCalidad}"
            }
            textResult.text = resultText

            // Configuramos el listener de clic para que llame a la función onItemClick
            itemView.setOnClickListener {
                onItemClick(inspection)
            }
        }
    }
}

class InspectionDiffCallback : DiffUtil.ItemCallback<Inspection>() {
    override fun areItemsTheSame(oldItem: Inspection, newItem: Inspection): Boolean {
        return oldItem.uniqueId == newItem.uniqueId
    }

    override fun areContentsTheSame(oldItem: Inspection, newItem: Inspection): Boolean {
        return oldItem == newItem
    }
}
