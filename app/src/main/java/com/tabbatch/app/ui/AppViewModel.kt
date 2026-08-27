package com.tabbatch.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tabbatch.app.data.export.PdfExporter
import com.tabbatch.app.data.repository.CollectionRepository
import com.tabbatch.app.domain.export.CsvExporter
import com.tabbatch.app.domain.export.ExportFormat
import com.tabbatch.app.domain.export.ExportOptions
import com.tabbatch.app.domain.export.JsonExporter
import com.tabbatch.app.domain.export.TextExporter
import com.tabbatch.app.domain.model.ImportError
import com.tabbatch.app.domain.model.RejectedInput
import com.tabbatch.app.domain.model.TabCollection
import com.tabbatch.app.domain.model.TabRecord
import com.tabbatch.app.domain.model.TabSource
import com.tabbatch.app.domain.parser.CsvImportParser
import com.tabbatch.app.domain.parser.ImportResult
import com.tabbatch.app.domain.parser.JsonImportParser
import com.tabbatch.app.domain.parser.TextImportParser
import com.tabbatch.app.platform.browser.UnsupportedChromeAndroidTabSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ImportUiState {
    data object Idle : ImportUiState()
    data object Loading : ImportUiState()
    data class Failed(val errors: List<RejectedInput>) : ImportUiState()

    /** Import succeeded but some lines/rows were rejected — e.g. "57 of 60 imported, 3
     * invalid" — so the user sees a partial-success summary instead of a silent drop. */
    data class PartialSuccess(val acceptedCount: Int, val totalCount: Int, val errors: List<RejectedInput>) :
        ImportUiState()
}

sealed class ExportUiState {
    data object Idle : ExportUiState()
    data object Generating : ExportUiState()
    data class Success(val fileName: String, val mimeType: String) : ExportUiState()
    data class Failed(val message: String) : ExportUiState()
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CollectionRepository()
    val collection: StateFlow<TabCollection?> = repository.current

    private val _importState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val importState: StateFlow<ImportUiState> = _importState.asStateFlow()

    private val _lastRejections = MutableStateFlow<List<RejectedInput>>(emptyList())
    val lastRejections: StateFlow<List<RejectedInput>> = _lastRejections.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    /** Deliberately unsupported today — see class doc on [UnsupportedChromeAndroidTabSource]. */
    val chromeTabSource = UnsupportedChromeAndroidTabSource()

    var lastExportedFile: java.io.File? = null
        private set
    var lastExportedMime: String = "text/plain"
        private set

    fun importText(text: String, source: TabSource, collectionName: String = "Imported tabs") {
        handleImportResult(TextImportParser.parse(text, source), collectionName)
    }

    fun importCsv(text: String, collectionName: String = "Imported tabs") {
        handleImportResult(CsvImportParser.parse(text), collectionName)
    }

    fun importJson(text: String, collectionName: String = "Imported tabs") {
        handleImportResult(JsonImportParser.parse(text), collectionName)
    }

    private fun handleImportResult(result: ImportResult, collectionName: String) {
        _lastRejections.value = result.rejected
        if (result.accepted.isEmpty()) {
            _importState.value = ImportUiState.Failed(
                result.rejected.ifEmpty { listOf(RejectedInput("", ImportError.EmptyImport)) },
            )
            return
        }
        val existing = repository.current.value
        val merged = if (existing != null) existing.records + result.accepted else result.accepted
        val reindexed = merged.mapIndexed { index, r -> r.copy(order = index) }
        repository.set(
            TabCollection(
                name = existing?.name ?: collectionName,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                records = reindexed,
            ),
        )
        _importState.value = if (result.rejected.isNotEmpty()) {
            ImportUiState.PartialSuccess(
                acceptedCount = result.accepted.size,
                totalCount = result.accepted.size + result.rejected.size,
                errors = result.rejected,
            )
        } else {
            ImportUiState.Idle
        }
        _selectedIds.value = emptySet()
    }

    fun loadCollection(collection: TabCollection) {
        repository.set(collection)
        _selectedIds.value = emptySet()
        _importState.value = ImportUiState.Idle
    }

    fun clearCollection() {
        repository.clear()
        _selectedIds.value = emptySet()
    }

    fun toggleSelected(id: String) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
    }

    fun selectAll(ids: Collection<String>) {
        _selectedIds.value = _selectedIds.value + ids
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun recordsForExport(options: ExportOptions): List<TabRecord> {
        val all = repository.current.value?.records.orEmpty()
        val selected = _selectedIds.value
        val base = if (selected.isEmpty()) all else all.filter { it.id in selected }
        return base
    }

    fun generateExport(format: ExportFormat, options: ExportOptions) {
        val col = repository.current.value
        if (col == null) {
            _exportState.value = ExportUiState.Failed("No collection to export.")
            return
        }
        val records = recordsForExport(options)
        if (records.isEmpty()) {
            _exportState.value = ExportUiState.Failed("Nothing is selected to export.")
            return
        }
        _exportState.value = ExportUiState.Generating
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val timestamp = System.currentTimeMillis()
                when (format) {
                    ExportFormat.Csv -> {
                        val content = CsvExporter.export(records, options)
                        val file = com.tabbatch.app.platform.sharing.FileShareHelper
                            .writeExportFile(app, "tabbatch-export.csv", content)
                        lastExportedFile = file
                        lastExportedMime = "text/csv"
                        _exportState.value = ExportUiState.Success(file.name, lastExportedMime)
                    }
                    ExportFormat.Json -> {
                        val content = JsonExporter.export(col.name, col.createdAt, records, options, timestamp)
                        val file = com.tabbatch.app.platform.sharing.FileShareHelper
                            .writeExportFile(app, "tabbatch-export.json", content)
                        lastExportedFile = file
                        lastExportedMime = "application/json"
                        _exportState.value = ExportUiState.Success(file.name, lastExportedMime)
                    }
                    ExportFormat.Text -> {
                        val content = TextExporter.export(col.name, records, options)
                        val file = com.tabbatch.app.platform.sharing.FileShareHelper
                            .writeExportFile(app, "tabbatch-export.txt", content)
                        lastExportedFile = file
                        lastExportedMime = "text/plain"
                        _exportState.value = ExportUiState.Success(file.name, lastExportedMime)
                    }
                    ExportFormat.Pdf -> {
                        val bytes = PdfExporter.export(col.name, records, options, timestamp)
                        val file = com.tabbatch.app.platform.sharing.FileShareHelper
                            .writeExportBytes(app, "tabbatch-export.pdf", bytes)
                        lastExportedFile = file
                        lastExportedMime = "application/pdf"
                        _exportState.value = ExportUiState.Success(file.name, lastExportedMime)
                    }
                }
            } catch (e: Exception) {
                _exportState.value = ExportUiState.Failed(e.message ?: "Unknown export failure")
            }
        }
    }

    fun resetExportState() {
        _exportState.value = ExportUiState.Idle
    }
}
