package com.example.expencetrackerapp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.expencetrackerapp.data.database.entities.Expense
import com.example.expencetrackerapp.data.database.entities.TransactionType
import com.example.expencetrackerapp.ui.theme.*
import com.example.expencetrackerapp.util.CurrencyFormatter
import com.example.expencetrackerapp.util.DateUtils

/**
 * Clean glassmorphic expense card - no unnecessary animations
 */
@Composable
fun ExpenseCard(
    expense: Expense,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    useGlass: Boolean = false
) {
    val source = detectBankSource(expense.smsBody, expense.accountNumber)
    val isExpense = expense.transactionType == TransactionType.DEBIT
    val categoryColor = getCategoryColor(expense.category)
    
    if (useGlass) {
        GlassRefractiveBox(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(18.dp),
            warpStrength = 0.08f,
            edgeThickness = 0.05f
        ) {
            ExpenseCardContent(expense, categoryColor, source, isExpense)
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable(onClick = onClick)
        ) {
            ExpenseCardContent(expense, categoryColor, source, isExpense)
        }
    }
}

@Composable
private fun ExpenseCardContent(
    expense: Expense,
    categoryColor: Color,
    source: String?,
    isExpense: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Subtle category tint
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            categoryColor.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(categoryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(expense.category),
                    contentDescription = expense.category,
                    tint = categoryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            // Merchant and category info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = expense.merchant,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = expense.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (source != null) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = source,
                            style = MaterialTheme.typography.labelSmall,
                            color = getBankColor(source)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Amount and time
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = CurrencyFormatter.formatWithSign(expense.amount, isExpense),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpense) Secondary else Primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = DateUtils.getRelativeDate(expense.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Detect bank source from SMS body or account number.
 */
private fun detectBankSource(smsBody: String?, accountNumber: String?): String? {
    val text = (smsBody ?: "").uppercase()
    
    return when {
        text.contains("HDFC") && text.contains("CREDIT CARD") -> "HDFC CC"
        text.contains("HDFC") && text.contains("CARD") -> "HDFC Card"
        text.contains("HDFC") -> "HDFC"
        text.contains("SBI") && text.contains("UPI") -> "SBI UPI"
        text.contains("SBI") -> "SBI"
        text.contains("ICICI") -> "ICICI"
        text.contains("AXIS") -> "Axis"
        text.contains("KOTAK") -> "Kotak"
        text.contains("PNB") -> "PNB"
        text.contains("BOB") || text.contains("BANK OF BARODA") -> "BOB"
        text.contains("IDFC") -> "IDFC"
        text.contains("YES BANK") -> "Yes Bank"
        text.contains("RBL") -> "RBL"
        text.contains("INDUSIND") -> "IndusInd"
        text.contains("FEDERAL") -> "Federal"
        text.contains("CANARA") -> "Canara"
        text.contains("PAYTM") -> "Paytm"
        text.contains("PHONEPE") || text.contains("PHONE PE") -> "PhonePe"
        text.contains("GPAY") || text.contains("GOOGLE PAY") -> "GPay"
        text.contains("AMAZON") && text.contains("PAY") -> "Amazon Pay"
        text.contains("CREDIT CARD") -> "Credit Card"
        text.contains("UPI") -> "UPI"
        else -> null
    }
}

/**
 * Get color for bank badge.
 */
private fun getBankColor(source: String): Color {
    return when {
        source.contains("HDFC") -> Color(0xFF004C8F)  // HDFC Blue
        source.contains("SBI") -> Color(0xFF1A4480)   // SBI Blue
        source.contains("ICICI") -> Color(0xFFB02A2A) // ICICI Red
        source.contains("Axis") -> Color(0xFF800020)  // Axis Maroon
        source.contains("Kotak") -> Color(0xFFED1C24) // Kotak Red
        source.contains("Paytm") -> Color(0xFF00BAF2) // Paytm Blue
        source.contains("PhonePe") -> Color(0xFF5F259F) // PhonePe Purple
        source.contains("GPay") -> Color(0xFF4285F4)  // Google Blue
        source.contains("UPI") -> Color(0xFF097969)   // UPI Green
        source.contains("Credit") -> Color(0xFFDAA520) // Gold
        else -> Color(0xFF666666)
    }
}

/**
 * 💎 Premium Summary card with gradient background
 */
@Composable
fun SpendingSummaryCard(
    title: String,
    amount: Double,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(getPrimaryGradient())
                .padding(28.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = CurrencyFormatter.format(amount),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * Simple category chip
 */
@Composable
fun CategoryChip(
    categoryName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = getCategoryColor(categoryName)
    
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) categoryColor else categoryColor.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = getCategoryIcon(categoryName),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) Color.White else categoryColor
            )
            Text(
                text = categoryName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) Color.White else categoryColor
            )
        }
    }
}

/**
 * 🎯 Beautiful empty state with animation
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * Get icon for category name.
 */
fun getCategoryIcon(categoryName: String): ImageVector {
    return when (categoryName.lowercase()) {
        "food & dining", "food" -> Icons.Filled.Restaurant
        "shopping" -> Icons.Filled.ShoppingCart
        "transport" -> Icons.Filled.DirectionsCar
        "fashion" -> Icons.Filled.Checkroom
        "entertainment" -> Icons.Filled.Movie
        "bills & utilities", "bills" -> Icons.Filled.Receipt
        "health" -> Icons.Filled.LocalHospital
        "education" -> Icons.Filled.School
        "travel" -> Icons.Filled.Flight
        "investments" -> Icons.Filled.TrendingUp
        "transfers" -> Icons.Filled.SwapHoriz
        else -> Icons.Filled.Category
    }
}
