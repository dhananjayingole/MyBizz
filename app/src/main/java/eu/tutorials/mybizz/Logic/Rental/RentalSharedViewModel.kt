package eu.tutorials.mybizz.Logic.Rental

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import eu.tutorials.mybizz.Model.Rental

class RentalSharedViewModel : ViewModel() {

    // The tenant name currently being viewed
    var selectedTenantName by mutableStateOf("")
        private set

    // All rentals for that tenant (passed from list → detail)
    var selectedTenantRentals by mutableStateOf<List<Rental>>(emptyList())
        private set

    fun selectTenant(tenantName: String, rentals: List<Rental>) {
        selectedTenantName    = tenantName
        selectedTenantRentals = rentals
    }

    // Called when a rental is updated/deleted so the detail screen stays fresh
    fun refreshRental(updated: Rental) {
        selectedTenantRentals = selectedTenantRentals.map {
            if (it.id == updated.id) updated else it
        }
    }

    fun removeRental(rentalId: String) {
        selectedTenantRentals = selectedTenantRentals.filter { it.id != rentalId }
    }
}