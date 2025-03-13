package com.mkayuni.bassbroker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mkayuni.bassbroker.service.StockMonitorService
import com.mkayuni.bassbroker.ui.stocks.StockListScreen
import com.mkayuni.bassbroker.ui.theme.BassBrokerTheme
import com.mkayuni.bassbroker.viewmodel.StockViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the stock monitoring service
        startService(Intent(this, StockMonitorService::class.java))

        setContent {
            BassBrokerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val stockViewModel: StockViewModel = viewModel()
                    StockListScreen(viewModel = stockViewModel)
                }
            }
        }
    }
}