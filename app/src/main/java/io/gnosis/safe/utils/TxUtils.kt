package io.gnosis.safe.utils

import io.gnosis.data.BuildConfig
import io.gnosis.data.models.transaction.TransactionDirection
import io.gnosis.data.models.transaction.TransactionInfo
import io.gnosis.data.models.transaction.TransferInfo
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.getAddressValueByName
import io.gnosis.data.repositories.getIntValueByName
import io.gnosis.safe.R
import io.gnosis.safe.ui.transactions.details.view.ActionInfoItem
import java.math.BigInteger

const val DEFAULT_ERC20_SYMBOL = "ERC20"
const val DEFAULT_ERC721_SYMBOL = "NFT"

fun TransactionInfo.formattedAmount(balanceFormatter: BalanceFormatter): String =
    when (val txInfo = this) {
        is TransactionInfo.Custom -> {
            balanceFormatter.formatAmount(txInfo.value, false, 18, BuildConfig.NATIVE_CURRENCY_SYMBOL)
        }
        is TransactionInfo.Transfer -> {
            val incoming = txInfo.direction == TransactionDirection.INCOMING
            val decimals: Int = when (val transferInfo = txInfo.transferInfo) {
                is TransferInfo.Erc20Transfer -> {
                    transferInfo.decimals ?: 0
                }
                is TransferInfo.EtherTransfer -> 18
                else -> 0
            }
            val symbol: String = when (val transferInfo = txInfo.transferInfo) {
                is TransferInfo.Erc20Transfer -> {
                    transferInfo.tokenSymbol ?: DEFAULT_ERC20_SYMBOL
                }
                is TransferInfo.Erc721Transfer -> {
                    transferInfo.tokenSymbol ?: DEFAULT_ERC721_SYMBOL
                }
                else -> {
                    BuildConfig.NATIVE_CURRENCY_SYMBOL
                }
            }
            val value = when (val transferInfo = txInfo.transferInfo) {
                is TransferInfo.Erc20Transfer -> {
                    transferInfo.value
                }
                is TransferInfo.Erc721Transfer -> {
                    BigInteger.ONE
                }

                is TransferInfo.EtherTransfer -> {
                    transferInfo.value
                }

            }
            balanceFormatter.formatAmount(value, incoming, decimals, symbol)
        }
        is TransactionInfo.SettingsChange -> "0 ${BuildConfig.NATIVE_CURRENCY_SYMBOL}"
        is TransactionInfo.Creation -> "0 ${BuildConfig.NATIVE_CURRENCY_SYMBOL}"
        TransactionInfo.Unknown -> "0 ${BuildConfig.NATIVE_CURRENCY_SYMBOL}"
    }

fun TransactionInfo.logoUri(): String? =
    when (val transactionInfo = this) {
        is TransactionInfo.Transfer -> when (val transferInfo = transactionInfo.transferInfo) {
            is TransferInfo.Erc20Transfer -> {
                transferInfo.logoUri
            }
            is TransferInfo.Erc721Transfer -> {
                transferInfo.logoUri
            }
            else -> {
                "local::native_currency"
            }
        }
        is TransactionInfo.Custom, is TransactionInfo.SettingsChange, is TransactionInfo.Creation, TransactionInfo.Unknown -> "local::native_currency"
    }

fun TransactionInfo.SettingsChange.txActionInfoItems(): List<ActionInfoItem> {
    val settingsMethodTitle = mapOf(
        SafeRepository.METHOD_ADD_OWNER_WITH_THRESHOLD to R.string.tx_details_add_owner,
        SafeRepository.METHOD_CHANGE_MASTER_COPY to R.string.tx_details_new_mastercopy,
        SafeRepository.METHOD_CHANGE_THRESHOLD to R.string.tx_details_change_required_confirmations,
        SafeRepository.METHOD_DISABLE_MODULE to R.string.tx_details_disable_module,
        SafeRepository.METHOD_ENABLE_MODULE to R.string.tx_details_enable_module,
        SafeRepository.METHOD_REMOVE_OWNER to R.string.tx_details_remove_owner,
        SafeRepository.METHOD_SET_FALLBACK_HANDLER to R.string.tx_details_set_fallback_handler,
        SafeRepository.METHOD_SWAP_OWNER to R.string.tx_details_remove_owner
    )
    val result = mutableListOf<ActionInfoItem>()
    val settingsChange = this

    val params = settingsChange.dataDecoded.parameters
    when (settingsChange.dataDecoded.method) {
        SafeRepository.METHOD_CHANGE_MASTER_COPY -> {
            val mainCopy = params.getAddressValueByName("_masterCopy")
            val label = mainCopy?.implementationVersion()

            result.add(
                ActionInfoItem.AddressWithLabel(
                    itemLabel = settingsMethodTitle[settingsChange.dataDecoded.method],
                    address = mainCopy,
                    addressLabel = label
                )
            )
        }
        SafeRepository.METHOD_CHANGE_THRESHOLD -> {
            val value = params.getIntValueByName("_threshold") ?: ""
            result.add(ActionInfoItem.Value(itemLabel = settingsMethodTitle[settingsChange.dataDecoded.method], value = value))
        }
        SafeRepository.METHOD_ADD_OWNER_WITH_THRESHOLD -> {
            result.add(
                ActionInfoItem.Address(
                    settingsMethodTitle[SafeRepository.METHOD_ADD_OWNER_WITH_THRESHOLD],
                    params.getAddressValueByName("owner")
                )
            )
            result.add(ActionInfoItem.Value(settingsMethodTitle[SafeRepository.METHOD_CHANGE_THRESHOLD], params.getIntValueByName("_threshold")!!))
        }
        SafeRepository.METHOD_REMOVE_OWNER -> {
            result.add(
                ActionInfoItem.Address(
                    settingsMethodTitle[SafeRepository.METHOD_REMOVE_OWNER],
                    params.getAddressValueByName("owner")
                )
            )
            result.add(ActionInfoItem.Value(settingsMethodTitle[SafeRepository.METHOD_CHANGE_THRESHOLD], params.getIntValueByName("_threshold")!!))
        }
        SafeRepository.METHOD_SET_FALLBACK_HANDLER -> {
            val fallbackHandler = params.getAddressValueByName("handler")
            val label =
                if (SafeRepository.DEFAULT_FALLBACK_HANDLER == fallbackHandler) {
                    R.string.default_fallback_handler
                } else {
                    R.string.unknown_fallback_handler
                }
            result.add(
                ActionInfoItem.AddressWithLabel(
                    itemLabel = settingsMethodTitle[SafeRepository.METHOD_SET_FALLBACK_HANDLER],
                    address = fallbackHandler,
                    addressLabel = label
                )
            )
        }
        SafeRepository.METHOD_SWAP_OWNER -> {
            result.add(
                ActionInfoItem.Address(
                    settingsMethodTitle[SafeRepository.METHOD_REMOVE_OWNER],
                    params.getAddressValueByName("oldOwner")
                )
            )
            result.add(
                ActionInfoItem.Address(
                    settingsMethodTitle[SafeRepository.METHOD_ADD_OWNER_WITH_THRESHOLD],
                    params.getAddressValueByName("newOwner")
                )
            )
        }
        SafeRepository.METHOD_ENABLE_MODULE -> {
            result.add(
                ActionInfoItem.Address(
                    settingsMethodTitle[SafeRepository.METHOD_ENABLE_MODULE],
                    params.getAddressValueByName("module")
                )
            )
        }
        SafeRepository.METHOD_DISABLE_MODULE -> {
            result.add(
                ActionInfoItem.Address(
                    settingsMethodTitle[SafeRepository.METHOD_DISABLE_MODULE],
                    params.getAddressValueByName("module")
                )
            )
        }
    }

    return result
}
