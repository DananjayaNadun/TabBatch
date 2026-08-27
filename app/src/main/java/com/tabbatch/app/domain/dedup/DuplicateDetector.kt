package com.tabbatch.app.domain.dedup

import com.tabbatch.app.domain.model.TabRecord

/**
 * Exact-duplicate detection.
 *
 * Definition of "exact duplicate" (documented per project brief section 9): two [TabRecord]s
 * are exact duplicates if their normalized [TabRecord.url] strings are byte-for-byte identical
 * after normalization. This intentionally does NOT treat differing query parameters, fragments,
 * trailing slashes, or http vs https as duplicates — those are different resources/requests and
 * silently merging them could hide real distinctions the user cares about.
 *
 * Uses a single-pass HashSet, O(n) in the number of records.
 */
object DuplicateDetector {

    /** Returns only the first occurrence of each unique URL, preserving original order. */
    fun deduplicate(records: List<TabRecord>): List<TabRecord> {
        val seen = HashSet<String>(records.size)
        return records.filter { seen.add(it.url) }
    }

    /** Returns the ids of records considered duplicates (i.e. every occurrence after the first). */
    fun duplicateIds(records: List<TabRecord>): Set<String> {
        val seen = HashSet<String>(records.size)
        val duplicates = LinkedHashSet<String>()
        for (record in records) {
            if (!seen.add(record.url)) duplicates.add(record.id)
        }
        return duplicates
    }
}
