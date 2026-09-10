package com.ajaxjiang.folddepth

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ajaxjiang.folddepth.display.OuterDisplayManager
import com.ajaxjiang.folddepth.sensor.HingeAngleSource
import com.ajaxjiang.folddepth.ui.FoldDepthDemo
import com.ajaxjiang.folddepth.util.MediaStoreHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        setContent {
            val hingeSource = remember { HingeAngleSource(applicationContext) }
            val outerDisplayManager = remember { OuterDisplayManager(this) }
            val foldState by hingeSource.foldState
            val lifecycleOwner = LocalLifecycleOwner.current

            var latestGalleryBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

            // Permission handling for reading latest gallery image
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_IMAGES
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) { isGranted ->
                if (isGranted) {
                    latestGalleryBitmap = MediaStoreHelper.loadLatestGalleryImage(applicationContext)
                }
            }

            LaunchedEffect(Unit) {
                if (ContextCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_GRANTED) {
                    latestGalleryBitmap = MediaStoreHelper.loadLatestGalleryImage(applicationContext)
                } else {
                    permissionLauncher.launch(permission)
                }
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> hingeSource.start()
                        Lifecycle.Event.ON_STOP -> {
                            hingeSource.stop()
                            outerDisplayManager.release()
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    hingeSource.stop()
                    outerDisplayManager.release()
                }
            }

            LaunchedEffect(foldState, latestGalleryBitmap) {
                outerDisplayManager.update(foldState, latestGalleryBitmap)
            }

            FoldDepthDemo(
                foldState = foldState,
                initialCustomBitmap = latestGalleryBitmap,
                onSimulateAngleChange = { angle -> hingeSource.setSimulatedAngle(angle) },
                onToggleSimulation = { enabled -> hingeSource.setSimulated(enabled) },
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}
