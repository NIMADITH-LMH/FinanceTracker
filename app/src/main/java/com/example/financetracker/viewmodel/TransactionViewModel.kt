package com.example.financetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.financetracker.data.CategoryTotal
import com.example.financetracker.data.FinanceDatabase
import com.example.financetracker.data.TransactionEntity
import com.example.financetracker.data.TransactionRepository
import com.example.financetracker.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    private val database = FinanceDatabase.getDatabase(application)

    init {
        repository = TransactionRepository(database.transactionDao())
    }

    fun insertTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        try {
            repository.insertTransaction(transaction)
        } catch (e: Exception) {
            // Log error or handle as needed
            e.printStackTrace()
        }
    }

    fun updateTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        try {
            repository.updateTransaction(transaction)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        try {
            repository.deleteTransaction(transaction)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Fixed method name to match repository
    fun getAllTransactions(): Flow<List<TransactionEntity>> = repository.getAllTransactions()

    // Fixed method name to match repository
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>> =
        repository.getTransactionsByType(type)

    suspend fun getTotalIncomeBetweenDates(startDate: Date, endDate: Date): Double =
        withContext(Dispatchers.IO) {
            try {
                repository.getTotalIncomeBetweenDates(startDate, endDate)
            } catch (e: Exception) {
                e.printStackTrace()
                0.0
            }
        }

    suspend fun getTotalExpensesBetweenDates(startDate: Date, endDate: Date): Double =
        withContext(Dispatchers.IO) {
            try {
                repository.getTotalExpensesBetweenDates(startDate, endDate)
            } catch (e: Exception) {
                e.printStackTrace()
                0.0
            }
        }

    fun getTopExpenseCategories(startDate: Date, endDate: Date): Flow<List<CategoryTotal>> =
        repository.getTopExpenseCategories(startDate, endDate)

    fun getTransactionsBetweenDates(startDate: Date, endDate: Date): Flow<List<TransactionEntity>> =
        repository.getTransactionsBetweenDates(startDate, endDate)

    /**
     * Get monthly expenses for a specific year and month
     * @param year The year (e.g., 2023)
     * @param month The month (1-12)
     * @return LiveData containing the list of expense transactions for that month
     */
    fun getMonthlyExpenses(year: Int, month: Int): LiveData<List<TransactionEntity>> {
        val result = MutableLiveData<List<TransactionEntity>>()

        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance()

                // First day of month
                calendar.set(year, month - 1, 1, 0, 0, 0) // Month is 0-based in Calendar
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.time

                // Last day of month
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endDate = calendar.time

                // Get transactions and filter expenses only
                getTransactionsBetweenDates(startDate, endDate).collectLatest { transactions ->
                    val expenseTransactions = transactions.filter {
                        it.type == TransactionType.EXPENSE.name || it.type == "EXPENSE"
                    }
                    result.postValue(expenseTransactions)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                result.postValue(emptyList())
            }
        }

        return result
    }

    /**
     * Get monthly income for a specific year and month
     * @param year The year (e.g., 2023)
     * @param month The month (1-12)
     * @return LiveData containing the list of income transactions for that month
     */
    fun getMonthlyIncome(year: Int, month: Int): LiveData<List<TransactionEntity>> {
        val result = MutableLiveData<List<TransactionEntity>>()

        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance()

                // First day of month
                calendar.set(year, month - 1, 1, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.time

                // Last day of month
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endDate = calendar.time

                // Get transactions and filter income only
                getTransactionsBetweenDates(startDate, endDate).collectLatest { transactions ->
                    val incomeTransactions = transactions.filter {
                        it.type == TransactionType.INCOME.name || it.type == "INCOME"
                    }
                    result.postValue(incomeTransactions)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                result.postValue(emptyList())
            }
        }

        return result
    }

    /**
     * Calculate monthly balance (income - expenses)
     * @param year The year
     * @param month The month (1-12)
     * @return The balance for the month
     */
    suspend fun getMonthlyBalance(year: Int, month: Int): Double {
        return try {
            val calendar = Calendar.getInstance()

            // First day of month
            calendar.set(year, month - 1, 1, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startDate = calendar.time

            // Last day of month
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endDate = calendar.time

            val income = getTotalIncomeBetweenDates(startDate, endDate)
            val expenses = getTotalExpensesBetweenDates(startDate, endDate)

            income - expenses
        } catch (e: Exception) {
            e.printStackTrace()
            0.0
        }
    }
}