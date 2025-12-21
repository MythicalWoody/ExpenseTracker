package com.example.expencetrackerapp.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Colors - Modern Dark Theme
val DarkBackground = Color(0xFF0D1117)
val DarkSurface = Color(0xFF161B22)
val DarkSurfaceVariant = Color(0xFF21262D)
val DarkCard = Color(0xFF1C2128)

// Light Theme Colors
val LightBackground = Color(0xFFF6F8FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F2F5)
val LightCard = Color(0xFFFFFFFF)

// Primary accent - Emerald Green (for positive/income)
val Primary = Color(0xFF10B981)
val PrimaryLight = Color(0xFF34D399)
val PrimaryDark = Color(0xFF059669)

// Secondary accent - Coral Red (for expenses)
val Secondary = Color(0xFFFF6B6B)
val SecondaryLight = Color(0xFFFF8787)
val SecondaryDark = Color(0xFFE55656)

// Category Colors
val CategoryFood = Color(0xFFFF6B6B)
val CategoryShopping = Color(0xFF4ECDC4)
val CategoryTransport = Color(0xFF45B7D1)
val CategoryFashion = Color(0xFF96CEB4)
val CategoryEntertainment = Color(0xFFDDA0DD)
val CategoryBills = Color(0xFFF7DC6F)
val CategoryHealth = Color(0xFF82E0AA)
val CategoryEducation = Color(0xFF85C1E9)
val CategoryTravel = Color(0xFFF8B500)
val CategoryInvestments = Color(0xFF27AE60)
val CategoryTransfers = Color(0xFF9B59B6)
val CategoryOthers = Color(0xFF95A5A6)

// Text Colors
val TextPrimary = Color(0xFFF0F6FC)
val TextSecondary = Color(0xFF8B949E)
val TextTertiary = Color(0xFF6E7681)

val TextPrimaryLight = Color(0xFF1F2937)
val TextSecondaryLight = Color(0xFF6B7280)
val TextTertiaryLight = Color(0xFF9CA3AF)

// Status Colors
val Success = Color(0xFF22C55E)
val Warning = Color(0xFFF59E0B)
val Error = Color(0xFFEF4444)
val Info = Color(0xFF3B82F6)

// Legacy colors for compatibility
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/**
 * Get color for a category by name
 */
fun getCategoryColor(categoryName: String): Color {
    return when (categoryName.lowercase()) {
        "food & dining", "food" -> CategoryFood
        "shopping" -> CategoryShopping
        "transport" -> CategoryTransport
        "fashion" -> CategoryFashion
        "entertainment" -> CategoryEntertainment
        "bills & utilities", "bills" -> CategoryBills
        "health" -> CategoryHealth
        "education" -> CategoryEducation
        "travel" -> CategoryTravel
        "investments" -> CategoryInvestments
        "transfers" -> CategoryTransfers
        else -> CategoryOthers
    }
}

/**
 * Parse hex color string to Color
 */
fun parseColor(hexColor: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hexColor))
    } catch (e: Exception) {
        CategoryOthers
    }
}