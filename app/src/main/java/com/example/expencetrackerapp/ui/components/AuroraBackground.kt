package com.example.expencetrackerapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    // 1. Infinite transition for movement
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    // Animate positions for 3 large blobs
    val blob1Offset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 200f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Reverse),
        label = "blob1"
    )
    val blob2Offset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -150f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
        label = "blob2"
    )
    val blob3Offset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 100f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
        label = "blob3"
    )

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.99f }) {
            val w = size.width
            val h = size.height

            // Base Deep Gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A0B2E), // Deep Purple
                        Color(0xFF0D0D15)  // Near Black
                    )
                )
            )

            // Blob 1: Pink/Magenta (Top Left)
            drawCircle(
                color = Color(0xFFFF2D55).copy(alpha = 0.6f),
                radius = w * 0.6f,
                center = Offset(w * 0.2f + blob1Offset, h * 0.3f - blob2Offset * 0.5f)
            )

            // Blob 2: Cyan/Blue (Bottom Right)
            drawCircle(
                color = Color(0xFF00C7BE).copy(alpha = 0.5f),
                radius = w * 0.5f,
                center = Offset(w * 0.8f - blob1Offset, h * 0.7f + blob3Offset)
            )

            // Blob 3: Purple/Violet (Center moving)
            drawCircle(
                color = Color(0xFFAF52DE).copy(alpha = 0.5f),
                radius = w * 0.7f,
                center = Offset(w * 0.5f + blob2Offset, h * 0.5f + blob1Offset * 0.5f)
            )
        }
        
        // Apply a massive blur over the whole canvas to mix the blobs into a mesh
        // This simulates the "Aurora" mesh gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                // We overlay a semi-transparent dark layer to keep text readable
                .background(Color.Black.copy(alpha = 0.3f))
        )
    }
}

