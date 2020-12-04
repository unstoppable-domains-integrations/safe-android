package io.gnosis.data.models.transaction

import com.squareup.moshi.Json
import com.squareup.moshi.JsonQualifier
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
    @Json(name = "none") None,
    @Json(name = "hasNext") HasNext,
    @Json(name = "end") End
}

enum class LabelType {
    @Json(name = "NEXT") Next,
    @Json(name = "QUEUED") Queued
}

sealed class UnifiedEntry(@Json(name = "tx_item_type") val type: UnifiedEntryType) {

    data class Transaction(
        @Json(name = "transaction_summary") val transaction: TransactionSummary,
        @Json(name = "conflict_type") val conflictType: ConflictType
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