package org.mlm.browkorftv.settings

import org.mozilla.geckoview.GeckoRuntimeSettings

fun Theme.toGeckoPreferredColorScheme(): Int {
    return when (this) {
        Theme.SYSTEM -> GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM
        Theme.WHITE -> GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
        Theme.BLACK -> GeckoRuntimeSettings.COLOR_SCHEME_DARK
    }
}