package com.example.ibero

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ibero.data.Inspection
import java.text.SimpleDateFormat
import java.util.Locale

class InspectionHistoryAdapter(private val onItemClick: (Inspection) -> Unit) :
    ListAdapter<Inspection, InspectionHistoryAdapter.InspectionViewHolder>(InspectionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InspectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inspection_history, parent, false)
        return InspectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: InspectionViewHolder, position: Int) {
        val inspection = getItem(position)
        holder.bind(inspection, onItemClick)
    }

    class InspectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textArticleRef: TextView = itemView.findViewById(R.id.text_article_ref)
        private val textDate: TextView = itemView.findViewById(R.id.text_date)
        private val textResult: TextView = itemView.findViewById(R.id.text_result)
        private val imageSyncStatus: ImageView = itemView.findViewById(R.id.image_sync_status)

        fun bind(inspection: Inspection, onItemClick: (Inspection) -> Unit) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            textArticleRef.text = inspection.articleReference
            textDate.text = dateFormat.format(inspection.inspectionDate)
            textResult.text = inspection.actionTaken

            // Mostrar icono de sincronización
            if (inspection.isSynced) {
                imageSyncStatus.setImageResource(R.drawable.ic_cloud_done) // Icono de sincronizado
                imageSyncStatus.contentDescription = "Sincronizado"
            } else {
                imageSyncStatus.setImageResource(R.drawable.ic_cloud_upload) // Icono de pendiente
                imageSyncStatus.contentDescription = "Pendiente de sincronizar"
            }

            itemView.setOnClickListener { onItemClick(inspection) }
        }
    }

    // Callback para calcular las diferencias entre listas de inspecciones (eficiente para RecyclerView)
    class InspectionDiffCallback : DiffUtil.ItemCallback<Inspection>() {
        override fun areItemsTheSame(oldItem: Inspection, newItem: Inspection): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Inspection, newItem: Inspection): Boolean {
            return oldItem == newItem
        }
    }
}