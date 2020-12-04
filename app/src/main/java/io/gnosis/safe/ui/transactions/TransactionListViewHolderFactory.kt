package io.gnosis.safe.ui.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.navigation.Navigation
import androidx.viewbinding.ViewBinding
import io.gnosis.data.models.transaction.ConflictType
import io.gnosis.safe.R
import io.gnosis.safe.databinding.*
import io.gnosis.safe.ui.base.adapter.Adapter
import io.gnosis.safe.ui.base.adapter.BaseFactory
import io.gnosis.safe.ui.base.adapter.UnsupportedViewType
import io.gnosis.safe.ui.transactions.TransactionListViewModel.Companion.OPACITY_FULL
import io.gnosis.safe.utils.formatForTxList

enum class TransactionViewType {
    TRANSFER,
    TRANSFER_QUEUED,
    CHANGE_IMPLEMENTATION,
    CHANGE_IMPLEMENTATION_QUEUED,
    SETTINGS_CHANGE,
    SETTINGS_CHANGE_QUEUED,
    CUSTOM_TRANSACTION,
    CUSTOM_TRANSACTION_QUEUED,
    SECTION_HEADER,
    CREATION,
    CONFLICT
}

class TransactionViewHolderFactory : BaseFactory<BaseTransactionViewHolder<TransactionView>, TransactionView>() {

    @Suppress("UNCHECKED_CAST")
    override fun newViewHolder(viewBinding: ViewBinding, viewType: Int): BaseTransactionViewHolder<TransactionView> =
        when (viewType) {
            TransactionViewType.CHANGE_IMPLEMENTATION.ordinal -> ChangeImplementationViewHolder(viewBinding as ItemTxChangeImplementationBinding)
            TransactionViewType.CHANGE_IMPLEMENTATION_QUEUED.ordinal -> ChangeImplementationQueuedViewHolder(viewBinding as ItemTxQueuedChangeImplementationBinding)
            TransactionViewType.SETTINGS_CHANGE.ordinal -> SettingsChangeViewHolder(viewBinding as ItemTxSettingsChangeBinding)
            TransactionViewType.SETTINGS_CHANGE_QUEUED.ordinal -> SettingsChangeQueuedViewHolder(viewBinding as ItemTxQueuedSettingsChangeBinding)
            TransactionViewType.TRANSFER.ordinal -> TransferViewHolder(viewBinding as ItemTxTransferBinding)
            TransactionViewType.TRANSFER_QUEUED.ordinal -> TransferQueuedViewHolder(viewBinding as ItemTxQueuedTransferBinding)
            TransactionViewType.CUSTOM_TRANSACTION.ordinal -> CustomTransactionViewHolder(viewBinding as ItemTxTransferBinding)
            TransactionViewType.CUSTOM_TRANSACTION_QUEUED.ordinal -> CustomTransactionQueuedViewHolder(viewBinding as ItemTxQueuedTransferBinding)
            TransactionViewType.SECTION_HEADER.ordinal -> SectionHeaderViewHolder(viewBinding as ItemTxSectionHeaderBinding)
            TransactionViewType.CREATION.ordinal -> CreationTransactionViewHolder(viewBinding as ItemTxSettingsChangeBinding)
            TransactionViewType.CONFLICT.ordinal -> ConflictViewHolder(viewBinding as ItemTxConflictTxBinding, this)
            else -> throw UnsupportedViewType(javaClass.name)
        } as BaseTransactionViewHolder<TransactionView>

    override fun layout(layoutInflater: LayoutInflater, parent: ViewGroup, viewType: Int): ViewBinding =
        when (viewType) {
            TransactionViewType.CHANGE_IMPLEMENTATION.ordinal -> ItemTxChangeImplementationBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.CHANGE_IMPLEMENTATION_QUEUED.ordinal -> ItemTxQueuedChangeImplementationBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SETTINGS_CHANGE.ordinal -> ItemTxSettingsChangeBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SETTINGS_CHANGE_QUEUED.ordinal -> ItemTxQueuedSettingsChangeBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.TRANSFER.ordinal,
            TransactionViewType.CUSTOM_TRANSACTION.ordinal -> ItemTxTransferBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.TRANSFER_QUEUED.ordinal,
            TransactionViewType.CUSTOM_TRANSACTION_QUEUED.ordinal -> ItemTxQueuedTransferBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SECTION_HEADER.ordinal -> ItemTxSectionHeaderBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.CREATION.ordinal -> ItemTxSettingsChangeBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.CONFLICT.ordinal -> ItemTxConflictTxBinding.inflate(layoutInflater, parent, false)
            else -> throw UnsupportedViewType(javaClass.name)
        }

    override fun viewTypeFor(item: TransactionView): Int =
        when (item) {
            is TransactionView.Transfer -> TransactionViewType.TRANSFER
            is TransactionView.TransferQueued -> TransactionViewType.TRANSFER_QUEUED
            is TransactionView.SettingsChange -> TransactionViewType.SETTINGS_CHANGE
            is TransactionView.SettingsChangeQueued -> TransactionViewType.SETTINGS_CHANGE_QUEUED
            is TransactionView.SettingsChangeVariant -> TransactionViewType.CHANGE_IMPLEMENTATION
            is TransactionView.SettingsChangeVariantQueued -> TransactionViewType.CHANGE_IMPLEMENTATION_QUEUED
            is TransactionView.SectionHeader -> TransactionViewType.SECTION_HEADER
            is TransactionView.CustomTransaction -> TransactionViewType.CUSTOM_TRANSACTION
            is TransactionView.CustomTransactionQueued -> TransactionViewType.CUSTOM_TRANSACTION_QUEUED
            is TransactionView.Creation -> TransactionViewType.CREATION
            is TransactionView.Conflict -> TransactionViewType.CONFLICT
            is TransactionView.Unknown -> throw UnsupportedViewType(javaClass.name)
        }.ordinal
}

abstract class BaseTransactionViewHolder<T : TransactionView>(viewBinding: ViewBinding) : Adapter.ViewHolder<T>(viewBinding.root)

class ConflictViewHolder(private val viewBinding: ItemTxConflictTxBinding, private val factory: TransactionViewHolderFactory) :
    BaseTransactionViewHolder<TransactionView.Conflict>(viewBinding) {
    override fun bind(data: TransactionView.Conflict, payloads: List<Any>) {
        viewBinding.txContainer.removeAllViews()
        val viewType = factory.viewTypeFor(data.innerView)
        val innerBinding = factory.layout(LayoutInflater.from(viewBinding.txContainer.context), viewBinding.txContainer, viewType)
        val innerViewHolder = factory.newViewHolder(innerBinding, viewType)
        innerViewHolder.bind(data.innerView, payloads)

        viewBinding.txContainer.addView(innerBinding.root)

        viewBinding.lineBottom.isVisible = data.conflictType != ConflictType.End
    }
}

class TransferViewHolder(private val viewBinding: ItemTxTransferBinding) :
    BaseTransactionViewHolder<TransactionView.Transfer>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.Transfer, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme
        with(viewBinding) {
            txTypeIcon.setImageResource(viewTransfer.txTypeIcon)
            finalStatus.setText(viewTransfer.statusText)
            finalStatus.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))
            dateTime.text = viewTransfer.dateTimeText
            blockies.setAddress(viewTransfer.address)
            ellipsizedAddress.text = viewTransfer.address.formatForTxList()
            amount.text = viewTransfer.amountText
            amount.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.amountColor, theme))
            nonce.text = viewTransfer.nonce

            finalStatus.alpha = OPACITY_FULL
            txTypeIcon.alpha = viewTransfer.alpha
            dateTime.alpha = viewTransfer.alpha
            blockies.alpha = viewTransfer.alpha
            ellipsizedAddress.alpha = viewTransfer.alpha
            amount.alpha = viewTransfer.alpha
            nonce.alpha = viewTransfer.alpha

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.id)
            }
        }
    }
}

class TransferQueuedViewHolder(private val viewBinding: ItemTxQueuedTransferBinding) :
    BaseTransactionViewHolder<TransactionView.TransferQueued>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.TransferQueued, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme
        with(viewBinding) {
            status.setText(viewTransfer.statusText)
            status.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))
            amount.text = viewTransfer.amountText
            dateTime.text = viewTransfer.dateTimeText
            txTypeIcon.setImageResource(viewTransfer.txTypeIcon)
            blockies.setAddress(viewTransfer.address)
            ellipsizedAddress.text = viewTransfer.address.formatForTxList()
            amount.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.amountColor, theme))
            confirmationsIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, viewTransfer.confirmationsIcon, theme))
            confirmations.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.confirmationsTextColor, theme))
            confirmations.text = resources.getString(R.string.tx_list_confirmations, viewTransfer.confirmations, viewTransfer.threshold)
            nonce.text = viewTransfer.nonce

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.id)
            }
        }
    }
}

class SettingsChangeViewHolder(private val viewBinding: ItemTxSettingsChangeBinding) :
    BaseTransactionViewHolder<TransactionView.SettingsChange>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.SettingsChange, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme

        with(viewBinding) {
            finalStatus.setText(viewTransfer.statusText)
            finalStatus.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))

            dateTime.text = viewTransfer.dateTimeText
            settingName.text = viewTransfer.method
            nonce.text = viewTransfer.nonce

            finalStatus.alpha = OPACITY_FULL
            txTypeIcon.alpha = viewTransfer.alpha
            dateTime.alpha = viewTransfer.alpha
            settingName.alpha = viewTransfer.alpha
            nonce.alpha = viewTransfer.alpha

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.id)
            }
        }
    }
}

class SettingsChangeQueuedViewHolder(private val viewBinding: ItemTxQueuedSettingsChangeBinding) :
    BaseTransactionViewHolder<TransactionView.SettingsChangeQueued>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.SettingsChangeQueued, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme
        with(viewBinding) {
            status.setText(viewTransfer.statusText)
            status.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))

            dateTime.text = viewTransfer.dateTimeText
            settingName.text = viewTransfer.settingNameText

            confirmations.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.confirmationsTextColor, theme))
            confirmationsIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, viewTransfer.confirmationsIcon, theme))
            confirmations.text = resources.getString(R.string.tx_list_confirmations, viewTransfer.confirmations, viewTransfer.threshold)
            nonce.text = viewTransfer.nonce

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.id)
            }
        }
    }
}

class ChangeImplementationViewHolder(private val viewBinding: ItemTxChangeImplementationBinding) :
    BaseTransactionViewHolder<TransactionView.SettingsChangeVariant>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.SettingsChangeVariant, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme
        with(viewBinding) {

            finalStatus.setText(viewTransfer.statusText)
            finalStatus.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))
            dateTime.text = viewTransfer.dateTimeText
            addressLabel.setText(viewTransfer.addressLabel)
            blockies.setAddress(viewTransfer.address)
            ellipsizedAddress.text = viewTransfer.address?.formatForTxList() ?: ""
            label.setText(viewTransfer.label)
            nonce.text = viewTransfer.nonce
            moduleAddress.text = viewTransfer.address?.formatForTxList() ?: ""

            finalStatus.alpha = OPACITY_FULL
            txTypeIcon.alpha = viewTransfer.alpha
            dateTime.alpha = viewTransfer.alpha
            addressLabel.alpha = viewTransfer.alpha
            blockies.alpha = viewTransfer.alpha
            ellipsizedAddress.alpha = viewTransfer.alpha
            label.alpha = viewTransfer.alpha
            nonce.alpha = viewTransfer.alpha
            moduleAddress.alpha = viewTransfer.alpha

            addressLabel.visibility = viewTransfer.visibilityAddressLabel
            ellipsizedAddress.visibility = viewTransfer.visibilityEllipsizedAddress
            moduleAddress.visibility = viewTransfer.visibilityModuleAddress

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.id)
            }
        }
    }
}

class ChangeImplementationQueuedViewHolder(private val viewBinding: ItemTxQueuedChangeImplementationBinding) :
    BaseTransactionViewHolder<TransactionView.SettingsChangeVariantQueued>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.SettingsChangeVariantQueued, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme
        with(viewBinding) {
            status.setText(viewTransfer.statusText)
            status.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))

            dateTime.text = viewTransfer.dateTimeText

            addressLabel.setText(viewTransfer.addressLabel)
            blockies.setAddress(viewTransfer.address)
            ellipsizedAddress.text = viewTransfer.address?.formatForTxList() ?: ""
            label.setText(viewTransfer.label)
            nonce.text = viewTransfer.nonce

            confirmationsIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, viewTransfer.confirmationsIcon, theme))
            confirmations.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.confirmationsTextColor, theme))
            confirmations.text = resources.getString(R.string.tx_list_confirmations, viewTransfer.confirmations, viewTransfer.threshold)

            moduleAddress.text = viewTransfer.address?.formatForTxList() ?: ""
            addressLabel.visibility = viewTransfer.visibilityAddressLabel
            ellipsizedAddress.visibility = viewTransfer.visibilityEllipsizedAddress
            moduleAddress.visibility = viewTransfer.visibilityModuleAddress

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.id)
            }
        }
    }
}

class CustomTransactionQueuedViewHolder(private val viewBinding: ItemTxQueuedTransferBinding) :
    BaseTransactionViewHolder<TransactionView.CustomTransactionQueued>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.CustomTransactionQueued, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme
        with(viewBinding) {
            txTypeIcon.setImageResource(R.drawable.ic_code_16dp)

            status.setText(viewTransfer.statusText)
            status.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))

            dateTime.text = viewTransfer.dateTimeText

            confirmationsIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, viewTransfer.confirmationsIcon, theme))
            confirmations.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.confirmationsTextColor, theme))
            confirmations.text = resources.getString(R.string.tx_list_confirmations, viewTransfer.confirmations, viewTransfer.threshold)

            dataSize.text = viewTransfer.dataSizeText
            amount.text = viewTransfer.amountText
            amount.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.amountColor, theme))

            blockies.setAddress(viewTransfer.address)
            ellipsizedAddress.text = viewTransfer.address.formatForTxList()
            nonce.text = viewTransfer.nonce

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.id)
            }
        }
    }
}

class CustomTransactionViewHolder(private val viewBinding: ItemTxTransferBinding) :
    BaseTransactionViewHolder<TransactionView.CustomTransaction>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.CustomTransaction, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme
        with(viewBinding) {
            txTypeIcon.setImageResource(R.drawable.ic_code_16dp)

            finalStatus.setText(viewTransfer.statusText)
            finalStatus.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))
            dateTime.text = viewTransfer.dateTimeText
            blockies.setAddress(viewTransfer.address)
            ellipsizedAddress.text = viewTransfer.address.formatForTxList()
            dataSize.text = viewTransfer.dataSizeText
            amount.text = viewTransfer.amountText
            amount.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.amountColor, theme))
            nonce.text = viewTransfer.nonce

            finalStatus.alpha = OPACITY_FULL
            txTypeIcon.alpha = viewTransfer.alpha
            dateTime.alpha = viewTransfer.alpha
            blockies.alpha = viewTransfer.alpha
            ellipsizedAddress.alpha = viewTransfer.alpha
            dataSize.alpha = viewTransfer.alpha
            amount.alpha = viewTransfer.alpha
            nonce.alpha = viewTransfer.alpha

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.id)
            }
        }
    }
}

class CreationTransactionViewHolder(private val viewBinding: ItemTxSettingsChangeBinding) :
    BaseTransactionViewHolder<TransactionView.Creation>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.Creation, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme
        with(viewBinding) {
            finalStatus.setText(viewTransfer.statusText)
            finalStatus.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))

            dateTime.text = viewTransfer.dateTimeText
            settingName.setText(viewTransfer.label)

            if (viewTransfer.creationDetails != null) {
                root.setOnClickListener {
                    navigateToCreationDetails(it, viewTransfer.creationDetails)
                }
            }
        }
    }
}

private fun navigateToCreationDetails(view: View, details: TransactionView.CreationDetails) {
    Navigation.findNavController(view)
        .navigate(
            TransactionListFragmentDirections.actionTransactionListFragmentToTransactionCreationDetailsFragment(
                statusColorRes = details.statusColorRes,
                statusTextRes = details.statusText,
                dateTimeText = details.dateTimeText,
                implementation = details.implementation,
                factory = details.factory,
                creator = details.creator,
                transActionHash = details.transactionHash
            )
        )
}

private fun navigateToTxDetails(view: View, id: String) {
    Navigation.findNavController(view).navigate(TransactionListFragmentDirections.actionTransactionListFragmentToTransactionDetailsFragment(id))
}

class SectionHeaderViewHolder(private val viewBinding: ItemTxSectionHeaderBinding) :
    BaseTransactionViewHolder<TransactionView.SectionHeader>(viewBinding) {

    override fun bind(sectionHeader: TransactionView.SectionHeader, payloads: List<Any>) {
        with(viewBinding) {
            sectionTitle.text = sectionHeader.title
        }
    }
}
