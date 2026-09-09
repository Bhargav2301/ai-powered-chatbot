package com.polymath.app

import android.app.Application
import android.os.Process
import androidx.core.app.CoreComponentFactory

/** The permissionless worker must not initialize Hilt, Room, news workers or image networking. */
class LocalProcessFactory : CoreComponentFactory() {
    override fun instantiateApplication(cl: ClassLoader, className: String): Application =
        if (Process.isIsolated()) Application() else super.instantiateApplication(cl, className)
}
