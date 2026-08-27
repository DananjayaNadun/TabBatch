@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tabbatch.app.ui.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tabbatch.app.domain.model.DomainGroup
import com.tabbatch.app.ui.AppViewModel

@Composable
fun CollectionScreen(
    viewModel: AppViewModel,
    onGroupSelected: (String) -> Unit,
    onExport: () -> Unit,
    onBackToHome: () -> Unit,
) {
    val collection by viewModel.collection.collectAsState()
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text(collection?.name ?: "Collection") }) },
    ) { padding ->
        if (collection == null || collection!!.records.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("No collection loaded yet.", style = MaterialTheme.typography.titleMedium)
                Text("Import URLs from the Home screen to get started.")
            }
            return@Scaffold
        }

        val col = collection!!
        val filteredRecords = if (query.isBlank()) {
            col.records
        } else {
            col.records.filter {
                it.url.contains(query, ignoreCase = true) || it.title?.contains(query, ignoreCase = true) == true
            }
        }
        val groups = if (query.isBlank()) col.groups else DomainGroup.groupRecords(filteredRecords)

        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                "${col.totalCount} tabs   ${col.groups.size} groups   ${col.duplicateInfo.duplicateCount} duplicates",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "${col.duplicateInfo.uniqueCount} unique",
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search tabs…") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(groups, key = { it.displayName }) { group ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { onGroupSelected(group.displayName) },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(group.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(group.count.toString(), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = onExport, modifier = Modifier.weight(1f)) { Text("Export") }
            }
        }
    }
}
