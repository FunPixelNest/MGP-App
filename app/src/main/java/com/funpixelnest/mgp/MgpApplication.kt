package com.funpixelnest.mgp

import android.app.Application
import com.funpixelnest.mgp.data.PlayerStore

class MgpApplication : Application() {
    val store: PlayerStore by lazy { PlayerStore(this) }
}
