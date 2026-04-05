package org.mlm.browkorftv.settings

import org.mozilla.geckoview.GeckoRuntimeSettings

fun Theme.toGeckoPreferredColorScheme(forceDarkWebpage: Boolean): Int {
    return when (this) {
        Theme.System -> GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM
        Theme.White -> GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
        Theme.Black -> if (forceDarkWebpage) GeckoRuntimeSettings.COLOR_SCHEME_DARK
                       else GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
    }
}