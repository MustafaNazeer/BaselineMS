package com.mustafan4x.msbattery.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mustafan4x.msbattery.data.UserProfileDao
import com.mustafan4x.msbattery.data.UserProfileEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(userProfileDao: UserProfileDao) {
    var profile by remember { mutableStateOf<UserProfileEntity?>(null) }
    LaunchedEffect(Unit) { profile = userProfileDao.getFirst() }
    val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Text("Profile", style = MaterialTheme.typography.titleMedium)
            val p = profile
            if (p == null) {
                Text("No profile yet.")
            } else {
                Text("Date of birth: ${df.format(Date(p.dateOfBirthEpochMs))}")
                Text("Sex: ${p.biologicalSex.name}")
                Text("Dominant hand: ${p.dominantHand.name}")
                Text("Height: ${p.heightCm.toInt()} cm")
                Text("MS type: ${p.msTypeDisclosed.name}")
            }
            Text(" ")
            Text("About", style = MaterialTheme.typography.titleMedium)
            Text(
                "MS Neuro Battery is not a medical device. It does not diagnose or treat any condition.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
