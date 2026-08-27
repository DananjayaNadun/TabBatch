package com.tabbatch.app.domain.model

/** A named set of imported [TabRecord]s, plus the pre-computed summary statistics the UI shows. */
data class TabCollection(
    val name: String,
    val createdAt: Long,
    val records: List<TabRecord>,
) {
    /** Records grouped by [DomainGroup], sorted by descending record count then name. */
    val groups: List<DomainGroup> by lazy { DomainGroup.groupRecords(records) }

    val duplicateInfo: DuplicateSummary by lazy { DuplicateSummary.of(records) }

    val totalCount: Int get() = records.size
}

/** One group of [TabRecord]s that share a display domain (see [DomainGrouper]). */
data class DomainGroup(
    val displayName: String,
    val records: List<TabRecord>,
) {
    val count: Int get() = records.size

    companion object {
        fun groupRecords(records: List<TabRecord>): List<DomainGroup> {
            val byDomain = LinkedHashMap<String, MutableList<TabRecord>>()
            for (record in records) {
                val key = record.registrableDomain ?: record.host.ifBlank { "Other" }
                byDomain.getOrPut(key) { mutableListOf() }.add(record)
            }
            return byDomain.map { (name, recs) -> DomainGroup(name, recs) }
                .sortedWith(compareByDescending<DomainGroup> { it.count }.thenBy { it.displayName })
        }
    }
}

/** Exact-duplicate statistics for a set of records. See docs/EXPORT_FORMATS.md for the
 * precise definition of "exact duplicate" used by TabBatch. */
data class DuplicateSummary(
    val totalCount: Int,
    val uniqueCount: Int,
    val duplicateCount: Int,
    /** ids of records considered duplicates of an earlier record (i.e. not the first occurrence). */
    val duplicateRecordIds: Set<String>,
) {
    companion object {
        fun of(records: List<TabRecord>): DuplicateSummary {
            val seen = HashSet<String>(records.size)
            val duplicateIds = LinkedHashSet<String>()
            for (record in records) {
                if (!seen.add(record.url)) {
                    duplicateIds.add(record.id)
                }
            }
            return DuplicateSummary(
                totalCount = records.size,
                uniqueCount = records.size - duplicateIds.size,
                duplicateCount = duplicateIds.size,
                duplicateRecordIds = duplicateIds,
            )
        }
    }
}
