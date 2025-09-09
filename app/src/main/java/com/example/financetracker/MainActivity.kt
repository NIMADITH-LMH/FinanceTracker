package com.example.financetracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.widget.Toolbar
import com.example.financetracker.adapter.TransactionAdapter
import com.example.financetracker.model.Transaction
import com.example.financetracker.model.TransactionType
import com.example.financetracker.util.NotificationHelper
import com.example.financetracker.util.PreferencesManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import java.text.NumberFormat
import java.util.*
import com.example.financetracker.data.DataMigration
import com.example.financetracker.data.FinanceDatabase
import com.example.financetracker.viewmodel.TransactionViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.financetracker.data.TransactionEntity
import com.example.financetracker.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull

class MainActivity : AppCompatActivity(), TransactionAdapter.TransactionClickListener {
    private val TAG = "MainActivity"
    
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var transactionsRecyclerView: RecyclerView
    private lateinit var binding: ActivityMainBinding
    private lateinit var transactionViewModel: TransactionViewModel
    
    // Budget views
    private lateinit var budgetAmount: TextView
    private lateinit var budgetProgress: ProgressBar
    private lateinit var remainingBalance: TextView
    
    // Income views
    private lateinit var monthlySalaryAmount: TextView
    private lateinit var additionalIncomeAmount: TextView
    private lateinit var totalMoneyToSpend: TextView
    
    private var monthlyBudget: Double = 0.0
    private var currentSpending: Double = 0.0
    private var monthlySalary: Double = 0.0
    private var additionalIncome: Double = 0.0

    private val defaultCategories = listOf(
        "Food", "Transport", "Bills", "Entertainment", "Shopping", 
        "Health", "Education", "Salary", "Investment", "Other"
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            showPermissionRationale()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Initialize core components
            preferencesManager = PreferencesManager(this)
            notificationHelper = NotificationHelper(this)
            transactionViewModel = ViewModelProvider(this)[TransactionViewModel::class.java]

            // Set up toolbar with NoActionBar theme
            val toolbar = binding.toolbar
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            
            // Check notification permission
            checkNotificationPermission()
            
            // Initialize UI
            initializeViews()
            
            // Get monthly budget and salary
            monthlyBudget = preferencesManager.getMonthlyBudget()
            monthlySalary = preferencesManager.getMonthlySalary()
            
            // Setup RecyclerView
            setupRecyclerView()

            // Initialize database and migrate data if needed - do this after UI is set up
            lifecycleScope.launch {
                try {
                    // Initialize database
                    FinanceDatabase.getDatabase(applicationContext)
                    
                    // Test database functionality - do this after database is initialized
                    testDatabaseFunctionality()
                    
                    // Clean up any test transactions from previous runs
                    removeTestTransactions()
                    
                    // Migrate data from PreferencesManager to Room
                    val dataMigration = DataMigration(applicationContext)
                    dataMigration.migrateData()
                    
                    // Load initial data after migration is complete
                    loadTransactions()
                } catch (e: Exception) {
                    Log.e(TAG, "Error during database initialization or migration", e)
                    runOnUiThread {
                        showSnackbar("Database initialization error: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical error during app initialization", e)
            Toast.makeText(this, "Error starting app: ${e.message}", Toast.LENGTH_LONG).show()
            // Consider finishing the activity or showing a dialog
            // finish()
        }
    }
    
    private fun initializeViews() {
        try {
            // Budget views
            transactionsRecyclerView = binding.transactionsRecyclerView
            budgetAmount = binding.budgetAmount
            budgetProgress = binding.budgetProgress
            remainingBalance = binding.remainingBalance
            
            // Income views
            monthlySalaryAmount = binding.monthlySalaryAmount
            additionalIncomeAmount = binding.additionalIncomeAmount
            totalMoneyToSpend = binding.totalMoneyToSpend
            
            // Setup the settings button if it exists
            setupSettingsButton()
            
            // Setup bottom navigation
            setupBottomNavigation()
            
            // Setup FAB with improved click handling
            val fab = binding.addTransactionFab
            fab.setOnClickListener { view ->
                // Prevent multiple rapid clicks
                view.isEnabled = false
                try {
                    showTransactionDialog()
                } catch (e: Exception) {
                    Log.e(TAG, "Error showing transaction dialog", e)
                    showSnackbar("Could not open transaction dialog")
                } finally {
                    // Re-enable the button after a short delay
                    view.postDelayed({ view.isEnabled = true }, 300)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            showErrorDialog("Initialization Error", "Could not initialize app views: ${e.message}")
        }
    }
    
    private fun setupSettingsButton() {
        try {
            // The settings button might not exist in the layout
            // Just a placeholder method for now - settings handled by bottom nav
            Log.d(TAG, "Settings button setup (using bottom navigation instead)")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up settings button", e)
        }
    }

    private fun setupBottomNavigation() {
        try {
            val bottomNavigation = binding.bottomNavigation
            
            // Set the home item as selected
            bottomNavigation.selectedItemId = R.id.nav_home
            
            // Set up navigation item selection listener
            bottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        // Already on home screen
                        true
                    }
                    R.id.nav_expense_analysis -> {
                        try {
                            startActivity(Intent(this, CategoryAnalysisActivity::class.java))
                        } catch (e: Exception) {
                            Log.e(TAG, "Error navigating to Expense Analysis", e)
                            showSnackbar("Could not open expense analysis")
                        }
                        true
                    }
                    R.id.nav_income_analysis -> {
                        try {
                            startActivity(Intent(this, IncomeAnalysisActivity::class.java))
                        } catch (e: Exception) {
                            Log.e(TAG, "Error navigating to Income Analysis", e)
                            showSnackbar("Could not open income analysis")
                        }
                        true
                    }
                    R.id.nav_budget -> {
                        try {
                            startActivity(Intent(this, BudgetActivity::class.java))
                        } catch (e: Exception) {
                            Log.e(TAG, "Error navigating to Budget", e)
                            showSnackbar("Could not open budget settings")
                        }
                        true
                    }
                    R.id.nav_settings -> {
                        // Use the direct method 
                        openSettingsDirectly()
                        true
                    }
                    else -> false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up bottom navigation", e)
        }
    }

    private fun setupRecyclerView() {
        try {
            transactionAdapter = TransactionAdapter(emptyList(), this)
            transactionsRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = transactionAdapter
                setHasFixedSize(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up RecyclerView", e)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission is granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showPermissionRationale()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun showPermissionRationale() {
        Snackbar.make(
            binding.root,
            "Notifications are required for budget alerts and reminders",
            Snackbar.LENGTH_LONG
        ).setAction("Settings") {
            startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
            })
        }.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        try {
            menuInflater.inflate(R.menu.main_menu, menu)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating options menu", e)
            return false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Don't navigate to HomeActivity from MainActivity on up button
                // This fixes the issue with unwanted navigation
                finish()
                true
            }
            R.id.action_category_analysis -> {
                try {
                    startActivity(Intent(this, CategoryAnalysisActivity::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to Expense Analysis", e)
                    showSnackbar("Could not open expense analysis")
                }
                true
            }
            R.id.action_income_analysis -> {
                try {
                    startActivity(Intent(this, IncomeAnalysisActivity::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to Income Analysis", e)
                    showSnackbar("Could not open income analysis")
                }
                true
            }
            R.id.action_monthly_report -> {
                try {
                    startActivity(Intent(this, MonthlyReportActivity::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to Monthly Report", e)
                    showSnackbar("Could not open monthly report")
                }
                true
            }
            R.id.action_settings -> {
                openSettingsDirectly()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun safelyCreateDialog(): AlertDialog.Builder {
        return try {
            MaterialAlertDialogBuilder(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating MaterialAlertDialogBuilder", e)
            AlertDialog.Builder(this)
        }
    }

    private fun showTransactionDialog(transaction: Transaction? = null) {
        try {
            // Check if activity is finishing
            if (isFinishing) {
                Log.e(TAG, "Activity is finishing, cannot show dialog")
                return
            }
            
            // Inflate the dialog layout
            val dialogView = layoutInflater.inflate(R.layout.dialog_transaction, null)
            if (dialogView == null) {
                Log.e(TAG, "Failed to inflate dialog layout")
                showSnackbar("Error creating transaction form")
                return
            }
            
            // Initialize dialog views with null safety checks
            val titleInput = dialogView.findViewById<TextInputEditText>(R.id.titleInput)
            val amountInput = dialogView.findViewById<TextInputEditText>(R.id.amountInput)
            val categoryInput = dialogView.findViewById<AutoCompleteTextView>(R.id.categoryInput)
            val typeGroup = dialogView.findViewById<RadioGroup>(R.id.transactionTypeGroup)
            val expenseRadio = dialogView.findViewById<RadioButton>(R.id.expenseRadio)
            val incomeRadio = dialogView.findViewById<RadioButton>(R.id.incomeRadio)

            // Verify all views were found
            if (titleInput == null || amountInput == null || categoryInput == null || 
                typeGroup == null || expenseRadio == null || incomeRadio == null) {
                Log.e(TAG, "Failed to find all views in dialog layout")
                showSnackbar("Error loading transaction form")
                return
            }

            // Setup category autocomplete
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, defaultCategories)
            categoryInput.setAdapter(adapter)

            // If editing, populate fields
            transaction?.let {
                titleInput.setText(it.title)
                // Format the amount properly
                amountInput.setText(String.format(Locale.US, "%.2f", it.amount))
                categoryInput.setText(it.category)
                if (it.type == TransactionType.INCOME) {
                    incomeRadio.isChecked = true
                } else {
                    expenseRadio.isChecked = true
                }
            }

            // Build and show the dialog
            val dialogBuilder = safelyCreateDialog()
                .setTitle(if (transaction == null) "Add Transaction" else "Edit Transaction")
                .setView(dialogView)
                .setPositiveButton("Save") { dialog, _ ->
                    val title = titleInput.text.toString().trim()
                    val amountText = amountInput.text.toString().replace(",", "")
                    val category = categoryInput.text.toString().trim()
                    val type = if (expenseRadio.isChecked) "EXPENSE" else "INCOME"

                    if (validateInput(title, amountText, category)) {
                        try {
                            val amount = amountText.toDouble()
                            val transactionEntity = TransactionEntity(
                                id = transaction?.id ?: 0,
                                title = title,
                                amount = amount,
                                category = category,
                                type = type,
                                date = transaction?.date ?: Date(),
                                description = null
                            )

                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    if (transaction == null) {
                                        transactionViewModel.insertTransaction(transactionEntity)
                                        runOnUiThread {
                                            showSnackbar("Transaction added successfully")
                                        }
                                    } else {
                                        transactionViewModel.updateTransaction(transactionEntity)
                                        runOnUiThread {
                                            showSnackbar("Transaction updated successfully")
                                        }
                                    }
                                    loadTransactions()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error saving transaction", e)
                                    runOnUiThread {
                                        showSnackbar("Error saving transaction: ${e.message}")
                                    }
                                }
                            }
                        } catch (e: NumberFormatException) {
                            Log.e(TAG, "Number format error", e)
                            showSnackbar("Please enter a valid amount")
                        }
                    }
                }
                .setNegativeButton("Cancel", null)

            // Add delete button if editing an existing transaction
            if (transaction != null) {
                dialogBuilder.setNeutralButton("Delete") { _, _ ->
                    deleteTransaction(transaction)
                }
            }

            // Show the dialog
            try {
                val dialog = dialogBuilder.create()
                dialog.show()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing dialog", e)
                showSnackbar("Could not display transaction dialog")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing transaction dialog", e)
            showErrorDialog("Transaction Error", "Could not open transaction details: ${e.message}")
        }
    }

    private fun validateInput(title: String, amount: String, category: String): Boolean {
        when {
            title.isEmpty() -> {
                showSnackbar("Please enter a title")
                return false
            }
            amount.isEmpty() -> {
                showSnackbar("Please enter an amount")
                return false
            }
            category.isEmpty() -> {
                showSnackbar("Please select a category")
                return false
            }
        }
        return true
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun deleteTransaction(transaction: Transaction) {
        try {
            // Use the safelyCreateDialog method for consistency
            safelyCreateDialog()
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction?")
                .setPositiveButton("Delete") { dialog, _ ->
                    try {
                        // Delete the transaction using Room
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val transactionEntity = TransactionEntity(
                                    id = transaction.id,
                                    title = transaction.title,
                                    amount = transaction.amount,
                                    category = transaction.category,
                                    type = if (transaction.type == TransactionType.EXPENSE) "EXPENSE" else "INCOME",
                                    date = transaction.date,
                                    description = null
                                )
                                transactionViewModel.deleteTransaction(transactionEntity)
                                runOnUiThread {
                                    showSnackbar("Transaction deleted")
                                    loadTransactions()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error deleting transaction", e)
                                runOnUiThread {
                                    showSnackbar("Error: Could not delete transaction")
                                }
                            }
                        }
                        // Dismiss the dialog to prevent issues
                        dialog.dismiss()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting transaction", e)
                        showSnackbar("Error: Could not delete transaction")
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ -> 
                    // Explicitly dismiss dialog when canceled
                    dialog.dismiss() 
                }
                .create()
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing delete confirmation", e)
            showSnackbar("Could not open delete confirmation")
        }
    }

    private fun updateBudgetDisplay() {
        val numberFormat = NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance("LKR")
        }

        // Update budget information
        budgetAmount.text = String.format("%s / %s", 
            numberFormat.format(currentSpending),
            numberFormat.format(monthlyBudget)
        )

        // Update monthly salary and income information
        monthlySalaryAmount.text = numberFormat.format(monthlySalary)
        additionalIncomeAmount.text = numberFormat.format(additionalIncome)
        
        // Calculate total money to spend and remaining balance
        val totalToSpend = monthlySalary + additionalIncome
        val remaining = totalToSpend - currentSpending
        
        totalMoneyToSpend.text = numberFormat.format(totalToSpend)
        remainingBalance.text = numberFormat.format(remaining)
        
        // Update progress bar
        if (monthlyBudget > 0) {
            val progress = ((currentSpending / monthlyBudget) * 100).toInt().coerceIn(0, 100)
            budgetProgress.progress = progress
            
            // Check if we need to show a warning
            if (currentSpending >= monthlyBudget * 0.9) {
                showBudgetWarning()
            }
        }
    }

    private fun showBudgetWarning() {
        if (preferencesManager.getBudgetAlertsEnabled()) {
            notificationHelper.showBudgetWarning(currentSpending, monthlyBudget)
        }
    }

    private fun loadTransactions() {
        try {
            lifecycleScope.launch {
                try {
                    transactionViewModel.getAllTransactions().collect { transactions ->
                        try {
                            val transactionList = transactions.map { entity ->
                                Transaction(
                                    id = entity.id,
                                    title = entity.title,
                                    amount = entity.amount,
                                    category = entity.category,
                                    date = entity.date,
                                    type = if (entity.type == "EXPENSE") TransactionType.EXPENSE else TransactionType.INCOME
                                )
                            }
                            
                            transactionAdapter.updateTransactions(transactionList)
                            
                            // Calculate current spending (expenses)
                            currentSpending = transactions
                                .filter { it.type == "EXPENSE" }
                                .sumOf { it.amount }
                                
                            // Calculate additional income (not including salary)
                            additionalIncome = transactions
                                .filter { it.type == "INCOME" }
                                .sumOf { it.amount }
                                
                            updateBudgetDisplay()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing transactions", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error collecting transactions flow", e)
                    runOnUiThread {
                        showSnackbar("Error loading transactions: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading transactions", e)
            showSnackbar("Error loading transactions")
        }
    }

    override fun onTransactionClick(transaction: Transaction) {
        try {
            // Show transaction dialog with the selected transaction
            showTransactionDialog(transaction)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling transaction click", e)
            showSnackbar("Could not open transaction details")
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload data in case it was modified in settings
        monthlyBudget = preferencesManager.getMonthlyBudget()
        monthlySalary = preferencesManager.getMonthlySalary()
        loadTransactions()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putDouble("monthlyBudget", monthlyBudget)
        outState.putDouble("currentSpending", currentSpending)
        outState.putDouble("monthlySalary", monthlySalary)
        outState.putDouble("additionalIncome", additionalIncome)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        monthlyBudget = savedInstanceState.getDouble("monthlyBudget")
        currentSpending = savedInstanceState.getDouble("currentSpending")
        monthlySalary = savedInstanceState.getDouble("monthlySalary")
        additionalIncome = savedInstanceState.getDouble("additionalIncome")
        updateBudgetDisplay()
    }

    override fun onBackPressed() {
        if (isTaskRoot) {
            moveTaskToBack(false)
        } else {
            super.onBackPressed()
        }
    }

    private fun showErrorDialog(title: String, message: String) {
        try {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .setIcon(android.R.drawable.ic_dialog_alert)
            
            val dialog = builder.create()
            dialog.show()
        } catch (e: Exception) {
            // If even showing the dialog fails, fall back to Toast
            Toast.makeText(this, "$title: $message", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Failed to show error dialog", e)
        }
    }

    private fun launchSettingsActivity() {
        try {
            startActivity(Intent(this, SettingsActivity::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Settings: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSettingsDirectly() {
        try {
            // Open the actual SettingsActivity
            startActivity(Intent(this, SettingsActivity::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open settings: ${e.message}")
            Toast.makeText(this, "Could not open settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun testDatabaseFunctionality() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Only check if database is accessible, don't add test transactions
                try {
                    val transactions = transactionViewModel.getAllTransactions().firstOrNull() ?: emptyList()
                    Log.d(TAG, "Database check: Found ${transactions.size} transactions in database")
                } catch (e: Exception) {
                    Log.e(TAG, "Error accessing database", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Database test failed", e)
            }
        }
    }

    private fun removeTestTransactions() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Find and delete test transactions
                val transactions = transactionViewModel.getAllTransactions().firstOrNull() ?: emptyList()
                for (transaction in transactions) {
                    if (transaction.title == "Test Transaction" && transaction.category == "Test Category") {
                        transactionViewModel.deleteTransaction(transaction)
                        Log.d(TAG, "Deleted test transaction: ${transaction.id}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing test transactions", e)
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }
}