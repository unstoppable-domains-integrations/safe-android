package io.gnosis.safe.di.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.gnosis.safe.di.ViewModelFactory
import io.gnosis.safe.di.ViewModelKey
import io.gnosis.safe.ui.assets.AssetsViewModel
import io.gnosis.safe.ui.assets.coins.CoinsViewModel
import io.gnosis.safe.ui.assets.collectibles.CollectiblesViewModel
import io.gnosis.safe.ui.dialogs.EnsInputViewModel
import io.gnosis.safe.ui.safe.add.AddSafeNameViewModel
import io.gnosis.safe.ui.safe.add.AddSafeViewModel
import io.gnosis.safe.ui.safe.selection.SafeSelectionViewModel
import io.gnosis.safe.ui.safe.share.ShareSafeViewModel
import io.gnosis.safe.ui.settings.SettingsViewModel
import io.gnosis.safe.ui.settings.app.AppSettingsViewModel
import io.gnosis.safe.ui.settings.app.GetInTouchViewModel
import io.gnosis.safe.ui.settings.app.fiat.AppFiatViewModel
import io.gnosis.safe.ui.settings.owner.OwnerSeedPhraseViewModel
import io.gnosis.safe.ui.settings.safe.AdvancedSafeSettingsViewModel
import io.gnosis.safe.ui.settings.safe.SafeSettingsEditNameViewModel
import io.gnosis.safe.ui.settings.safe.SafeSettingsViewModel
import io.gnosis.safe.ui.settings.owner.list.OwnerSelectionViewModel
import io.gnosis.safe.ui.splash.SplashViewModel
import io.gnosis.safe.ui.transactions.TransactionListViewModel
import io.gnosis.safe.ui.transactions.TransactionsViewModel
import io.gnosis.safe.ui.transactions.details.TransactionDetailsViewModel
import javax.inject.Singleton

@Module
abstract class ViewModelFactoryModule {

    @Binds
    @IntoMap
    @ViewModelKey(AddSafeViewModel::class)
    abstract fun bindsAddSafeViewModel(viewModel: AddSafeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SplashViewModel::class)
    abstract fun bindsSplashViewModel(viewModel: SplashViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(EnsInputViewModel::class)
    abstract fun providesEnsInputViewModel(viewModel: EnsInputViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddSafeNameViewModel::class)
    abstract fun providesAddSafeNameViewModel(viewModel: AddSafeNameViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CoinsViewModel::class)
    abstract fun providesCoinsViewModel(viewModel: CoinsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CollectiblesViewModel::class)
    abstract fun providesCollectiblesViewModel(viewModel: CollectiblesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AssetsViewModel::class)
    abstract fun providesAssetsViewModel(viewModel: AssetsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeSelectionViewModel::class)
    abstract fun providesSafeSelectionViewModel(viewModel: SafeSelectionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(OwnerSelectionViewModel::class)
    abstract fun providesOwnerSelectionViewModel(viewModel: OwnerSelectionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    abstract fun providesSettingsViewModel(viewModel: SettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeSettingsViewModel::class)
    abstract fun providesSafeSettingsViewModel(viewModel: SafeSettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SafeSettingsEditNameViewModel::class)
    abstract fun providesSafeSettingsEditNameViewModel(viewModel: SafeSettingsEditNameViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AppSettingsViewModel::class)
    abstract fun providesAppSettingsViewModel(viewModel: AppSettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TransactionsViewModel::class)
    abstract fun providesTransactionsViewModel(viewModel: TransactionsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TransactionListViewModel::class)
    abstract fun providesTransactionListViewModel(viewModel: TransactionListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TransactionDetailsViewModel::class)
    abstract fun providesTransactionDetailsViewModel(viewModel: TransactionDetailsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AdvancedSafeSettingsViewModel::class)
    abstract fun providesAdvancedSafeSettingsViewModel(viewModel: AdvancedSafeSettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ShareSafeViewModel::class)
    abstract fun providesSharedSafeViewModel(viewModel: ShareSafeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GetInTouchViewModel::class)
    abstract fun providesGetInTouchViewModel(viewModel: GetInTouchViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(OwnerSeedPhraseViewModel::class)
    abstract fun providesOwnerSeedPhraseViewModel(viewModel: OwnerSeedPhraseViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AppFiatViewModel::class)
    abstract fun providesAppFiatViewModel(viewModel: AppFiatViewModel): ViewModel

    @Binds
    @Singleton
    abstract fun bindsViewModelFactory(viewModel: ViewModelFactory): ViewModelProvider.Factory
}
