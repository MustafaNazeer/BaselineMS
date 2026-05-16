package com.mustafanazeer.baselinems.ui.settings

import android.app.KeyguardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mustafanazeer.baselinems.R
import com.mustafanazeer.baselinems.battery.sdmt.SdmtSettings
import com.mustafanazeer.baselinems.battery.voice.VoiceSettings
import com.mustafanazeer.baselinems.data.UserProfileDao
import com.mustafanazeer.baselinems.data.UserProfileEntity
import com.mustafanazeer.baselinems.ui.common.displayLabel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userProfileDao: UserProfileDao,
    onEditProfile: (() -> Unit)? = null,
    deviceSecureProvider: (Context) -> Boolean = ::defaultDeviceSecureProvider
) {
    val context = LocalContext.current
    var profile by remember { mutableStateOf<UserProfileEntity?>(null) }
    var showSdmtCountdown by remember { mutableStateOf(SdmtSettings.showCountdown(context)) }
    var saveAudio by remember { mutableStateOf(VoiceSettings.saveAudio(context)) }
    val deviceSecure = remember { deviceSecureProvider(context) }
    var showOnDialog by remember { mutableStateOf(false) }
    var showOffDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { profile = userProfileDao.getFirst() }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
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

            Text("Cognitive tests", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.padding(end = 16.dp)) {
                    Text("Show timer during cognitive tests")
                    Text(
                        "Off by default. A visible countdown can be distracting during a paced task.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = showSdmtCountdown,
                    onCheckedChange = { newValue ->
                        showSdmtCountdown = newValue
                        SdmtSettings.setShowCountdown(context, newValue)
                    }
                )
            }
            Spacer(Modifier.height(16.dp))

            Text("Voice test", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.padding(end = 16.dp)) {
                    Text(stringResource(R.string.settings_voice_save_audio_label))
                    Text(
                        stringResource(R.string.settings_voice_save_audio_off_copy),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!deviceSecure) {
                        Text(
                            stringResource(R.string.settings_voice_save_audio_locked_subcopy),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Switch(
                    enabled = deviceSecure,
                    checked = saveAudio,
                    onCheckedChange = { newValue ->
                        if (newValue) showOnDialog = true else showOffDialog = true
                    }
                )
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

    if (showOnDialog) {
        AlertDialog(
            onDismissRequest = { showOnDialog = false },
            title = { Text(stringResource(R.string.settings_voice_save_audio_dialog_on_title)) },
            text = { Text(stringResource(R.string.settings_voice_save_audio_dialog_on_body)) },
            confirmButton = {
                TextButton(onClick = {
                    saveAudio = true
                    VoiceSettings.setSaveAudio(context, true)
                    showOnDialog = false
                }) {
                    Text(stringResource(R.string.settings_voice_save_audio_dialog_on_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showOnDialog = false }) {
                    Text(stringResource(R.string.settings_voice_save_audio_dialog_cancel))
                }
            }
        )
    }
    if (showOffDialog) {
        AlertDialog(
            onDismissRequest = { showOffDialog = false },
            title = { Text(stringResource(R.string.settings_voice_save_audio_dialog_off_title)) },
            confirmButton = {
                TextButton(onClick = {
                    saveAudio = false
                    VoiceSettings.setSaveAudio(context, false)
                    VoiceSettings.deleteAllRetainedAudio(context.filesDir)
                    showOffDialog = false
                }) {
                    Text(stringResource(R.string.settings_voice_save_audio_dialog_off_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showOffDialog = false }) {
                    Text(stringResource(R.string.settings_voice_save_audio_dialog_cancel))
                }
            }
        )
    }
}

private fun defaultDeviceSecureProvider(context: Context): Boolean {
    val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    return km?.isDeviceSecure ?: false
}

private fun yearOf(epochMs: Long): Int {
    val cal = Calendar.getInstance()
    cal.timeInMillis = epochMs
    return cal.get(Calendar.YEAR)
}
