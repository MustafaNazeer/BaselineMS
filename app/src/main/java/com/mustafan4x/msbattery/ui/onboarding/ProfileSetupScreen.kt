package com.mustafan4x.msbattery.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mustafan4x.msbattery.data.Hand
import com.mustafan4x.msbattery.data.MSType
import com.mustafan4x.msbattery.data.Sex
import com.mustafan4x.msbattery.data.UserProfileDao
import com.mustafan4x.msbattery.data.UserProfileEntity
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
    var heightCmText by remember { mutableStateOf("170") }
    var dobYearText by remember { mutableStateOf("1995") }

    Scaffold(topBar = { TopAppBar(title = { Text("Set up profile") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = dobYearText,
                onValueChange = { dobYearText = it },
                label = { Text("Year of birth") }
            )
            OutlinedTextField(
                value = heightCmText,
                onValueChange = { heightCmText = it },
                label = { Text("Height (cm)") }
            )
            EnumDropdown("Biological sex", Sex.values().toList(), sex) { sex = it }
            EnumDropdown("Dominant hand", Hand.values().toList(), hand) { hand = it }
            EnumDropdown("MS type (optional)", MSType.values().toList(), msType) { msType = it }
            Button(onClick = {
                scope.launch {
                    val year = dobYearText.toIntOrNull() ?: 1995
                    val cal = java.util.Calendar.getInstance().apply { set(year, 0, 1) }
                    userProfileDao.insert(
                        UserProfileEntity(
                            dateOfBirthEpochMs = cal.timeInMillis,
                            biologicalSex = sex,
                            dominantHand = hand,
                            msTypeDisclosed = msType,
                            heightCm = heightCmText.toDoubleOrNull() ?: 170.0
                        )
                    )
                    onComplete()
                }
            }) {
                Text("Save and continue")
            }
        }
    }
}

@Composable
private fun <T : Enum<T>> EnumDropdown(
    label: String,
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text("$label: ${selected.name}")
        Button(onClick = { expanded = true }) { Text("Change") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option.name) }, onClick = {
                    onSelected(option); expanded = false
                })
            }
        }
    }
}
