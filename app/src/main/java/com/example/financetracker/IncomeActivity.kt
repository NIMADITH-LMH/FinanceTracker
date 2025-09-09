package com.example.financetracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Redirect class that forwards to IncomeAnalysisActivity
 * This is for backward compatibility with code that references IncomeActivity
 */
class IncomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Forward to IncomeAnalysisActivity
        val intent = Intent(this, IncomeAnalysisActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_FORWARD_RESULT
        
        // Pass along any extras
        if (intent.extras != null) {
            intent.putExtras(intent)
        }
        
        startActivity(intent)
        finish()
    }
}
