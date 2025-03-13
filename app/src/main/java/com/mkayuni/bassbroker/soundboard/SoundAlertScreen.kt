package com.mkayuni.bassbroker.ui.soundboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mkayuni.bassbroker.model.SoundEffect
import com.mkayuni.bassbroker.model.SoundType
import com.mkayuni.bassbroker.util.SoundPlayer
import androidx.compose.ui.platform.LocalContext

@Composable
fun SoundAlertDialog(
    stockSymbol: String,
    isHighAlert: Boolean,
    currentThreshold: Double?,
    onDismiss: () -> Unit,
    onSave: (Double, Int) -> Unit
) {
    var threshold by remember { mutableStateOf(currentThreshold?.toString() ?: "") }
    var selectedSoundId by remember { mutableStateOf(0) }

    // Sample sound effects - in a real app, these would come from a repository
    val soundEffects = listOf(
        SoundEffect(1, "Funky Bass Up", 0, SoundType.PRICE_UP),
        SoundEffect(2, "Bass Drop", 0, SoundType.PRICE_DOWN),
        SoundEffect(3, "Steady Bass", 0, SoundType.PRICE_STABLE),
        SoundEffect(4, "Slap Bass", 0, SoundType.CUSTOM)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set ${if (isHighAlert) "High" else "Low"} Alert for $stockSymbol") },
        text = {
            Column {
                OutlinedTextField(
                    value = threshold,
                    onValueChange = {
                        // Only allow numeric input with decimal point
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            threshold = it
                        }
                    },
                    label = { Text("Price Threshold") },
                    prefix = { Text("$") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                Text(
                    "Select Sound Effect",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(soundEffects) { sound ->
                        SoundEffectItem(
                            sound = sound,
                            isSelected = sound.id == selectedSoundId,
                            onClick = { selectedSoundId = sound.id }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    threshold.toDoubleOrNull()?.let { thresholdValue ->
                        onSave(thresholdValue, selectedSoundId)
                    }
                },
                enabled = threshold.isNotEmpty() && threshold.toDoubleOrNull() != null && selectedSoundId != 0
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SoundEffectItem(
    sound: SoundEffect,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val soundPlayer = remember { SoundPlayer(context) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )

        Text(
            text = sound.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )

        Button(
            onClick = {
                // Play the sound
                soundPlayer.playSound(sound.type)
            },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text("Play")
        }
    }
}