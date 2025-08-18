// TonalidadesAdapter.kt
package com.example.ibero.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ibero.R
import com.example.ibero.data.TonalidadItem // AÑADE esta importación

class TonalidadesAdapter(private val items: MutableList<TonalidadItem>) :
    RecyclerView.Adapter<TonalidadesAdapter.TonalidadViewHolder>() {

    class TonalidadViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewColumnaT: TextView = view.findViewById(R.id.text_view_columna_t)
        val editTextTonalidad: EditText = view.findViewById(R.id.edit_text_tonalidad)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TonalidadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tonalidad, parent, false)
        return TonalidadViewHolder(view)
    }

    override fun onBindViewHolder(holder: TonalidadViewHolder, position: Int) {
        val item = items[position]
        holder.textViewColumnaT.text = item.valorColumnaT
        holder.editTextTonalidad.setText(item.nuevaTonalidad)

        holder.editTextTonalidad.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                item.nuevaTonalidad = holder.editTextTonalidad.text.toString()
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<TonalidadItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getUpdatedItems(): List<TonalidadItem> {
        return items.filter { it.nuevaTonalidad.isNotEmpty() }
    }
}