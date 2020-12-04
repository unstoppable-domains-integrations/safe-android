package io.gnosis.safe.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import io.gnosis.data.models.Safe
import io.gnosis.safe.R
import io.gnosis.safe.databinding.FragmentSettingsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.SafeOverviewBaseFragment
import io.gnosis.safe.ui.safe.empty.NoSafeFragment
import io.gnosis.safe.ui.settings.app.AppSettingsFragment
import io.gnosis.safe.ui.settings.safe.SafeSettingsFragment
import javax.inject.Inject

class TransactionsFragment : SafeOverviewBaseFragment<FragmentSettingsBinding>() {

    private lateinit var pager: TransactionsPagerAdapter

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsBinding =
        FragmentSettingsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            pager = TransactionsPagerAdapter(this@TransactionsFragment)
            settingsContent.adapter = pager
            TabLayoutMediator(settingsTabBar, settingsContent, true) { tab, position ->
                when (TransactionsPagerAdapter.Tabs.values()[position]) {
                    TransactionsPagerAdapter.Tabs.QUEUE -> {
                        tab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_settings_safe_24dp)
                        tab.text = "Queue"
                    }
                    TransactionsPagerAdapter.Tabs.HISTORY -> {
                        tab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_settings_app_24dp)
                        tab.text = "History"
                    }
                }
            }.attach()
        }
    }

    override fun handleActiveSafe(safe: Safe?) {
        navHandler?.setSafeData(safe)
    }

    override fun screenId() = null
}

class TransactionsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    enum class Tabs { QUEUE, HISTORY }

    enum class Items(val value: Long) {
        QUEUE(Tabs.QUEUE.ordinal.toLong()),
        HISTORY(Tabs.HISTORY.ordinal.toLong()),
        NO_SAFE(RecyclerView.NO_ID) }

    var noActiveSafe: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = Tabs.values().size

    override fun createFragment(position: Int): Fragment {
        if (noActiveSafe) return NoSafeFragment.newInstance(NoSafeFragment.Position.SETTINGS)
        return when (Tabs.values()[position]) {
            Tabs.QUEUE -> TransactionListFragment.newQueueInstance()
            Tabs.HISTORY -> TransactionListFragment.newHistoryInstance()
        }
    }

    override fun getItemId(position: Int): Long {
        return when (Tabs.values()[position]) {
            Tabs.QUEUE -> if (noActiveSafe) Items.NO_SAFE.value else Items.QUEUE.value
            Tabs.HISTORY -> Items.HISTORY.value
        }
    }

    override fun containsItem(itemId: Long): Boolean {
        return when (itemId) {
            Items.NO_SAFE.value -> noActiveSafe
            Items.QUEUE.value -> !noActiveSafe
            else -> true
        }
    }
}
