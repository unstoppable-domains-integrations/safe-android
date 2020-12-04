package io.gnosis.safe.ui.transactions

import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.gnosis.data.models.transaction.ConflictType
import io.gnosis.data.models.transaction.TransactionStatus
import pm.gnosis.model.Solidity

sealed class TransactionView(
    open val status: TransactionStatus?,
    open val id: String
) {

    data class Conflict(
        val innerView: TransactionView,
        val conflictType: ConflictType,
        override val id: String = innerView.id,
        override val status: TransactionStatus? = innerView.status
    ) : TransactionView(status, id)

    data class Transfer(
        override val id: String,
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val amountText: String,
        val dateTimeText: String,
        @DrawableRes val txTypeIcon: Int,
        val address: Solidity.Address,
        @ColorRes val amountColor: Int,
        val alpha: Float,
        val nonce: String
    ) : TransactionView(status, id)

    data class TransferQueued(
        override val id: String,
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val amountText: String,
        val dateTimeText: String,
        @DrawableRes val txTypeIcon: Int,
        val address: Solidity.Address,
        @ColorRes val amountColor: Int,
        val confirmations: Int,
        val threshold: Int,
        @ColorRes val confirmationsTextColor: Int,
        @DrawableRes val confirmationsIcon: Int,
        val nonce: String
    ) : TransactionView(status, id)

    data class SettingsChange(
        override val id: String,
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val method: String,
        val alpha: Float,
        val nonce: String
    ) : TransactionView(status, id)

    data class SettingsChangeQueued(
        override val id: String,
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val settingNameText: String,
        val confirmations: Int,
        val threshold: Int,
        @ColorRes val confirmationsTextColor: Int,
        @DrawableRes val confirmationsIcon: Int,
        val nonce: String
    ) : TransactionView(status, id)

    data class SettingsChangeVariant(
        override val id: String,
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val address: Solidity.Address?,
        @StringRes val addressLabel: Int,
        val alpha: Float,
        @StringRes val label: Int,
        val visibilityAddressLabel: Int = View.VISIBLE,
        val visibilityEllipsizedAddress: Int = View.VISIBLE,
        val visibilityModuleAddress: Int = View.GONE,
        val nonce: String

    ) : TransactionView(status, id)

    data class SettingsChangeVariantQueued(
        override val id: String,
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val confirmations: Int,
        val threshold: Int,
        @ColorRes val confirmationsTextColor: Int,
        @DrawableRes val confirmationsIcon: Int,
        val nonce: String,
        val address: Solidity.Address?,
        @StringRes val addressLabel: Int,
        @StringRes val label: Int,
        val visibilityAddressLabel: Int = View.VISIBLE,
        val visibilityEllipsizedAddress: Int = View.VISIBLE,
        val visibilityModuleAddress: Int = View.GONE
    ) : TransactionView(status, id)

    data class CustomTransaction(
        override val id: String,
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val address: Solidity.Address,
        val dataSizeText: String,
        val amountText: String,
        @ColorRes val amountColor: Int,
        val alpha: Float,
        val nonce: String
    ) : TransactionView(status, id)

    data class CustomTransactionQueued(
        override val id: String,
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val address: Solidity.Address,
        val confirmations: Int,
        val threshold: Int,
        @ColorRes val confirmationsTextColor: Int,
        @DrawableRes val confirmationsIcon: Int,
        val dataSizeText: String,
        val amountText: String,
        @ColorRes val amountColor: Int,
        val nonce: String
    ) : TransactionView(status, id)

    data class Creation(
        override val id: String,
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        @StringRes val label: Int,
        val creationDetails: CreationDetails? = null
    ) : TransactionView(status, id)

    data class CreationDetails(
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val creator: String,
        val implementation: String?,
        val factory: String?,
        val transactionHash: String
    )

    data class SectionHeader(val title: String, override val id: String = "<unused>") : TransactionView(TransactionStatus.PENDING, id)

    object Unknown : TransactionView(null, "<unused>")
}
