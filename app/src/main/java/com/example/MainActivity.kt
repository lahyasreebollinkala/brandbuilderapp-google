package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.BrandCampaignRepository
import com.example.ui.BrandApp
import com.example.ui.BrandViewModel
import com.example.ui.BrandViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Local SQLite persistence initialization
        val database = AppDatabase.getDatabase(this)
        val repository = BrandCampaignRepository(database.brandCampaignDao())

        // ViewModel instantiation via factory
        val factory = BrandViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[BrandViewModel::class.java]

        setContent {
            MyApplicationTheme {
                BrandApp(viewModel = viewModel)
            }
        }
    }
}
