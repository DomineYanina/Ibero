package com.example.ibero

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ibero.ui.InspectionViewModel

class InspectionViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InspectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InspectionViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}