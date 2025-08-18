package com.example.ibero.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ibero.R
import com.example.ibero.data.TonalidadItem
import com.google.android.material.textfield.TextInputEditText

class TonalidadesAdapter(private var items: MutableList<TonalidadItem>) :
    RecyclerView.Adapter<TonalidadesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val uniqueIdTextView: TextView = view.findViewById(R.id.text_view_columna_t)
        val tonalidadEditText: TextInputEditText = view.findViewById(R.id.edit_text_tonalidad)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tonalidad, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // Muestra el ID de la hoja de ruta
        holder.uniqueIdTextView.text = item.valorHojaDeRutaId

        // Lógica de llenado y habilitación/deshabilitación
        if (!item.isEditable) {
            // El campo ya tiene un valor, lo muestra y lo deshabilita
            holder.tonalidadEditText.setText(item.tonalidadPrevia)
            holder.tonalidadEditText.isEnabled = false
            holder.tonalidadEditText.alpha = 0.5f // Opcional: para que se vea deshabilitado
        } else {
            // No tiene valor, es un nuevo registro, el campo está vacío y habilitado
            holder.tonalidadEditText.text?.clear()
            holder.tonalidadEditText.isEnabled = true
            holder.tonalidadEditText.alpha = 1.0f // Opcional
        }

        // Escucha cambios para guardar la nueva tonalidad
        holder.tonalidadEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                item.nuevaTonalidad = holder.tonalidadEditText.text.toString()
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: MutableList<TonalidadItem>) { // Se eliminó el '?' de TonalidadItem
        this.items = newList
        notifyDataSetChanged()
    }

    // Devuelve solo los ítems que son editables y han cambiado
    fun getUpdatedItems(): List<TonalidadItem> {
        return items.filter { it.isEditable && it.nuevaTonalidad.isNotBlank() }
    }
}