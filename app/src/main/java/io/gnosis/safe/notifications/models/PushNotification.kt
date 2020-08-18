package io.gnosis.safe.notifications.models

import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

sealed class PushNotification(
    val type: String
) {

    abstract val safe: Solidity.Address

    data class NewConfirmation(
        override val safe: Solidity.Address,
        val safeTxHash: String,
        val owner: Solidity.Address
    ) : PushNotification(TYPE) {
        companion object {
            const val TYPE = "NEW_CONFIRMATION"
            fun fromMap(params: Map<String, String>) =
                NewConfirmation(
                    params.getOrThrow("address").asEthereumAddress()!!,
                    params.getOrThrow("safeTxHash"),
                    params.getOrThrow("owner").asEthereumAddress()!!
                )
        }
    }

    data class ExecutedTransaction(
        override val safe: Solidity.Address,
        val safeTxHash: String,
        val failed: Boolean
    ) : PushNotification(TYPE) {
        companion object {
            const val TYPE = "EXECUTED_MULTISIG_TRANSACTION"
            fun fromMap(params: Map<String, String>) =
                ExecutedTransaction(
                    params.getOrThrow("address").asEthereumAddress()!!,
                    params.getOrThrow("safeTxHash"),
                    params.getOrThrow("failed").toBoolean()
                )
        }
    }

    data class IncomingToken(
        override val safe: Solidity.Address,
        val txHash: String,
        val value: BigInteger,
        val tokenAddress: Solidity.Address,
        val tokenId: String? = null // for ERC721 tokens
    ) : PushNotification(TYPE) {
        companion object {
            const val TYPE = "INCOMING_TOKEN"
            fun fromMap(params: Map<String, String>) =
                IncomingToken(
                    params.getOrThrow("address").asEthereumAddress()!!,
                    params.getOrThrow("txHash"),
                    params.getOrThrow("value").toBigInteger(),
                    params.getOrThrow("tokenAddress").asEthereumAddress()!!,
                    params["tokenId"]
                )
        }
    }

    data class IncomingEther(
        override val safe: Solidity.Address,
        val txHash: String,
        val value: BigInteger
    ) : PushNotification(TYPE) {
        companion object {
            const val TYPE = "INCOMING_ETHER"
            fun fromMap(params: Map<String, String>) =
                IncomingEther(
                    params.getOrThrow("address").asEthereumAddress()!!,
                    params.getOrThrow("txHash"),
                    params.getOrThrow("value").toBigInteger()
                )
        }
    }

    companion object {
        fun fromMap(params: Map<String, String>) =
            when (params["type"]) {
                "NEW_CONFIRMATION" -> NewConfirmation.fromMap(params)
                "EXECUTED_MULTISIG_TRANSACTION" -> ExecutedTransaction.fromMap(params)
                "INCOMING_TOKEN" -> IncomingToken.fromMap(params)
                "INCOMING_ETHER" -> IncomingEther.fromMap(params)
                else -> throw IllegalArgumentException("Unknown push type")
            }
    }
}

private fun Map<String, String>.getOrThrow(key: String) =
    get(key) ?: throw IllegalArgumentException("Missing param $key")
