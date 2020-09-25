package io.gnosis.safe.ui.transactions.details.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ViewLabeledValueItemBinding
import timber.log.Timber

class LabeledValueItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewLabeledValueItemBinding.inflate(LayoutInflater.from(context), this) }

    init {
        readAttributesAndSetupFields(context, attrs)
    }

    var label: CharSequence? = null
        set(value) {
            binding.label.text = value
            field = value
        }

    var value: CharSequence? = null
        set(value) {
            binding.value.text = value
            field = value
        }

    private fun readAttributesAndSetupFields(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.LabeledValueItem,
            0, 0
        ).also {
            runCatching {
                applyAttributes(context, it)
            }
                .onFailure { Timber.e(it) }
            it.recycle()
        }
    }

    private fun applyAttributes(context: Context, a: TypedArray) {
        label = a.getString(R.styleable.LabeledValueItem_item_label)
    }
}
