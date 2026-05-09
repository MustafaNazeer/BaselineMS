package com.mustafan4x.baselinems.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mustafan4x.baselinems.data.UserProfileDao
import com.mustafan4x.baselinems.data.UserProfileEntity
import com.mustafan4x.baselinems.ui.common.displayLabel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userProfileDao: UserProfileDao,
    onEditProfile: (() -> Unit)? = null
) {
    var profile by remember { mutableStateOf<UserProfileEntity?>(null) }
    LaunchedEffect(Unit) { profile = userProfileDao.getFirst() }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Profile", style = MaterialTheme.typography.titleMedium)
            val p = profile
            if (p == null) {
                Text("No profile yet.")
            } else {
                Text("Year of birth: ${yearOf(p.dateOfBirthEpochMs)}")
                Text("Sex: ${p.biologicalSex.displayLabel()}")
                Text("Dominant hand: ${p.dominantHand.displayLabel()}")
                Text("Height: ${p.heightCm?.toInt() ?: "Not provided"} cm")
                Text("MS type: ${p.msTypeDisclosed.displayLabel()}")
                if (onEditProfile != null) {
                    TextButton(
                        onClick = onEditProfile,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Edit profile") }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("About", style = MaterialTheme.typography.titleMedium)
            Text(
                "BaselineMS is not a medical device. " +
                    "It does not diagnose or treat any condition.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private fun yearOf(epochMs: Long): Int {
    val cal = Calendar.getInstance()
    cal.timeInMillis = epochMs
    return cal.get(Calendar.YEAR)
}
