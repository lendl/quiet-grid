package com.quietgrid.app.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {
    @Binds
    abstract fun bindSessionStore(impl: SessionRepository): SessionStore

    @Binds
    abstract fun bindStatsStore(impl: StatsRepository): StatsStore

    @Binds
    abstract fun bindPlayHistoryStore(impl: PlayHistoryRepository): PlayHistoryStore
}
