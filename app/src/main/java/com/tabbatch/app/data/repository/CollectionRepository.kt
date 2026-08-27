package com.tabbatch.app.data.repository

import com.tabbatch.app.domain.model.TabCollection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the currently open [TabCollection] in memory for the lifetime of the process.
 *
 * Persistence decision (see docs/ARCHITECTURE_DECISIONS.md, ADR-0003): rather than adding Room
 * for what the MVP actually needs, "recent collections" persistence is implemented as
 * user-directed save/open of the app's own JSON export format through the Storage Access
 * Framework (i.e. a collection is just a TabBatch JSON file the user chooses where to keep).
 * This avoids a database migration surface for a single-entity, no-query use case, while still
 * satisfying "Open Collection" from the Home screen. A Room-backed "recent collections" list
 * remains a natural Phase 2 addition if usage shows it's wanted (see docs/ARCHITECTURE.md).
 */
class CollectionRepository {
    private val _current = MutableStateFlow<TabCollection?>(null)
    val current: StateFlow<TabCollection?> = _current.asStateFlow()

    fun set(collection: TabCollection) {
        _current.value = collection
    }

    fun clear() {
        _current.value = null
    }
}
