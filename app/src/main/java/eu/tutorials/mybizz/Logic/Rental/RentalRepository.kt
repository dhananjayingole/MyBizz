package eu.tutorials.mybizz.Logic.Rental

import android.util.Log
import eu.tutorials.mybizz.Model.Rental

class RentalRepository {
    companion object {
        private const val TAG = "RentalRepository"
    }

    suspend fun addRental(rental: Rental, sheetsRepo: RentalSheetsRepository): Boolean {
        return try {
            Log.d(TAG, "Adding rental: ${rental.tenantName}")
            sheetsRepo.addRental(rental)
        } catch (e: Exception) {
            Log.e(TAG, "Error in addRental", e)
            false
        }
    }

    suspend fun updateRental(rental: Rental, sheetsRepo: RentalSheetsRepository): Boolean {
        return try {
            Log.d(TAG, "Updating rental: ${rental.id}")
            sheetsRepo.updateRental(rental)
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateRental", e)
            false
        }
    }

    suspend fun deleteRental(rentalId: String, sheetsRepo: RentalSheetsRepository): Boolean {
        return try {
            Log.d(TAG, "Deleting rental: $rentalId")
            sheetsRepo.deleteRental(rentalId)
        } catch (e: Exception) {
            Log.e(TAG, "Error in deleteRental", e)
            false
        }
    }

    suspend fun markRentalAsPaid(rentalId: String, sheetsRepo: RentalSheetsRepository): Boolean {
        return try {
            Log.d(TAG, "Marking rental as paid: $rentalId")
            sheetsRepo.markRentalAsPaid(rentalId)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking rental as paid", e)
            false
        }
    }

    // ADD THIS METHOD - It's missing
    suspend fun getAllRentals(sheetsRepo: RentalSheetsRepository): List<Rental> {
        return try {
            Log.d(TAG, "Fetching all rentals")
            sheetsRepo.getAllRentals()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all rentals", e)
            emptyList()
        }
    }
}