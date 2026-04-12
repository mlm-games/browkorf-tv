package org.mlm.browkorftv.settings

import org.mozilla.geckoview.GeckoRuntimeSettings

fun Theme.toGeckoPreferredColorScheme(forceDarkWebpage: Boolean): Int {
    if (!forceDarkWebpage) {
        return GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
    }

    return when (this) {
        Theme.System -> GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM
        Theme.White -> GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
        Theme.Black -> GeckoRuntimeSettings.COLOR_SCHEME_DARK
    }
}
