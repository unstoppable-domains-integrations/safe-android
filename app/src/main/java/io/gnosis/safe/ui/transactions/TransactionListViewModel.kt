package io.gnosis.safe.ui.transactions

import android.content.Context
import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import io.gnosis.data.models.Safe
import io.gnosis.data.models.transaction.*
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_CHANGE_MASTER_COPY
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_DISABLE_MODULE
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_ENABLE_MODULE
import io.gnosis.data.repositories.SafeRepository.Companion.METHOD_SET_FALLBACK_HANDLER
import io.gnosis.data.repositories.TokenRepository.Companion.NATIVE_CURRENCY_INFO
import io.gnosis.safe.R
import io.gnosis.safe.di.ApplicationContext
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.transactions.paging.TransactionPagingProvider
import io.gnosis.safe.utils.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger
import javax.inject.Inject

class TransactionListViewModel
@Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionsPager: TransactionPagingProvider,
    private val safeRepository: SafeRepository,
    private val ownerCredentialsRepository: OwnerCredentialsRepository,
    private val balanceFormatter: BalanceFormatter,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<TransactionsViewState>(appDispatchers) {

    private var historyList: Boolean = true
    fun init(history: Boolean) {
        historyList = history
        safeLaunch {
            safeRepository.activeSafeFlow().collect { load(true) }
        }
    }

    override fun initialState(): TransactionsViewState = TransactionsViewState(null, true)

    fun load(safeChange: Boolean = false) {
        safeLaunch {
            val safe = safeRepository.getActiveSafe()
            updateState { TransactionsViewState(isLoading = true, viewAction = if (safeChange) ActiveSafeChanged(safe) else ViewAction.None) }
            if (safe != null) {
                val owner = ownerCredentialsRepository.retrieveCredentials()?.address
                getTransactions(safe.address, owner).collectLatest {
                    updateState {
                        TransactionsViewState(
                            isLoading = false,
                            viewAction = LoadTransactions(it)
                        )
                    }
                }
            } else {
                updateState(forceViewAction = true) { TransactionsViewState(isLoading = false, viewAction = NoSafeSelected) }
            }
        }
    }

    private fun getTransactions(safe: Solidity.Address, owner: Solidity.Address?): Flow<PagingData<TransactionView>> =
        transactionsPager.getTransactionsStream(safe, historyList)
            .map { pagingData ->
                pagingData
                    .map { entries ->
                        getEntryView(entries, safe, owner)
                    }
                    .filter { it !is TransactionView.Unknown }
            }
            .cachedIn(viewModelScope)

    private fun getEntryView(
        entry: UnifiedEntry,
        activeSafe: Solidity.Address,
        owner: Solidity.Address?
    ): TransactionView {
            return when (entry) {
                is UnifiedEntry.Transaction -> {
                    val isConflict = entry.conflictType != ConflictType.None
                    val txView = getTransactionView(entry.transaction, activeSafe, isConflict, entry.transaction.canBeSignedByOwner(owner))
                    if (isConflict) {
                        TransactionView.Conflict(txView, entry.conflictType)
                    } else txView
                }
                is UnifiedEntry.DateLabel -> TransactionView.SectionHeader(title = entry.timestamp.formatBackendDate())
                is UnifiedEntry.Label -> when (entry.label) {
                    LabelType.Next -> TransactionView.SectionHeader(title = "NEXT")
                    LabelType.Queued -> TransactionView.SectionHeader(title = context.getString(R.string.tx_list_queue))
                }
                is UnifiedEntry.ConflictHeader -> TransactionView.SectionHeader(title = "${entry.nonce} These transactions conflict as they use the same nonce. Executing one will automatically replace the other(s).")
                UnifiedEntry.Unknown -> TransactionView.Unknown
            }
    }

    fun getTransactionView(
        transaction: Transaction,
        activeSafe: Solidity.Address,
        isConflict: Boolean,
        awaitingYourConfirmation: Boolean = false
    ): TransactionView {
        with(transaction) {
            return when (val txInfo = txInfo) {
                is TransactionInfo.Transfer -> toTransferView(txInfo, awaitingYourConfirmation, isConflict)
                is TransactionInfo.SettingsChange -> toSettingsChangeView(txInfo, awaitingYourConfirmation)
                is TransactionInfo.Custom -> toCustomTransactionView(txInfo, activeSafe, awaitingYourConfirmation, isConflict)
                is TransactionInfo.Creation -> toHistoryCreation(txInfo)
                TransactionInfo.Unknown -> TransactionView.Unknown
            }
        }
    }

    private fun Transaction.toTransferView(txInfo: TransactionInfo.Transfer, awaitingYourConfirmation: Boolean, isConflict: Boolean): TransactionView =
        if (isCompleted(txStatus)) historicTransfer(txInfo, isConflict)
        else queuedTransfer(txInfo, awaitingYourConfirmation, isConflict)

    private fun Transaction.historicTransfer(txInfo: TransactionInfo.Transfer, isConflict: Boolean): TransactionView.Transfer =
        TransactionView.Transfer(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus),
            statusColorRes = statusTextColor(txStatus),
            amountText = formatTransferAmount(txInfo.transferInfo, txInfo.incoming()),
            dateTimeText = timestamp.formatBackendDate(),
            txTypeIcon = if (txInfo.incoming()) R.drawable.ic_arrow_green_10dp else R.drawable.ic_arrow_red_10dp,
            address = if (txInfo.incoming()) txInfo.sender else txInfo.recipient,
            amountColor = if (txInfo.transferInfo.value() > BigInteger.ZERO && txInfo.incoming()) R.color.safe_green else R.color.gnosis_dark_blue,
            alpha = alpha(txStatus),
            nonce = if (isConflict) "" else executionInfo?.nonce?.toString() ?: ""
        )

    private fun Transaction.queuedTransfer(txInfo: TransactionInfo.Transfer, awaitingYourConfirmation: Boolean, isConflict: Boolean): TransactionView.TransferQueued {
        //FIXME this wouldn't make sense for incoming Ethereum TXs
        val threshold = executionInfo?.confirmationsRequired ?: -1
        val thresholdMet = checkThreshold(threshold, executionInfo?.confirmationsSubmitted)
        val incoming = txInfo.incoming()

        return TransactionView.TransferQueued(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus, awaitingYourConfirmation),
            statusColorRes = statusTextColor(txStatus),
            amountText = formatTransferAmount(txInfo.transferInfo, incoming),
            dateTimeText = timestamp.formatBackendDate(),
            txTypeIcon = if (incoming) R.drawable.ic_arrow_green_10dp else R.drawable.ic_arrow_red_10dp,
            address = if (incoming) txInfo.sender else txInfo.recipient,
            amountColor = if (txInfo.transferInfo.value() > BigInteger.ZERO && incoming) R.color.safe_green else R.color.gnosis_dark_blue,
            confirmations = executionInfo?.confirmationsSubmitted ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = if (isConflict) "" else executionInfo?.nonce?.toString().orEmpty()
        )
    }

    private fun Transaction.toSettingsChangeView(txInfo: TransactionInfo.SettingsChange, awaitingYourConfirmation: Boolean): TransactionView =
        when {
            txInfo.isQueuedImplementationChange(txStatus) -> queuedImplementationChange(txInfo, awaitingYourConfirmation)
            txInfo.isHistoricImplementationChange(txStatus) -> historicImplementationChange(txInfo)
            txInfo.isQueuedSetFallbackHandler(txStatus) -> queuedSetFallbackHandler(txInfo, awaitingYourConfirmation)
            txInfo.isHistoricSetFallbackHandler(txStatus) -> historicSetFallbackHandler(txInfo)
            txInfo.isQueuedModuleChange(txStatus) -> queuedModuleChange(txInfo, awaitingYourConfirmation)
            txInfo.isHistoricModuleChange(txStatus) -> historicModuleChange(txInfo)
            txInfo.isQueuedSettingsChange(txStatus) -> queuedSettingsChange(txInfo, awaitingYourConfirmation)
            txInfo.isHistoricSettingsChange(txStatus) -> historicSettingsChange(txInfo)
            else -> TransactionView.Unknown
        }

    private fun TransactionInfo.SettingsChange.isHistoricSetFallbackHandler(txStatus: TransactionStatus): Boolean =
        (settingsInfo is SettingsInfo.SetFallbackHandler || METHOD_SET_FALLBACK_HANDLER == dataDecoded.method) && isCompleted(txStatus)

    private fun TransactionInfo.SettingsChange.isQueuedSetFallbackHandler(txStatus: TransactionStatus): Boolean =
        (settingsInfo is SettingsInfo.SetFallbackHandler || METHOD_SET_FALLBACK_HANDLER == dataDecoded.method) && !isCompleted(txStatus)

    private fun TransactionInfo.SettingsChange.isHistoricModuleChange(txStatus: TransactionStatus): Boolean =
        ((settingsInfo is SettingsInfo.EnableModule || settingsInfo is SettingsInfo.DisableModule)
                || (METHOD_ENABLE_MODULE == dataDecoded.method || METHOD_DISABLE_MODULE == dataDecoded.method))
                && isCompleted(txStatus)

    private fun TransactionInfo.SettingsChange.isQueuedModuleChange(txStatus: TransactionStatus): Boolean =
        ((settingsInfo is SettingsInfo.EnableModule || settingsInfo is SettingsInfo.DisableModule)
                || (METHOD_ENABLE_MODULE == dataDecoded.method || METHOD_DISABLE_MODULE == dataDecoded.method))
                && !isCompleted(txStatus)

    private fun TransactionInfo.SettingsChange.isHistoricImplementationChange(txStatus: TransactionStatus): Boolean =
        (settingsInfo is SettingsInfo.ChangeImplementation || METHOD_CHANGE_MASTER_COPY == dataDecoded.method) && isCompleted(txStatus)

    private fun TransactionInfo.SettingsChange.isQueuedImplementationChange(txStatus: TransactionStatus): Boolean =
        (settingsInfo is SettingsInfo.ChangeImplementation || METHOD_CHANGE_MASTER_COPY == dataDecoded.method) && !isCompleted(txStatus)

    private fun TransactionInfo.SettingsChange.isHistoricSettingsChange(txStatus: TransactionStatus): Boolean =
        (settingsInfo !is SettingsInfo.ChangeImplementation || METHOD_CHANGE_MASTER_COPY != dataDecoded.method) && isCompleted(txStatus)

    private fun TransactionInfo.SettingsChange.isQueuedSettingsChange(txStatus: TransactionStatus): Boolean =
        (settingsInfo !is SettingsInfo.ChangeImplementation || METHOD_CHANGE_MASTER_COPY != dataDecoded.method) && !isCompleted(txStatus)

    private fun Transaction.historicSettingsChange(txInfo: TransactionInfo.SettingsChange): TransactionView.SettingsChange =
        TransactionView.SettingsChange(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            method = txInfo.dataDecoded.method,
            alpha = alpha(txStatus),
            nonce = executionInfo?.nonce?.toString().orEmpty()
        )

    private fun Transaction.queuedSettingsChange(
        txInfo: TransactionInfo.SettingsChange,
        awaitingYourConfirmation: Boolean
    ): TransactionView.SettingsChangeQueued {
        //FIXME this wouldn't make sense for incoming Ethereum TXs
        val threshold = executionInfo?.confirmationsRequired ?: -1
        val thresholdMet = checkThreshold(threshold, executionInfo?.confirmationsSubmitted)

        return TransactionView.SettingsChangeQueued(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus, awaitingYourConfirmation),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            settingNameText = txInfo.dataDecoded.method,
            confirmations = executionInfo?.confirmationsSubmitted ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = executionInfo?.nonce?.toString().orEmpty()
        )
    }

    private fun Transaction.historicImplementationChange(txInfo: TransactionInfo.SettingsChange): TransactionView.SettingsChangeVariant {
        val address = (txInfo.settingsInfo as SettingsInfo.ChangeImplementation).implementation
        val version = address.implementationVersion()

        return TransactionView.SettingsChangeVariant(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            alpha = alpha(txStatus),
            addressLabel = version,
            address = address,
            label = R.string.tx_list_change_mastercopy,
            nonce = executionInfo?.nonce.toString()
        )
    }

    private fun Transaction.queuedSetFallbackHandler(
        txInfo: TransactionInfo.SettingsChange,
        awaitingYourConfirmation: Boolean
    ): TransactionView.SettingsChangeVariantQueued {
        val threshold = executionInfo!!.confirmationsRequired
        val thresholdMet = checkThreshold(threshold, executionInfo?.confirmationsSubmitted)
        val address = (txInfo.settingsInfo as? SettingsInfo.SetFallbackHandler)?.handler
        val fallbackHandlerDisplayString = address.fallBackHandlerLabel()

        return TransactionView.SettingsChangeVariantQueued(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus, awaitingYourConfirmation),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            label = R.string.tx_list_set_fallback_handler,
            address = address,
            addressLabel = fallbackHandlerDisplayString,
            confirmations = executionInfo?.confirmationsSubmitted ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = executionInfo?.nonce?.toString().orEmpty()
        )
    }

    private fun Transaction.historicSetFallbackHandler(txInfo: TransactionInfo.SettingsChange): TransactionView.SettingsChangeVariant {
        val address = (txInfo.settingsInfo as SettingsInfo.SetFallbackHandler).handler
        val fallBackHandlerLabel = address.fallBackHandlerLabel()

        return TransactionView.SettingsChangeVariant(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            alpha = alpha(txStatus),
            label = R.string.tx_list_set_fallback_handler,
            address = address,
            addressLabel = fallBackHandlerLabel,
            nonce = executionInfo?.nonce.toString()
        )
    }

    private fun Transaction.queuedModuleChange(
        txInfo: TransactionInfo.SettingsChange,
        awaitingYourConfirmation: Boolean
    ): TransactionView.SettingsChangeVariantQueued {
        val threshold = executionInfo!!.confirmationsRequired
        val thresholdMet = checkThreshold(threshold, executionInfo?.confirmationsSubmitted)
        val settingsInfo = txInfo.settingsInfo
        val (address, label) =
            if (settingsInfo is SettingsInfo.EnableModule) settingsInfo.module to R.string.tx_list_enable_module
            else (settingsInfo as SettingsInfo.DisableModule).module to R.string.tx_list_disable_module

        return TransactionView.SettingsChangeVariantQueued(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus, awaitingYourConfirmation),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            label = label,
            address = address,
            addressLabel = R.string.empty_string,
            confirmations = executionInfo?.confirmationsSubmitted ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = executionInfo?.nonce?.toString().orEmpty(),
            visibilityAddressLabel = View.INVISIBLE,
            visibilityEllipsizedAddress = View.INVISIBLE,
            visibilityModuleAddress = View.VISIBLE
        )
    }

    private fun Transaction.historicModuleChange(txInfo: TransactionInfo.SettingsChange): TransactionView.SettingsChangeVariant {
        val settingsInfo = txInfo.settingsInfo
        val (address, label) =
            if (settingsInfo is SettingsInfo.EnableModule) settingsInfo.module to R.string.tx_list_enable_module
            else (settingsInfo as SettingsInfo.DisableModule).module to R.string.tx_list_disable_module

        return TransactionView.SettingsChangeVariant(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            alpha = alpha(txStatus),
            label = label,
            address = address,
            addressLabel = R.string.empty_string,
            visibilityAddressLabel = View.INVISIBLE,
            visibilityEllipsizedAddress = View.INVISIBLE,
            visibilityModuleAddress = View.VISIBLE,
            nonce = executionInfo?.nonce?.toString().orEmpty()
        )
    }

    private fun Transaction.queuedImplementationChange(
        txInfo: TransactionInfo.SettingsChange,
        awaitingYourConfirmation: Boolean
    ): TransactionView.SettingsChangeVariantQueued {
        val threshold = executionInfo!!.confirmationsRequired
        val thresholdMet = checkThreshold(threshold, executionInfo?.confirmationsSubmitted)
        val address = (txInfo.settingsInfo as SettingsInfo.ChangeImplementation).implementation
        val version = address.implementationVersion()

        return TransactionView.SettingsChangeVariantQueued(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus, awaitingYourConfirmation),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            confirmations = executionInfo?.confirmationsSubmitted ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = executionInfo?.nonce?.toString().orEmpty(),
            label = R.string.tx_list_change_mastercopy,
            address = address,
            addressLabel = version
        )
    }

    private fun Transaction.toCustomTransactionView(
        txInfo: TransactionInfo.Custom,
        safeAddress: Solidity.Address,
        awaitingYourConfirmation: Boolean,
        isConflict: Boolean
    ): TransactionView =
        if (!isCompleted(txStatus)) queuedCustomTransaction(txInfo, safeAddress, awaitingYourConfirmation, isConflict)
        else historicCustomTransaction(txInfo, safeAddress, isConflict)

    private fun Transaction.historicCustomTransaction(
        txInfo: TransactionInfo.Custom,
        safeAddress: Solidity.Address,
        isConflict: Boolean
    ): TransactionView.CustomTransaction {
        //FIXME https://github.com/gnosis/safe-client-gateway/issues/189
        val isIncoming: Boolean = txInfo.to == safeAddress
        return TransactionView.CustomTransaction(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            address = txInfo.to,
            dataSizeText = if (txInfo.dataSize >= 0) "${txInfo.dataSize} bytes" else "",
            amountText = balanceFormatter.formatAmount(txInfo.value, isIncoming, NATIVE_CURRENCY_INFO.decimals, NATIVE_CURRENCY_INFO.symbol),
            amountColor = if (txInfo.value > BigInteger.ZERO && isIncoming) R.color.safe_green else R.color.gnosis_dark_blue,
            alpha = alpha(txStatus),
            nonce = if (isConflict) "" else executionInfo?.nonce?.toString() ?: ""
        )
    }

    private fun Transaction.queuedCustomTransaction(
        txInfo: TransactionInfo.Custom,
        safeAddress: Solidity.Address,
        awaitingYourConfirmation: Boolean,
        isConflict: Boolean
    ): TransactionView.CustomTransactionQueued {
        //FIXME https://github.com/gnosis/safe-client-gateway/issues/189
        val isIncoming: Boolean = txInfo.to == safeAddress
        //FIXME this wouldn't make sense for incoming Ethereum TXs
        val threshold = executionInfo?.confirmationsRequired ?: -1
        val thresholdMet = checkThreshold(threshold, executionInfo?.confirmationsSubmitted)

        return TransactionView.CustomTransactionQueued(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus, awaitingYourConfirmation),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            address = txInfo.to,
            confirmations = executionInfo?.confirmationsSubmitted ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.safe_green else R.color.medium_grey,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = if (isConflict) "" else executionInfo?.nonce?.toString() ?: "",
            dataSizeText = if (txInfo.dataSize >= 0) "${txInfo.dataSize} bytes" else "",
            amountText = balanceFormatter.formatAmount(txInfo.value, isIncoming, NATIVE_CURRENCY_INFO.decimals, NATIVE_CURRENCY_INFO.symbol),
            amountColor = if (txInfo.value > BigInteger.ZERO && isIncoming) R.color.safe_green else R.color.gnosis_dark_blue
        )
    }

    private fun Transaction.toHistoryCreation(txInfo: TransactionInfo.Creation): TransactionView.Creation =
        TransactionView.Creation(
            id = id,
            status = txStatus,
            statusText = displayString(txStatus),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendDate(),
            label = R.string.tx_list_creation,
            creationDetails = TransactionView.CreationDetails(
                statusText = displayString(txStatus),
                statusColorRes = statusTextColor(txStatus),
                dateTimeText = timestamp.formatBackendDate(),
                creator = txInfo.creator.asEthereumAddressString(),
                factory = txInfo.factory?.asEthereumAddressString(),
                implementation = txInfo.implementation?.asEthereumAddressString(),
                transactionHash = txInfo.transactionHash
            )
        )

    private fun isCompleted(status: TransactionStatus): Boolean =
        when (status) {
            TransactionStatus.AWAITING_CONFIRMATIONS,
            TransactionStatus.AWAITING_EXECUTION,
            TransactionStatus.PENDING -> false
            TransactionStatus.SUCCESS,
            TransactionStatus.FAILED,
            TransactionStatus.CANCELLED -> true
        }

    @StringRes
    private fun displayString(status: TransactionStatus, awaitingYourConfirmation: Boolean = false): Int =
        when (status) {
            TransactionStatus.AWAITING_CONFIRMATIONS -> if (awaitingYourConfirmation) R.string.tx_status_awaiting_your_confirmation else R.string.tx_status_awaiting_confirmations
            TransactionStatus.AWAITING_EXECUTION -> R.string.tx_status_awaiting_execution
            TransactionStatus.CANCELLED -> R.string.tx_status_cancelled
            TransactionStatus.FAILED -> R.string.tx_status_failed
            TransactionStatus.SUCCESS -> R.string.tx_status_success
            TransactionStatus.PENDING -> R.string.tx_status_pending
        }

    private fun formatTransferAmount(transferInfo: TransferInfo, incoming: Boolean): String {
        val symbol = transferInfo.symbol().let { symbol ->
            if (symbol.isNullOrEmpty()) {
                getDefaultSymbol(transferInfo.type)
            } else {
                symbol
            }
        }
        return balanceFormatter.formatAmount(transferInfo.value(), incoming, transferInfo.decimals() ?: 0, symbol)
    }

    private fun getDefaultSymbol(type: TransferType?): String = when (type) {
        TransferType.ERC721 -> DEFAULT_ERC721_SYMBOL
        TransferType.ERC20 -> DEFAULT_ERC20_SYMBOL
        else -> ""
    }

    private fun statusTextColor(status: TransactionStatus): Int {
        return when (status) {
            TransactionStatus.SUCCESS -> R.color.safe_green
            TransactionStatus.CANCELLED -> R.color.dark_grey
            TransactionStatus.AWAITING_EXECUTION,
            TransactionStatus.AWAITING_CONFIRMATIONS,
            TransactionStatus.PENDING -> R.color.safe_pending_orange
            else -> R.color.safe_failed_red
        }
    }

    private fun alpha(txStatus: TransactionStatus): Float =
        when (txStatus) {
            TransactionStatus.FAILED, TransactionStatus.CANCELLED -> OPACITY_HALF
            else -> OPACITY_FULL
        }

    private fun checkThreshold(safeThreshold: Int, txThreshold: Int?): Boolean {
        return txThreshold?.let {
            it >= safeThreshold
        } ?: false
    }

    companion object {
        const val OPACITY_FULL = 1.0F
        const val OPACITY_HALF = 0.5F
    }
}

data class TransactionsViewState(
    override var viewAction: BaseStateViewModel.ViewAction?,
    val isLoading: Boolean
) : BaseStateViewModel.State

data class LoadTransactions(
    val newTransactions: PagingData<TransactionView>
) : BaseStateViewModel.ViewAction

data class ActiveSafeChanged(
    val activeSafe: Safe?
) : BaseStateViewModel.ViewAction

object NoSafeSelected : BaseStateViewModel.ViewAction

fun TransactionView.isQueued() = when (status) {
    TransactionStatus.PENDING,
    TransactionStatus.AWAITING_CONFIRMATIONS,
    TransactionStatus.AWAITING_EXECUTION -> true
    else -> false
}

fun TransactionView.isHistory() = when (status) {
    TransactionStatus.CANCELLED,
    TransactionStatus.FAILED,
    TransactionStatus.SUCCESS -> true
    else -> false
}

fun Transaction.canBeSignedByOwner(ownerAddress: Solidity.Address?): Boolean {
    return executionInfo?.missingSigners?.contains(ownerAddress) == true
}

fun TransactionInfo.Transfer.incoming(): Boolean = direction != TransactionDirection.OUTGOING
