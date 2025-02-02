package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.models.Page
import io.gnosis.data.models.transaction.Param
import io.gnosis.data.models.transaction.TransactionConfirmationRequest
import io.gnosis.data.models.transaction.TransactionDetails
import io.gnosis.data.models.transaction.TxListEntry
import io.gnosis.data.utils.toSignatureString
import pm.gnosis.crypto.KeyPair
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.removeHexPrefix
import java.math.BigInteger

class TransactionRepository(
    private val gatewayApi: GatewayApi
) {

    suspend fun getQueuedTransactions(safeAddress: Solidity.Address): Page<TxListEntry> =
        gatewayApi.loadTransactionsQueue(safeAddress.asEthereumAddressChecksumString())

    suspend fun getHistoryTransactions(safeAddress: Solidity.Address): Page<TxListEntry> =
        gatewayApi.loadTransactionsHistory(safeAddress.asEthereumAddressChecksumString())

    suspend fun loadTransactionsPage(pageLink: String): Page<TxListEntry> =
        gatewayApi.loadTransactionsPage(pageLink)

    suspend fun getTransactionDetails(txId: String): TransactionDetails =
        gatewayApi.loadTransactionDetails(txId)

    suspend fun submitConfirmation(safeTxHash: String, signedSafeTxHash: String): TransactionDetails =
        gatewayApi.submitConfirmation(safeTxHash, TransactionConfirmationRequest(signedSafeTxHash))

    fun sign(ownerKey: BigInteger, safeTxHash: String): String =
        KeyPair.fromPrivate(ownerKey.toByteArray())
            .sign(safeTxHash.hexToByteArray())
            .toSignatureString()
}

fun List<Param>?.getAddressValueByName(name: String): Solidity.Address? {
    return this?.find {
        it is Param.Address && it.name == name
    }?.value as Solidity.Address?
}

fun List<Param>?.getIntValueByName(name: String): String? {
    return this?.find {
        it is Param.Value && it.name == name
    }?.value as String?
}

fun String.dataSizeBytes(): Long = removeHexPrefix().hexToByteArray().size.toLong()
fun String?.hexStringNullOrEmpty(): Boolean = this?.dataSizeBytes() ?: 0L == 0L
