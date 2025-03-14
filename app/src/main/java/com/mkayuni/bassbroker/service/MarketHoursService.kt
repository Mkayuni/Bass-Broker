// Create as Class
package com.mkayuni.bassbroker.service

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class MarketHoursService {
    private val eastCoastZone = ZoneId.of("America/New_York")

    fun isMarketOpen(): Boolean {
        val now = ZonedDateTime.now(eastCoastZone)
        val dayOfWeek = now.dayOfWeek

        // Check if it's weekend
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false
        }

        val hour = now.hour
        val minute = now.minute
        val currentTimeInMinutes = hour * 60 + minute

        // Market hours: 9:30 AM - 4:00 PM Eastern
        val marketOpenInMinutes = 9 * 60 + 30
        val marketCloseInMinutes = 16 * 60

        return currentTimeInMinutes in marketOpenInMinutes until marketCloseInMinutes
    }

    fun getMarketStatus(): MarketStatus {
        val now = ZonedDateTime.now(eastCoastZone)
        val dayOfWeek = now.dayOfWeek

        // Check if it's weekend
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return MarketStatus.CLOSED_WEEKEND
        }

        val hour = now.hour
        val minute = now.minute
        val currentTimeInMinutes = hour * 60 + minute

        val preMarketOpenInMinutes = 4 * 60  // 4:00 AM
        val marketOpenInMinutes = 9 * 60 + 30 // 9:30 AM
        val marketCloseInMinutes = 16 * 60  // 4:00 PM
        val afterHoursCloseInMinutes = 20 * 60  // 8:00 PM

        return when {
            currentTimeInMinutes < preMarketOpenInMinutes -> MarketStatus.CLOSED
            currentTimeInMinutes < marketOpenInMinutes -> MarketStatus.PRE_MARKET
            currentTimeInMinutes < marketCloseInMinutes -> MarketStatus.OPEN
            currentTimeInMinutes < afterHoursCloseInMinutes -> MarketStatus.AFTER_HOURS
            else -> MarketStatus.CLOSED
        }
    }

    fun isMarketOpeningInMinutes(minutes: Int): Boolean {
        val now = ZonedDateTime.now(eastCoastZone)
        val marketOpen = now.withHour(9).withMinute(30).withSecond(0)

        val diffInMinutes = (marketOpen.toEpochSecond() - now.toEpochSecond()) / 60

        return diffInMinutes in 0..minutes
    }

    fun isMarketClosingInMinutes(minutes: Int): Boolean {
        val now = ZonedDateTime.now(eastCoastZone)
        val marketClose = now.withHour(16).withMinute(0).withSecond(0)

        val diffInMinutes = (marketClose.toEpochSecond() - now.toEpochSecond()) / 60

        return diffInMinutes in 0..minutes
    }
}

enum class MarketStatus {
    OPEN,
    CLOSED,
    PRE_MARKET,
    AFTER_HOURS,
    CLOSED_WEEKEND
}