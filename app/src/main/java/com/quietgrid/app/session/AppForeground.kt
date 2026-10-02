package com.quietgrid.app.session

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

fun interface AppForeground {
    fun isForeground(): Boolean
}

class ProcessAppForeground @Inject constructor() : AppForeground {
    override fun isForeground(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppForegroundModule {
    @Binds
    abstract fun bindAppForeground(impl: ProcessAppForeground): AppForeground
}
