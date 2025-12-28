package com.example.expencetrackerapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.expencetrackerapp.data.database.dao.CategorySpending
import com.example.expencetrackerapp.ui.theme.getCategoryColor
import com.example.expencetrackerapp.util.CurrencyFormatter
import androidx.compose.ui.graphics.Brush
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive donut/pie chart showing spending by category.
 * Tap on a slice to see details.
 */
@Composable
fun InteractivePieChart(
    data: List<CategorySpending>,
    totalAmount: Double,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }
    
    // Calculate angles for each slice
    val slices = remember(data, totalAmount) {
        var startAngle = -90f
        data.mapIndexed { index, spending ->
            val sweepAngle = ((spending.total / totalAmount) * 360f).toFloat()
            val slice = PieSlice(
                category = spending.category,
                amount = spending.total,
                percentage = (spending.total / totalAmount * 100).toFloat(),
                color = getCategoryColor(spending.category),
                startAngle = startAngle,
                sweepAngle = sweepAngle
            )
            startAngle += sweepAngle
            slice
        }
    }
    
    GlassRefractiveBox(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Spending Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Pie Chart
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(200.dp)
                        .pointerInput(slices) {
                            detectTapGestures { offset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val tapAngle = calculateAngle(offset, center)
                                
                                selectedIndex = slices.indexOfFirst { slice ->
                                    val normalizedStart = (slice.startAngle + 360) % 360
                                    val normalizedEnd = (normalizedStart + slice.sweepAngle) % 360
                                    
                                    if (normalizedStart < normalizedEnd) {
                                        tapAngle >= normalizedStart && tapAngle < normalizedEnd
                                    } else {
                                        tapAngle >= normalizedStart || tapAngle < normalizedEnd
                                    }
                                }.takeIf { it >= 0 }
                            }
                        }
                ) {
                    val strokeWidth = 45.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val topLeft = Offset(
                        (size.width - 2 * radius - strokeWidth) / 2 + strokeWidth / 2,
                        (size.height - 2 * radius - strokeWidth) / 2 + strokeWidth / 2
                    )
                    val arcSize = Size(radius * 2, radius * 2)
                    
                    slices.forEachIndexed { index, slice ->
                        val isSelected = selectedIndex == index
                        val animatedSweep = slice.sweepAngle * animatedProgress.value
                        
                        // Draw glassmorphic arc with category color
                        drawGlassmorphicArc(
                            color = slice.color,
                            startAngle = slice.startAngle,
                            sweepAngle = animatedSweep,
                            topLeft = topLeft,
                            size = arcSize,
                            strokeWidth = strokeWidth,
                            isSelected = isSelected,
                            glassIntensity = 0.75f
                        )
                    }
                }
                
                // Center text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedIndex != null && selectedIndex!! < slices.size) {
                        val selected = slices[selectedIndex!!]
                        Text(
                            text = selected.category,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = CurrencyFormatter.format(selected.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = selected.color
                        )
                        Text(
                            text = "${selected.percentage.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.formatCompact(totalAmount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap slice for details",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Legend
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                slices.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { slice ->
                            LegendItem(
                                color = slice.color,
                                label = slice.category,
                                percentage = slice.percentage,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill empty space if odd number
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    percentage: Float,
    modifier: Modifier = Modifier
) {
    GlassRefractiveBox(
        modifier = modifier.padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp),
        rimWidth = 1.dp,
        glassThickness = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${percentage.toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
    }
}

private fun calculateAngle(point: Offset, center: Offset): Float {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val angleRad = atan2(dy, dx)
    var angleDeg = (angleRad * 180f / PI).toFloat()
    angleDeg = (angleDeg + 360) % 360
    return angleDeg
}

private data class PieSlice(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val color: Color,
    val startAngle: Float,
    val sweepAngle: Float
)
