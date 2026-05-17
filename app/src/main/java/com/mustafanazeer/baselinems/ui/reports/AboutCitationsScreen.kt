package com.mustafanazeer.baselinems.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mustafanazeer.baselinems.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutCitationsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.phase9_about_citations_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.phase9_about_back_content_description)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.phase9_about_section_trends_heading),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.phase9_about_section_trends_body),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = stringResource(R.string.phase9_about_section_quality_heading),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.phase9_about_section_quality_body),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = stringResource(R.string.phase9_about_section_privacy_heading),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.phase9_about_section_privacy_body),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = stringResource(R.string.phase9_about_section_disclaimer_heading),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.phase9_about_section_disclaimer_body),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
