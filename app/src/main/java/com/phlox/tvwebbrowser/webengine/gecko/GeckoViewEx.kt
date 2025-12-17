package com.phlox.tvwebbrowser.webengine.gecko

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoView
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.core.graphics.scale

open class GeckoViewEx @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GeckoView(context, attrs) {

    /**
     * Modern GeckoView API: capturePixels() is public and documented.
     * Must be called on UI thread; capturePixels completes with a Bitmap of the currently visible surface.
     */
    suspend fun renderThumbnail(existing: Bitmap?): Bitmap? = suspendCoroutine { cont ->
        post {
            val r: GeckoResult<Bitmap> =
                try {
                    capturePixels()
                } catch (_: Throwable) {
                    cont.resume(null)
                    return@post
                }

            r.then(
                { full ->
                    full!!
                    // Downscale to reduce memory; match legacy “thumbnail” intent.
                    val targetW = (full.width / 2).coerceAtLeast(1)
                    val targetH = (full.height / 2).coerceAtLeast(1)

                    val thumb =
                        try {
                            full.scale(targetW, targetH)
                        } catch (_: Throwable) {
                            null
                        }

                    cont.resume(thumb)
                    GeckoResult.fromValue(null)
                },
                { _ ->
                    cont.resume(null)
                    GeckoResult.fromValue(null)
                }
            )
        }
    }
}