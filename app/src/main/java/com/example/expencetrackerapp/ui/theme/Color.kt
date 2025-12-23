package com.example.expencetrackerapp.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================================
// 🎨 PREMIUM COLOR PALETTE - Designed for Maximum Visual Impact
// ============================================================================

// Dark Theme - Deep, Rich, Premium
val DarkBackground = Color(0xFF0A0E27)           // Deep midnight blue
val DarkSurface = Color(0xFF141830)              // Rich dark surface
val DarkSurfaceVariant = Color(0xFF1F2347)       // Elevated surface
val DarkCard = Color(0xFF1A1F3A)                 // Card background

// Light Theme - Clean, Bright, Airy
val LightBackground = Color(0xFFF8FAFC)          // Ultra light gray-blue
val LightSurface = Color(0xFFFFFFFF)             // Pure white
val LightSurfaceVariant = Color(0xFFF1F5F9)      // Subtle gray
val LightCard = Color(0xFFFFFFFF)                // Pure white cards

// ============================================================================
// 🌈 PRIMARY GRADIENT - Emerald to Teal (Money, Growth, Success)
// ============================================================================
val Primary = Color(0xFF10B981)                  // Emerald 500
val PrimaryLight = Color(0xFF34D399)             // Emerald 400
val PrimaryDark = Color(0xFF059669)              // Emerald 600
val PrimaryGradientStart = Color(0xFF06B6D4)    // Cyan 500
val PrimaryGradientEnd = Color(0xFF10B981)      // Emerald 500

// ============================================================================
// 🔥 SECONDARY GRADIENT - Vibrant Pink to Orange (Expense, Alert, Energy)
// ============================================================================
val Secondary = Color(0xFFFF6B6B)                // Coral red
val SecondaryLight = Color(0xFFFF8787)           // Light coral
val SecondaryDark = Color(0xFFE55656)            // Dark coral
val SecondaryGradientStart = Color(0xFFFF6B6B)  // Coral
val SecondaryGradientEnd = Color(0xFFFF8E53)    // Soft orange

// ============================================================================
// 🎨 ACCENT COLORS - Premium Purple & Amber
// ============================================================================
val Accent = Color(0xFF8B5CF6)                   // Vibrant purple
val AccentLight = Color(0xFFA78BFA)              // Light purple
val AccentDark = Color(0xFF7C3AED)               // Deep purple
val AccentAmber = Color(0xFFFBBF24)              // Bright amber
val AccentRose = Color(0xFFF43F5E)               // Rose red

// ============================================================================
// 🏷️ CATEGORY COLORS - Carefully Curated Vibrant Palette
// ============================================================================
val CategoryFood = Color(0xFFFF6B6B)             // Coral red
val CategoryShopping = Color(0xFF4ECDC4)         // Turquoise
val CategoryTransport = Color(0xFF45B7D1)        // Sky blue
val CategoryFashion = Color(0xFFEC4899)          // Hot pink
val CategoryEntertainment = Color(0xFFA78BFA)    // Purple
val CategoryBills = Color(0xFFFBBF24)            // Amber
val CategoryHealth = Color(0xFF10B981)           // Emerald
val CategoryEducation = Color(0xFF3B82F6)        // Blue
val CategoryTravel = Color(0xFFF59E0B)           // Orange
val CategoryInvestments = Color(0xFF059669)      // Dark emerald
val CategoryTransfers = Color(0xFF8B5CF6)        // Violet
val CategoryOthers = Color(0xFF94A3B8)           // Slate gray

// ============================================================================
// 📝 TEXT COLORS - Enhanced Readability
// ============================================================================
val TextPrimary = Color(0xFFF8FAFC)              // Almost white
val TextSecondary = Color(0xFF94A3B8)            // Cool gray
val TextTertiary = Color(0xFF64748B)             // Medium gray

val TextPrimaryLight = Color(0xFF0F172A)         // Almost black
val TextSecondaryLight = Color(0xFF475569)       // Dark gray
val TextTertiaryLight = Color(0xFF94A3B8)        // Light gray

// ============================================================================
// ✅ STATUS COLORS - Clear Visual Feedback
// ============================================================================
val Success = Color(0xFF10B981)                  // Emerald
val Warning = Color(0xFFFBBF24)                  // Amber
val Error = Color(0xFFEF4444)                    // Red
val Info = Color(0xFF3B82F6)                     // Blue

// ============================================================================
// ✨ SPECIAL EFFECTS - Glassmorphism & Overlays
// ============================================================================
val GlassWhite = Color(0x20FFFFFF)               // Frosted white overlay
val GlassBlack = Color(0x20000000)               // Frosted black overlay
val ShimmerHighlight = Color(0x40FFFFFF)         // Shimmer effect

// ============================================================================
// 🎨 GRADIENT BUILDERS - Reusable Gradient Functions
// ============================================================================

/**
 * Primary income/success gradient - Cyan to Emerald
 */
fun getPrimaryGradient() = Brush.linearGradient(
    colors = listOf(PrimaryGradientStart, PrimaryGradientEnd)
)

/**
 * Secondary expense/alert gradient - Coral to Orange
 */
fun getSecondaryGradient() = Brush.linearGradient(
    colors = listOf(SecondaryGradientStart, SecondaryGradientEnd)
)

/**
 * Premium purple gradient for special elements
 */
fun getAccentGradient() = Brush.linearGradient(
    colors = listOf(AccentLight, Accent, AccentDark)
)

/**
 * Sunset gradient - Beautiful warm tones
 */
fun getSunsetGradient() = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFA500),  // Orange
        Color(0xFFFF6B6B),  // Coral
        Color(0xFFEC4899)   // Pink
    )
)

/**
 * Ocean gradient - Cool, calming blues
 */
fun getOceanGradient() = Brush.linearGradient(
    colors = listOf(
        Color(0xFF06B6D4),  // Cyan
        Color(0xFF3B82F6),  // Blue
        Color(0xFF8B5CF6)   // Purple
    )
)

/**
 * Success gradient with glow effect
 */
fun getSuccessGradient() = Brush.radialGradient(
    colors = listOf(
        PrimaryLight.copy(alpha = 0.6f),
        Primary,
        PrimaryDark
    )
)

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