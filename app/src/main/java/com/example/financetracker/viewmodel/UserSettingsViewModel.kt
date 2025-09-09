package com.example.financetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.financetracker.data.FinanceDatabase
import com.example.financetracker.data.UserSettings
import com.example.financetracker.data.UserSettingsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class UserSettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val userSettingsDao: UserSettingsDao
    
    // LiveData for observing user settings
    val userSettings: LiveData<UserSettings>
    
    // Flow properties for individual settings
    val monthlySalary: Flow<Double>
    val monthlyBudget: Flow<Double>
    val currency: Flow<String>

    init {
        val database = FinanceDatabase.getInstance(application)
        userSettingsDao = database.userSettingsDao()
        
        // Initialize LiveData
        userSettings = userSettingsDao.getUserSettingsLive()
        
        // Initialize Flow properties
        monthlySalary = userSettingsDao.getMonthlySalary()
        monthlyBudget = userSettingsDao.getMonthlyBudget()
        currency = userSettingsDao.getCurrency()
    }
    
    // Method to save complete UserSettings
    fun saveUserSettings(userSettings: UserSettings) {
        viewModelScope.launch {
            if (userSettingsDao.getUserSettingsCount() > 0) {
                userSettingsDao.updateUserSettings(userSettings)
            } else {
                userSettingsDao.insertUserSettings(userSettings)
            }
        }
    }
    
    // Individual setters for backward compatibility
    fun updateMonthlySalary(salary: Double) {
        viewModelScope.launch {
            userSettingsDao.updateMonthlySalary(salary)
        }
    }
    
    fun updateMonthlyBudget(budget: Double) {
        viewModelScope.launch {
            userSettingsDao.updateMonthlyBudget(budget)
        }
    }
    
    fun updateCurrency(currency: String) {
        viewModelScope.launch {
            userSettingsDao.updateCurrency(currency)
        }
    }
    
    fun updateBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsDao.updateBudgetAlertsEnabled(enabled)
        }
    }
} 