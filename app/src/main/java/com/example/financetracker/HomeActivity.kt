package com.example.financetracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class HomeActivity : AppCompatActivity() {
    private val TAG = "HomeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_home)
            
            // Set up the toolbar with NoActionBar theme
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            if (toolbar != null) {
                setSupportActionBar(toolbar)
                supportActionBar?.setDisplayHomeAsUpEnabled(false) // No back button on home
            }
            
            // Set up the Get Started button
            val getStartedButton = findViewById<Button>(R.id.getStartedButton)
            getStartedButton.setOnClickListener {
                try {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start MainActivity", e)
                    showErrorDialog("Navigation Error", "Unable to start the main app: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            showErrorDialog("Initialization Error", "Error initializing home screen: ${e.message}")
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
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        try {
            menuInflater.inflate(R.menu.home_menu, menu)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating options menu", e)
            showErrorDialog("Menu Error", "Error creating options menu: ${e.message}")
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                try {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to Settings", e)
                    showErrorDialog("Navigation Error", "Could not open settings: ${e.message}")
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 