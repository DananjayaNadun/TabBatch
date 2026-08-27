@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tabbatch.app.ui.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tabbatch.app.ui.AppViewModel

@Composable
fun GroupScreen(
    viewModel: AppViewModel,
    domain: String,
    onBack: () -> Unit,
    onExport: () -> Unit,
) {
    val collection by viewModel.collection.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val group = collection?.groups?.firstOrNull { it.displayName == domain }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$domain — ${group?.count ?: 0} tabs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (group == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text("Group not found.")
            }
            return@Scaffold
        }

        val duplicateIds = collection?.duplicateInfo?.duplicateRecordIds.orEmpty()

        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Button(onClick = { viewModel.selectAll(group.records.map { it.id }) }) {
                    Text("Select All")
                }
                Button(onClick = { viewModel.clearSelection() }) { Text("Clear") }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(group.records, key = { it.id }) { record ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.Top,
                    ) {
                        Checkbox(
                            checked = record.id in selectedIds,
                            onCheckedChange = { viewModel.toggleSelected(record.id) },
                        )
                        Column {
                            Text(
                                record.title ?: record.url,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(record.url, style = MaterialTheme.typography.bodyMedium)
                            if (record.id in duplicateIds) {
                                Text(
                                    "Duplicate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }

            Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Text("Export Group")
            }
        }
    }
}
