package org.mlm.browkorftv.settings

import org.mozilla.geckoview.GeckoRuntimeSettings

fun Theme.toGeckoPreferredColorScheme(forceDarkWebpage: Boolean): Int {
    return when (this) {
        Theme.SYSTEM -> GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM
        Theme.WHITE -> GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
        Theme.BLACK -> if (forceDarkWebpage) GeckoRuntimeSettings.COLOR_SCHEME_DARK 
                       else GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
    }
}