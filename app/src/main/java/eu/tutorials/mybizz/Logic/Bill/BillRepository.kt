// BillRepository.kt - UPDATED VERSION
package eu.tutorials.mybizz.Logic.Bill

import android.util.Log
import eu.tutorials.mybizz.Model.Bill
import eu.tutorials.mybizz.Model.BillHistoryEntry
import eu.tutorials.mybizz.Repository.BillSheetsRepository

class BillRepository {
    companion object {
        private const val TAG = "BillRepository"
    }

    suspend fun addBill(bill: Bill, sheetsRepo: BillSheetsRepository): Boolean {
        return try {
            Log.d(TAG, "Adding bill: ${bill.title}")
            sheetsRepo.addBill(bill)
        } catch (e: Exception) {
            Log.e(TAG, "Error in addBill", e)
            false
        }
    }

    suspend fun updateBill(bill: Bill, sheetsRepo: BillSheetsRepository, updatedBy: String): Boolean {
        return try {
            Log.d(TAG, "Updating bill: ${bill.id}")
            sheetsRepo.updateBill(bill, updatedBy)
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateBill", e)
            false
        }
    }

    suspend fun deleteBill(billId: String, sheetsRepo: BillSheetsRepository): Boolean {
        return try {
            Log.d(TAG, "Deleting bill: $billId")
            sheetsRepo.deleteBill(billId)
        } catch (e: Exception) {
            Log.e(TAG, "Error in deleteBill", e)
            false
        }
    }

    suspend fun markBillAsPaid(billId: String, sheetsRepo: BillSheetsRepository, paidByEmail: String): Boolean {
        return try {
            Log.d(TAG, "Marking bill as paid: $billId by $paidByEmail")
            sheetsRepo.markBillAsPaid(billId, paidByEmail)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking bill as paid", e)
            false
        }
    }

    suspend fun getBillHistory(billNumber: String, sheetsRepo: BillSheetsRepository): List<BillHistoryEntry> {
        return try {
            Log.d(TAG, "Fetching history for bill: $billNumber")
            sheetsRepo.getBillHistory(billNumber)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching bill history", e)
            emptyList()
        }
    }

    // NEW: Get all bills
    suspend fun getAllBills(sheetsRepo: BillSheetsRepository): List<Bill> {
        return try {
            Log.d(TAG, "Fetching all bills")
            sheetsRepo.getAllBills()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all bills", e)
            emptyList()
        }
    }

    // NEW: Get bill by ID
    suspend fun getBillById(billId: String, sheetsRepo: BillSheetsRepository): Bill? {
        return try {
            Log.d(TAG, "Fetching bill by ID: $billId")
            sheetsRepo.getBillById(billId)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching bill by ID", e)
            null
        }
    }
}