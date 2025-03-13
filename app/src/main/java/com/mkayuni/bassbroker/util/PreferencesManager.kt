package com.mkayuni.bassbroker.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mkayuni.bassbroker.model.StockAlert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create a DataStore instance at the top level
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    // Keys for storing data
    companion object {
        val STOCK_SYMBOLS = stringSetPreferencesKey("stock_symbols")
        // Other keys will be dynamically created based on stock symbols
    }

    // Save a stock alert
    suspend fun saveStockAlert(alert: StockAlert) {
        context.dataStore.edit { preferences ->
            // First add the stock symbol to our tracked stocks
            val currentSymbols = preferences[STOCK_SYMBOLS]?.toMutableSet() ?: mutableSetOf()
            currentSymbols.add(alert.stockSymbol)
            preferences[STOCK_SYMBOLS] = currentSymbols

            // Save the alert threshold
            val thresholdKey = doublePreferencesKey("${alert.stockSymbol}_${if (alert.isHighAlert) "high" else "low"}_threshold")
            preferences[thresholdKey] = alert.thresholdValue

            // Save the sound effect
            val soundKey = intPreferencesKey("${alert.stockSymbol}_${if (alert.isHighAlert) "high" else "low"}_sound")
            preferences[soundKey] = alert.soundEffectId
        }
    }

    // Get all saved stock symbols
    val savedStockSymbols: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[STOCK_SYMBOLS] ?: emptySet()
    }

    // Get alerts for a specific stock
    fun getStockAlerts(symbol: String): Flow<List<StockAlert>> {
        return context.dataStore.data.map { preferences ->
            val alerts = mutableListOf<StockAlert>()

            // Check for high alert
            val highThresholdKey = doublePreferencesKey("${symbol}_high_threshold")
            val highSoundKey = intPreferencesKey("${symbol}_high_sound")

            if (preferences.contains(highThresholdKey) && preferences.contains(highSoundKey)) {
                alerts.add(
                    StockAlert(
                        stockSymbol = symbol,
                        thresholdValue = preferences[highThresholdKey] ?: 0.0,
                        isHighAlert = true,
                        soundEffectId = preferences[highSoundKey] ?: 0
                    )
                )
            }

            // Check for low alert
            val lowThresholdKey = doublePreferencesKey("${symbol}_low_threshold")
            val lowSoundKey = intPreferencesKey("${symbol}_low_sound")

            if (preferences.contains(lowThresholdKey) && preferences.contains(lowSoundKey)) {
                alerts.add(
                    StockAlert(
                        stockSymbol = symbol,
                        thresholdValue = preferences[lowThresholdKey] ?: 0.0,
                        isHighAlert = false,
                        soundEffectId = preferences[lowSoundKey] ?: 0
                    )
                )
            }

            alerts
        }
    }
}