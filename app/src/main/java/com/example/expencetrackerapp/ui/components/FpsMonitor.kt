package com.example.expencetrackerapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FpsMonitor(modifier: Modifier = Modifier) {
    var fps by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        var startTime = System.nanoTime()
        var frameCount = 0

        while (true) {
            // Suspends until next frame
            withFrameNanos {}

            frameCount++
            val now = System.nanoTime()
            val elapsed = now - startTime

            // Update FPS every 500ms
            if (elapsed >= 500_000_000) {
                fps = ((frameCount * 1_000_000_000.0) / elapsed).toInt()
                startTime = now
                frameCount = 0
            }
        }
    }

    Box(
            modifier =
                    modifier.background(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
                text = "FPS: $fps",
                color = Color.Green,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
        )
    }
}
