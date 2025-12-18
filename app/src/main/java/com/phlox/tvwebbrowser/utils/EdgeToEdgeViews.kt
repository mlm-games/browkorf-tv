package com.phlox.tvwebbrowser.utils

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max

object EdgeToEdgeViews {

    /**
     * This keeps legacy Views layouts working by applying padding for system bars/IME.
     */
    fun enable(activity: Activity, root: View) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            v.setPadding(
                sys.left,
                sys.top,
                sys.right,
                max(sys.bottom, if (imeVisible) ime.bottom else 0)
            )
            insets
        }

        ViewCompat.requestApplyInsets(root)
    }
}