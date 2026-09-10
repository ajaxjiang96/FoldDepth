package com.ajaxjiang.folddepth.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.InputStream

/**
 * High-fidelity foldable wallpaper and desktop mockup surface.
 * Can render either the Left Half (folding) or Right Half (stationary/outer display),
 * supporting both built-in procedural wallpaper and user-imported screenshots.
 */
@Composable
fun WallpaperHalfView(
    isLeftHalf: Boolean,
    customBitmap: ImageBitmap? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (customBitmap != null) Color.Transparent else Color(0xFF0D0E15)),
    ) {
        if (customBitmap != null) {
            // Render cropped half of custom image
            CustomBitmapHalf(
                bitmap = customBitmap,
                isLeftHalf = isLeftHalf,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Render procedural wallpaper and foldable desktop elements
            ProceduralWallpaperBackground(isLeftHalf = isLeftHalf)

            if (isLeftHalf) {
                LeftHalfDesktopContent()
            } else {
                RightHalfDesktopContent()
            }
        }
    }
}

@Composable
private fun CustomBitmapHalf(
    bitmap: ImageBitmap,
    isLeftHalf: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val bmpW = bitmap.width
        val bmpH = bitmap.height

        val srcLeft = if (isLeftHalf) 0 else bmpW / 2
        val srcRight = if (isLeftHalf) bmpW / 2 else bmpW
        val srcRect = IntRect(srcLeft, 0, srcRight, bmpH)
        val dstSize = IntSize(size.width.toInt(), size.height.toInt())

        drawImage(
            image = bitmap,
            srcOffset = IntOffset(srcRect.left, srcRect.top),
            srcSize = IntSize(srcRect.width, srcRect.height),
            dstOffset = IntOffset.Zero,
            dstSize = dstSize,
        )
    }
}

@Composable
private fun ProceduralWallpaperBackground(isLeftHalf: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Rich cosmic dusk gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F172A),
                    Color(0xFF1E1B4B),
                    Color(0xFF311042),
                    Color(0xFF0B0F19),
                ),
                startY = 0f,
                endY = h,
            ),
        )

        // Glowing luminous focal orbs
        if (isLeftHalf) {
            // Violet-cyan aurora glow on left half
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(w * 0.4f, h * 0.35f),
                    radius = w * 0.9f,
                ),
                center = Offset(w * 0.4f, h * 0.35f),
                radius = w * 0.9f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF06B6D4).copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.7f),
                    radius = w * 0.7f,
                ),
                center = Offset(w * 0.85f, h * 0.7f),
                radius = w * 0.7f,
            )
        } else {
            // Amber-coral luminous glow on right half
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF43F5E).copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(w * 0.15f, h * 0.7f),
                    radius = w * 0.8f,
                ),
                center = Offset(w * 0.15f, h * 0.7f),
                radius = w * 0.8f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF59E0B).copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(w * 0.6f, h * 0.3f),
                    radius = w * 0.85f,
                ),
                center = Offset(w * 0.6f, h * 0.3f),
                radius = w * 0.85f,
            )
        }
    }
}

@Composable
private fun LeftHalfDesktopContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Top Clock & Date Widget
        Column {
            Text(
                text = "09:41",
                color = Color.White,
                fontSize = 52.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.SansSerif,
            )
            Text(
                text = "Thursday, September 10 · 26°C Sunny",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
            )
        }

        // Mid Cards (Calendar / Quick Task)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "NEXT EVENT",
                    color = Color(0xFF67E8F9),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Foldable Architecture Review",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "10:30 AM – 11:30 AM · Design Lab",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                )
            }
        }

        // Left Half App Grid (4 apps)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            AppIconItem("Camera", Color(0xFFEF4444))
            AppIconItem("Photos", Color(0xFF3B82F6))
            AppIconItem("Notes", Color(0xFFEAB308))
            AppIconItem("Settings", Color(0xFF64748B))
        }
    }
}

@Composable
private fun RightHalfDesktopContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Top Search Bar
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.1f),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF38BDF8), CircleShape),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Search apps, documents & web...",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                )
            }
        }

        // Media Player Widget
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFFF43F5E), Color(0xFF8B5CF6)),
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ),
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Depth Field Horizon",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Spatial Dynamics · Playing",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // Right Half App Grid (4 apps)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            AppIconItem("Maps", Color(0xFF10B981))
            AppIconItem("Browser", Color(0xFF8B5CF6))
            AppIconItem("Terminal", Color(0xFF0F172A))
            AppIconItem("Files", Color(0xFFF59E0B))
        }
    }
}

@Composable
private fun AppIconItem(name: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(color, RoundedCornerShape(14.dp))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = name,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
        )
    }
}
