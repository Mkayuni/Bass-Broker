package com.mkayuni.bassbroker.ui.stocks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.mkayuni.bassbroker.model.Stock

@Composable
fun StockList(
    stocks: List<Stock>,
    onStockClick: (Stock) -> Unit
) {
    LazyColumn {
        items(stocks) { stock ->
            StockItem(stock = stock, onClick = { onStockClick(stock) })
            HorizontalDivider()
        }
    }
}