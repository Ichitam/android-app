package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.home.ConversationListFragment
import one.mixin.android.ui.home.circle.CirclesFragment
import one.mixin.android.ui.home.circle.ConversationCircleEditFragment
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.ui.search.SearchSingleFragment

@Module
abstract class MainActivityModule {

    @ContributesAndroidInjector
    internal abstract fun contributeConversationListFragment(): ConversationListFragment

    @ContributesAndroidInjector
    internal abstract fun contributeSearchFragment(): SearchFragment

    @ContributesAndroidInjector
    internal abstract fun contributeSearchSingleFragment(): SearchSingleFragment

    @ContributesAndroidInjector
    internal abstract fun contributeCirclesFragment(): CirclesFragment

    @ContributesAndroidInjector
    internal abstract fun contributeConversationCircleEditFragment(): ConversationCircleEditFragment
}
