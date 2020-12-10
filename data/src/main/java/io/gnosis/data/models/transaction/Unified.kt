package io.gnosis.data.models.transaction

import io.gnosis.data.models.transaction.Transaction as TransactionSummary

import java.util.*

enum class UnifiedEntryType {
    TRANSACTION,
    DATE_LABEL,
    LABEL,
    CONFLICT_HEADER,
    UNKNOWN
}

enum class ConflictType {
    None,
    HasNext,
    End
}

enum class LabelType {
    Next,
    Queued
}

sealed class UnifiedEntry(val type: UnifiedEntryType) {

    data class Transaction(
        val transaction: TransactionSummary,
        val conflictType: ConflictType
    ) : UnifiedEntry(UnifiedEntryType.TRANSACTION)

    data class DateLabel(
        val timestamp: Date
    ) : UnifiedEntry(UnifiedEntryType.DATE_LABEL)

    data class Label(
        val label: LabelType
    ) : UnifiedEntry(UnifiedEntryType.LABEL)

    data class ConflictHeader(
        val nonce: Long
    ) : UnifiedEntry(UnifiedEntryType.CONFLICT_HEADER)

    object Unknown : UnifiedEntry(UnifiedEntryType.UNKNOWN)
}
