package com.quietgrid.app.session

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner

fun interface AppForeground {
    fun isForeground(): Boolean
}

class ProcessAppForeground : AppForeground {
    override fun isForeground(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
}
