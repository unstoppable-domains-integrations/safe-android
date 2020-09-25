package io.gnosis.safe.ui.transactions.details.view

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.gnosis.safe.R

enum class TxType(@DrawableRes val iconRes: Int, @StringRes val titleRes: Int) {
    TRANSFER_INCOMING(R.drawable.ic_arrow_green_10dp, R.string.tx_status_type_transfer_incoming),
    TRANSFER_OUTGOING(R.drawable.ic_arrow_red_10dp, R.string.tx_status_type_transfer_outgoing),
    MODIFY_SETTINGS(R.drawable.ic_settings_change_14dp, R.string.tx_status_type_modify_settings),
    CUSTOM(R.drawable.ic_code_16dp, R.string.tx_status_type_custom),
    CREATION(R.drawable.ic_settings_change_14dp, R.string.tx_status_type_creation)
}
