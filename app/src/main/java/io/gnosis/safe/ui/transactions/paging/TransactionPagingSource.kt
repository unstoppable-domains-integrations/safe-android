package io.gnosis.safe.ui.transactions.paging

import androidx.paging.PagingSource
import io.gnosis.data.models.transaction.UnifiedEntry
import io.gnosis.data.repositories.TransactionRepository
import pm.gnosis.model.Solidity
import timber.log.Timber


class TransactionPagingSource(
    private val safe: Solidity.Address,
    private val txRepo: TransactionRepository,
    private val history: Boolean
) : PagingSource<String, UnifiedEntry>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, UnifiedEntry> {

        val pageLink = params.key

        kotlin.runCatching {
            pageLink?.let { txRepo.loadUnifiedTransactionsPage(pageLink) }
                ?: if (history) txRepo.loadTransactionsHistory(safe) else txRepo.loadTransactionsQueue(safe)

        }.onSuccess {
            return LoadResult.Page(
                data = it.results,
                prevKey = it.previous,
                nextKey = it.next
            )
        }
            .onFailure {
                Timber.e(it)
                return LoadResult.Error(it)
            }

        throw IllegalStateException(javaClass.name)
    }
}
