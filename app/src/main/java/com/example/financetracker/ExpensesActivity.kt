package com.example.financetracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Redirect class that forwards to CategoryAnalysisActivity
 * This is for backward compatibility with code that references ExpensesActivity
 */
class ExpensesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Forward to CategoryAnalysisActivity
        val intent = Intent(this, CategoryAnalysisActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_FORWARD_RESULT
        
        // Pass along any extras
        if (intent.extras != null) {
            intent.putExtras(intent)
        }
        
        startActivity(intent)
        finish()
    }
}
