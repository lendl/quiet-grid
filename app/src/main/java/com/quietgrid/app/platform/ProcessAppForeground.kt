package com.quietgrid.app.platform

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.quietgrid.app.session.AppForeground

class ProcessAppForeground : AppForeground {
    override fun isForeground(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
}
