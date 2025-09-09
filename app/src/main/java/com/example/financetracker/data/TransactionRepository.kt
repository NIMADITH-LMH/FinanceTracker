package com.example.financetracker.data

import com.example.financetracker.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.*

class TransactionRepository(private val transactionDao: TransactionDao) {

    fun getTransactionsBetweenDates(startDate: Date, endDate: Date): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetweenDates(startDate, endDate)

    suspend fun insertTransaction(transaction: TransactionEntity) =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    suspend fun getTotalIncomeBetweenDates(startDate: Date, endDate: Date): Double =
        transactionDao.getTotalIncomeBetweenDates(startDate, endDate) ?: 0.0

    suspend fun getTotalExpensesBetweenDates(startDate: Date, endDate: Date): Double =
        transactionDao.getTotalExpensesBetweenDates(startDate, endDate) ?: 0.0

    fun getTopExpenseCategories(startDate: Date, endDate: Date): Flow<List<CategoryTotal>> =
        transactionDao.getTopExpenseCategories(startDate, endDate)

    // Add these methods to your TransactionRepository class:
    fun getAllTransactions(): Flow<List<TransactionEntity>> {
        return transactionDao.getAllTransactions()
    }

    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByType(type.name)
    }

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }
}
