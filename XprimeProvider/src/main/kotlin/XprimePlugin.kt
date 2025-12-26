package com.yourname.xprime

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class XprimePlugin : Plugin() {

    override fun load() {
        registerMainAPI(XPrimeProvider())
    }
}
