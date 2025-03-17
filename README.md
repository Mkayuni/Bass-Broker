# Bass Broker

<div align="center">

  ![Bass Broker Logo](https://github.com/user-attachments/assets/8d16e9ed-2a4d-4df4-9843-35d44e3c20ab)

  [![Kotlin](https://img.shields.io/badge/Kotlin-1.8.10-blue.svg)](https://kotlinlang.org)
  [![Compose](https://img.shields.io/badge/Jetpack%20Compose-Latest-green.svg)](https://developer.android.com/jetpack/compose)

  *Listen to your stocks. Feel the market.*
</div>

## Overview

Bass Broker is my passion project that brings together my three loves: software development, finance, and bass guitar. I created this Android app to help me monitor stock movements through bass sounds, turning market data into audio cues that I can recognize while working on other things. It's a practical tool that lets me stay connected to my investments without constantly checking charts, while also giving me a creative outlet to apply my programming skills to real problems that matter to me.

**Note:** This project is still a work in progress. Between balancing my thesis, school coursework, and teaching responsibilities, I haven't had as much time as I'd like to dedicate to it. I'm continuously improving it when time permits.

## Demo

Watch the live demo of Bass Broker in action:

[![Watch the demo](https://img.youtube.com/vi/fhgYDDQGIVk/0.jpg)](https://youtu.be/fhgYDDQGIVk)

## Key Features

- **Audio Stock Monitoring**: Distinctive bass sounds indicate market events
- **Real-time Tracking**: Live data from Yahoo Finance API
- **Interactive Visualizations**: Custom Compose Canvas-based charts
- **Smart Alerts**: Configurable threshold notifications
- **Predictive Analytics**: Statistical forecasting with confidence intervals
- **Background Processing**: Continuous monitoring even when the app is closed

## TensorFlow Prediction Engine

Bass Broker incorporates a dual-approach prediction system to forecast potential price movements:

```kotlin
fun predictPrices(
    symbol: String,
    historicalPrices: List<Double>,
    useTensorFlow: Boolean = true
): PredictionResult {
    // Try TensorFlow model first, fall back to statistical if needed
    if (useTensorFlow) {
        try {
            val tfResult = tensorFlowService.predictPrices(symbol, historicalPrices)
            if (tfResult != null) {
                return PredictionResult(
                    predictedPrices = tfResult.predictedPrices,
                    confidence = tfResult.confidence,
                    modelType = "neural"
                )
            }
        } catch (e: Exception) {
            // Fall back to statistical prediction
        }
    }

    // Statistical fallback uses trend, volatility, and momentum analysis
    return predictPricesStatistical(historicalPrices)
}
```

The app uses a custom-built TensorFlow Lite model trained specifically for a limited set of stocks I personally follow (AAPL, MSFT, GOOGL, NVDA, META, TSLA, etc.). This model analyzes:

- Technical indicators (RSI, MACD)
- Market correlations (S&P 500, VIX)
- Volatility regime detection
- Multi-year trend analysis
- Cyclical market patterns

This is strictly a personal hobby project to help me observe trends and make entry/exit decisions based on 35-day forecasts. The statistical fallback model provides additional context when the neural model isn't available.

## Technologies & Architecture

| Category | Technologies |
|----------|--------------|
| **Frontend** | Jetpack Compose, Material 3 Design |
| **Backend** | Kotlin Coroutines, Flow, StateFlow |
| **Architecture** | MVVM, Repository Pattern, Use Cases |
| **Networking** | Retrofit2, OkHttp3, Kotlin Serialization |
| **Persistence** | DataStore, Room (planned) |
| **Async Processing** | WorkManager, Foreground Services |
| **Visualization** | Compose Canvas, Custom Chart Implementations |
| **Audio** | MediaPlayer, Custom Sound Processing |

## Technical Implementation

### Advanced Sound Pattern Recognition

Bass Broker employs sophisticated algorithms to translate market patterns into distinctive audio cues:

```kotlin
when {
    PatternDetector.isBreakoutPattern(stock, prices) ->
        soundPlayer.playBreakoutSound()
    PatternDetector.isBreakdownPattern(stock, prices) ->
        soundPlayer.playBreakdownSound()
    PatternDetector.isBullishTrend(prices) ->
        soundPlayer.playBullishSound()
    PatternDetector.isBearishTrend(prices) ->
        soundPlayer.playBearishSound()
}
```

### Custom Visualization Engine

The app features a bespoke chart rendering system built with Compose Canvas:

```kotlin
Canvas(modifier = Modifier.fillMaxSize()) {
    // Draw baseline
    // Draw price line
    drawPath(
        path = path,
        color = Color.Blue,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )

    // Draw prediction with confidence
    if (showPrediction) {
        drawPath(
            path = predictionPath,
            color = Color.Red,
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
            )
        )

        // Draw confidence interval
        drawPath(
            path = confidencePath,
            color = Color.Red.copy(alpha = 0.15f)
        )
    }
}
```

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or newer
- Android SDK 24+
- Kotlin 1.8.10+

### Building the Project

1. Clone the repository
```bash
git clone https://github.com/yourusername/bass-broker.git
```

2. Open in Android Studio

3. Configure your API keys (for Yahoo Finance)
```kotlin
// Create local.properties with:
API_KEY=your_api_key_here
```

4. Build and run the app

## Future Enhancements

- [ ] Additional data sources integration
- [ ] Machine learning model for enhanced predictions
- [ ] Portfolio performance analytics
- [ ] Custom widgets for home screen
- [ ] Social sharing features
- [ ] Watchlist categorization

<div align="center">

  **Bass Broker** - Developed by Moses Kayuni

</div>

