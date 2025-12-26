package com.yourname.xprime

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.MainAPI

@CloudstreamPlugin
class XprimePlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(XPrimeProvider())
    }
}
