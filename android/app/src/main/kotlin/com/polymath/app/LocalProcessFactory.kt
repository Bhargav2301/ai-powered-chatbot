package com.polymath.app

import android.app.Application
import android.os.Process
import android.app.AppComponentFactory

/** The permissionless worker must not initialize Hilt, Room, news workers or image networking. */
class LocalProcessFactory : AppComponentFactory() {
    override fun instantiateApplication(cl: ClassLoader, className: String): Application =
        if (Process.isIsolated()) Application() else super.instantiateApplication(cl, className)
}
