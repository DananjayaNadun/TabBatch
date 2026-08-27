@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tabbatch.app.ui.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tabbatch.app.domain.export.ExportFormat
import com.tabbatch.app.domain.export.ExportOptions
import com.tabbatch.app.platform.sharing.FileShareHelper
import com.tabbatch.app.ui.AppViewModel
import com.tabbatch.app.ui.ExportUiState

@Composable
fun ExportScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val exportState by viewModel.exportState.collectAsState()

    var format by remember { mutableStateOf(ExportFormat.Pdf) }
    var includeTitles by remember { mutableStateOf(true) }
    var includeUrls by remember { mutableStateOf(true) }
    var includeDuplicates by remember { mutableStateOf(true) }
    var includeOriginalUrls by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Export Collection") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Format", style = MaterialTheme.typography.titleMedium)
            listOf(
                ExportFormat.Pdf to "PDF",
                ExportFormat.Csv to "CSV",
                ExportFormat.Json to "JSON",
                ExportFormat.Text to "Text",
            ).forEach { (value, label) ->
                Row {
                    RadioButton(selected = format == value, onClick = { format = value })
                    Text(label, modifier = Modifier.padding(top = 12.dp))
                }
            }

            Text("Include", style = MaterialTheme.typography.titleMedium)
            LabeledCheckbox("Titles", includeTitles) { includeTitles = it }
            LabeledCheckbox("URLs", includeUrls) { includeUrls = it }
            LabeledCheckbox("Duplicate URLs", includeDuplicates) { includeDuplicates = it }
            LabeledCheckbox("Original URLs", includeOriginalUrls) { includeOriginalUrls = it }

            Button(
                onClick = {
                    val options = ExportOptions(includeTitles, includeUrls, includeDuplicates, includeOriginalUrls)
                    viewModel.generateExport(format, options)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Generate Export")
            }

            when (val state = exportState) {
                is ExportUiState.Generating -> {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text("Generating…")
                    }
                }
                is ExportUiState.Success -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Export ready: ${state.fileName}", style = MaterialTheme.typography.titleMedium)
                            Button(
                                onClick = {
                                    val file = viewModel.lastExportedFile ?: return@Button
                                    val intent = FileShareHelper.shareIntentFor(context, file, state.mimeType)
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            ) { Text("Share") }
                        }
                    }
                }
                is ExportUiState.Failed -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Export failed", style = MaterialTheme.typography.titleMedium)
                            Text(state.message)
                        }
                    }
                }
                is ExportUiState.Idle -> Unit
            }
        }
    }

    LaunchedEffect(Unit) { viewModel.resetExportState() }
}

@Composable
private fun LabeledCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}
