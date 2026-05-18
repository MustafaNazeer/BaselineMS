package com.mustafanazeer.baselinems.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mustafanazeer.baselinems.data.Hand
import com.mustafanazeer.baselinems.data.MSType
import com.mustafanazeer.baselinems.data.Sex
import com.mustafanazeer.baselinems.data.UserProfileDao
import com.mustafanazeer.baselinems.data.UserProfileEntity
import com.mustafanazeer.baselinems.ui.common.displayLabel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    userProfileDao: UserProfileDao,
    onComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var sex by remember { mutableStateOf(Sex.UNDISCLOSED) }
    var hand by remember { mutableStateOf(Hand.RIGHT) }
    var msType by remember { mutableStateOf(MSType.UNDISCLOSED) }
    var heightCmText by remember { mutableStateOf("") }
    var dobYearText by remember { mutableStateOf("") }

    val yearError = !isPlausibleYear(dobYearText)
    val heightError = !isPlausibleHeightCm(heightCmText)
    val canSave = !yearError && !heightError

    Scaffold(topBar = { TopAppBar(title = { Text("Set up profile (1 of 1)") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = dobYearText,
                onValueChange = { dobYearText = it.filter { ch -> ch.isDigit() }.take(4) },
                label = { Text("Year of birth (for example 1985)") },
                isError = dobYearText.isNotEmpty() && yearError,
                supportingText = {
                    if (dobYearText.isNotEmpty() && yearError) {
                        Text("Please enter a year between 1925 and 2026")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = heightCmText,
                onValueChange = { heightCmText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) },
                label = { Text("Height in cm (for example 168)") },
                isError = heightCmText.isNotEmpty() && heightError,
                supportingText = {
                    if (heightCmText.isNotEmpty() && heightError) {
                        Text("Please enter a height between 100 and 230 cm")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            EnumMenuBox(
                label = "Biological sex",
                options = Sex.values().toList(),
                selected = sex,
                labelOf = { it.displayLabel() },
                onSelected = { sex = it }
            )
            EnumMenuBox(
                label = "Dominant hand",
                options = Hand.values().toList(),
                selected = hand,
                labelOf = { it.displayLabel() },
                onSelected = { hand = it }
            )
            EnumMenuBox(
                label = "MS type (optional)",
                options = MSType.values().toList(),
                selected = msType,
                labelOf = { it.displayLabel() },
                onSelected = { msType = it }
            )
            Text(
                "We use these to personalize your trends. The MS type field is optional.",
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = {
                    val year = dobYearText.toIntOrNull() ?: return@Button
                    val heightCm = heightCmText.toDoubleOrNull() ?: return@Button
                    scope.launch {
                        val cal = java.util.Calendar.getInstance().apply { set(year, 0, 1) }
                        userProfileDao.insert(
                            UserProfileEntity(
                                dateOfBirthEpochMs = cal.timeInMillis,
                                biologicalSex = sex,
                                dominantHand = hand,
                                msTypeDisclosed = msType,
                                heightCm = heightCm
                            )
                        )
                        onComplete()
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save and continue")
            }
            TextButton(
                onClick = {
                    scope.launch {
                        userProfileDao.insert(
                            UserProfileEntity(
                                dateOfBirthEpochMs = 0L,
                                biologicalSex = Sex.UNDISCLOSED,
                                dominantHand = Hand.RIGHT,
                                msTypeDisclosed = MSType.UNDISCLOSED,
                                heightCm = null
                            )
                        )
                        onComplete()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for now (you can fill these in any time from Settings)")
            }
        }
    }
}

private fun isPlausibleYear(text: String): Boolean {
    val n = text.toIntOrNull() ?: return false
    return n in 1925..2026
}

private fun isPlausibleHeightCm(text: String): Boolean {
    val n = text.toDoubleOrNull() ?: return false
    return n in 100.0..230.0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumMenuBox(
    label: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = labelOf(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(labelOf(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
