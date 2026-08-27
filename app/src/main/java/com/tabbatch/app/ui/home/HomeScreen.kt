package com.tabbatch.app.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tabbatch.app.domain.model.TabSource
import com.tabbatch.app.platform.filepicker.SafFileIO
import com.tabbatch.app.platform.sharing.ClipboardHelper
import com.tabbatch.app.ui.AppViewModel
import com.tabbatch.app.ui.ImportUiState
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    initialSharedText: String?,
    onCollectionReady: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importState by viewModel.importState.collectAsState()
    val collection by viewModel.collection.collectAsState()

    var showPasteDialog by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }

    val openDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = SafFileIO.readText(context, uri).getOrNull() ?: return@launch
            val name = uri.lastPathSegment.orEmpty().lowercase()
            when {
                name.endsWith(".csv") -> viewModel.importCsv(text)
                name.endsWith(".json") -> viewModel.importJson(text)
                else -> viewModel.importText(text, TabSource.TextFile)
            }
        }
    }

    LaunchedEffect(initialSharedText) {
        if (!initialSharedText.isNullOrBlank()) {
            viewModel.importText(initialSharedText, TabSource.SharedText)
        }
    }

    LaunchedEffect(collection) {
        if (collection != null && collection!!.records.isNotEmpty()) onCollectionReady()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("TabBatch", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            Text(
                "Organize. Export. Keep control of your tabs.",
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { openDocumentLauncher.launch(arrayOf("text/*", "application/json")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null)
                    Text("  Import URLs (.txt / .csv / .json)")
                }
                OutlinedButton(
                    onClick = {
                        val text = ClipboardHelper.readClipboardText(context)
                        if (text != null) {
                            pasteText = text
                        }
                        showPasteDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.ContentPaste, contentDescription = null)
                    Text("  Paste from Clipboard")
                }
                OutlinedButton(
                    onClick = { openDocumentLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null)
                    Text("  Open Collection (.json)")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.CloudOff, contentDescription = null)
                    Text(
                        "Live Chrome tab access",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "Not currently available through a supported Chrome Android API. " +
                            "Use Import / Share / Paste instead.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (importState is ImportUiState.Failed) {
                val failed = importState as ImportUiState.Failed
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Import failed",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        )
                        LazyColumn {
                            items(failed.errors.take(10)) { rejection ->
                                Text("• ${rejection.reason.message}", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            if (importState is ImportUiState.PartialSuccess) {
                val partial = importState as ImportUiState.PartialSuccess
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "${partial.acceptedCount} of ${partial.totalCount} imported, " +
                                "${partial.errors.size} invalid",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        )
                        LazyColumn {
                            items(partial.errors.take(10)) { rejection ->
                                Text(
                                    "• ${rejection.rawText.take(60)} — ${rejection.reason.message}",
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        if (partial.errors.size > 10) {
                            Text(
                                "…and ${partial.errors.size - 10} more.",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = { Text("Paste URLs") },
            text = {
                TextField(
                    value = pasteText,
                    onValueChange = { pasteText = it },
                    placeholder = { Text("One URL per line…") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPasteDialog = false
                    viewModel.importText(pasteText, TabSource.Clipboard)
                    pasteText = ""
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) { Text("Cancel") }
            },
        )
    }
}
