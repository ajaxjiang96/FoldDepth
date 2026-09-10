package com.ajaxjiang.folddepth.display

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.ComposeView
import com.ajaxjiang.folddepth.model.FoldState
import com.ajaxjiang.folddepth.ui.OuterScreenView

/**
 * Manages physical secondary/cover displays on foldable hardware using Android Presentation API.
 */
class OuterDisplayManager(
    private val context: Context,
) {
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    private var presentation: OuterScreenPresentation? = null

    val isPhysicalSecondaryDisplayAvailable: Boolean
        get() {
            val displays = displayManager?.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            return !displays.isNullOrEmpty()
        }

    fun update(
        foldState: FoldState,
        customBitmap: ImageBitmap?,
    ) {
        val displays = displayManager?.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
        val outerDisplay = displays?.firstOrNull()

        if (outerDisplay == null) {
            presentation?.dismiss()
            presentation = null
            return
        }

        if (foldState.angle <= 90f) {
            if (presentation == null || presentation?.display != outerDisplay) {
                presentation?.dismiss()
                presentation = OuterScreenPresentation(
                    context = context,
                    display = outerDisplay,
                ).apply {
                    updateContent(foldState, customBitmap)
                    try {
                        show()
                    } catch (_: Exception) {
                        // Handle devices with strict display presentation policies
                    }
                }
            } else {
                presentation?.updateContent(foldState, customBitmap)
                if (presentation?.isShowing == false) {
                    try {
                        presentation?.show()
                    } catch (_: Exception) {
                    }
                }
            }
        } else {
            if (presentation?.isShowing == true) {
                presentation?.dismiss()
            }
        }
    }

    fun release() {
        try {
            presentation?.dismiss()
        } catch (_: Exception) {
        }
        presentation = null
    }

    private class OuterScreenPresentation(
        context: Context,
        display: Display,
    ) : Presentation(context, display) {
        private var composeView: ComposeView? = null
        private var currentState: FoldState? = null
        private var currentBitmap: ImageBitmap? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_PANEL)

            val view = ComposeView(context).apply {
                setContent {
                    currentState?.let { state ->
                        OuterScreenView(
                            foldState = state,
                            customBitmap = currentBitmap,
                        )
                    }
                }
            }
            setContentView(view)
            composeView = view
        }

        fun updateContent(foldState: FoldState, customBitmap: ImageBitmap?) {
            currentState = foldState
            currentBitmap = customBitmap
            composeView?.setContent {
                OuterScreenView(
                    foldState = foldState,
                    customBitmap = customBitmap,
                )
            }
        }
    }
}
