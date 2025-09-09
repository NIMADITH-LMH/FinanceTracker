package com.example.financetracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.financetracker.data.UserSettings
import com.example.financetracker.databinding.ActivityBudgetBinding
import com.example.financetracker.viewmodel.TransactionViewModel
import com.example.financetracker.viewmodel.UserSettingsViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import android.content.res.ColorStateList
import com.google.android.material.bottomnavigation.BottomNavigationView

class BudgetActivity : AppCompatActivity() {
    private val TAG = "BudgetActivity"
    
    private lateinit var binding: ActivityBudgetBinding
    private lateinit var userSettingsViewModel: UserSettingsViewModel
    private lateinit var transactionViewModel: TransactionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        try {
            // Set up the toolbar
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "Budget & Income"
            
            // Initialize view models
            userSettingsViewModel = ViewModelProvider(this)[UserSettingsViewModel::class.java]
            transactionViewModel = ViewModelProvider(this)[TransactionViewModel::class.java]
            
            // Load current settings
            observeUserSettings()
            
            // Calculate current spending
            calculateCurrentSpending()
            
            // Set up save button
            binding.saveButton.setOnClickListener {
                saveSettings()
            }
            
            // Setup bottom navigation
            setupBottomNavigation()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Could not initialize budget settings", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun observeUserSettings() {
        userSettingsViewModel.userSettings.observe(this) { settings ->
            if (settings != null) {
                Log.d(TAG, "Observed UserSettings: $settings")
                // Populate UI with settings values
                binding.salaryInput.setText(settings.monthlySalary.toString())
                binding.budgetLimitInput.setText(settings.monthlyBudget.toString())
                
                // Update UI with current values
                val formatter = NumberFormat.getCurrencyInstance().apply {
                    currency = Currency.getInstance("LKR")
                }
                binding.monthlySalaryDisplay.text = formatter.format(settings.monthlySalary)
                binding.monthlyBudgetDisplay.text = formatter.format(settings.monthlyBudget)
            } else {
                Log.d(TAG, "No UserSettings found, initializing with defaults")
                // Set default values if no settings exist
                binding.salaryInput.setText("0.0")
                binding.budgetLimitInput.setText("0.0")
                binding.monthlySalaryDisplay.text = "$0.00"
                binding.monthlyBudgetDisplay.text = "$0.00"
            }
        }
    }
    
    private fun saveSettings() {
        try {
            val monthlySalaryText = binding.salaryInput.text.toString()
            val budgetLimitText = binding.budgetLimitInput.text.toString()

            val monthlySalary = if (monthlySalaryText.isNotEmpty()) monthlySalaryText.toDouble() else 0.0
            val budgetLimit = if (budgetLimitText.isNotEmpty()) budgetLimitText.toDouble() else 0.0

            Log.d(TAG, "Saving settings - Salary: $monthlySalary, Budget: $budgetLimit")

            // Save to Room database
            val settings = UserSettings(
                id = 1, // Always use ID 1 for single user settings
                monthlySalary = monthlySalary,
                monthlyBudget = budgetLimit,
                currency = "LKR",
                budgetAlertsEnabled = true
            )
            
            userSettingsViewModel.saveUserSettings(settings)

            // Show success message
            Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
            
            // Recalculate current spending with new budget
            calculateCurrentSpending()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving settings", e)
            Toast.makeText(this, "Error saving settings: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun calculateCurrentSpending() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
        
        // Format for logging
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val monthYearStr = monthYearFormat.format(calendar.time)
        
        Log.d(TAG, "Calculating spending for $monthYearStr")
        
        // Observe monthly expenses
        transactionViewModel.getMonthlyExpenses(currentYear, currentMonth).observe(this) { expenses ->
            val totalSpending = expenses.sumOf { it.amount }
            Log.d(TAG, "Current month spending: $totalSpending")
            
            // Format currency
            val formatter = NumberFormat.getCurrencyInstance().apply {
                currency = Currency.getInstance("LKR")
            }
            
            // Update UI with current spending
            binding.currentSpendingDisplay.text = formatter.format(totalSpending)
            
            // Get budget limit for progress calculation
            userSettingsViewModel.userSettings.value?.let { settings ->
                val budgetLimit = settings.monthlyBudget
                if (budgetLimit > 0) {
                    val percentage = (totalSpending / budgetLimit) * 100
                    binding.budgetProgressBar.progress = percentage.toInt().coerceAtMost(100)
                    
                    // Update color based on percentage
                    val progressColor = when {
                        percentage >= 90 -> ContextCompat.getColor(this, R.color.expense_red)
                        percentage >= 75 -> ContextCompat.getColor(this, R.color.warning_orange)
                        else -> ContextCompat.getColor(this, R.color.income_green)
                    }
                    binding.budgetProgressBar.progressTintList = ColorStateList.valueOf(progressColor)
                    
                    // Show remaining budget
                    val remaining = (budgetLimit - totalSpending).coerceAtLeast(0.0)
                    binding.remainingBudgetDisplay.text = formatter.format(remaining)
                    binding.remainingBudgetDisplay.setTextColor(progressColor)
                    
                    // Display percentage
                    binding.budgetPercentageDisplay.text = String.format("%.1f%%", percentage)
                } else {
                    binding.budgetProgressBar.progress = 0
                    binding.remainingBudgetDisplay.text = "No budget set"
                    binding.budgetPercentageDisplay.text = "0%"
                }
            }
        }
    }
    
    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_budget

        // Set up listener for navigation item clicks
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }                R.id.nav_expense_analysis -> {
                    startActivity(Intent(this, CategoryAnalysisActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_income_analysis -> {
                    startActivity(Intent(this, IncomeAnalysisActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_budget -> {
                    // Already on budget page
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            // Just finish this activity and go back to previous screen
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, BudgetActivity::class.java)
        }
    }
} 