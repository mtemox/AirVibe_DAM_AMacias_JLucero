package com.example.airvibe.feature.services.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ServicesViewModel : ViewModel() {

    private val _categories = MutableStateFlow(
        listOf(
            ServiceCategory("Electricista", isSelected = true),
            ServiceCategory("Albañil"),
            ServiceCategory("Plomero"),
            ServiceCategory("Limpieza"),
            ServiceCategory("Carpintero"),
        ),
    )
    val categories: StateFlow<List<ServiceCategory>> = _categories.asStateFlow()

    private val _providers = MutableStateFlow<List<ServiceProvider>>(emptyList())
    val providers: StateFlow<List<ServiceProvider>> = _providers.asStateFlow()

    fun onCategorySelected(categoryName: String) {
        _categories.value = _categories.value.map { category ->
            category.copy(isSelected = category.name == categoryName)
        }
    }

    fun onContactProvider(provider: ServiceProvider) {
        // TODO: cuando exista el backend de servicios, abrir chat con
        // el provider o navegar a su perfil usando su nodeId real.
    }
}
