package com.example.expencetrackerapp.domain.categorization

import com.example.expencetrackerapp.data.database.entities.Category
import com.example.expencetrackerapp.data.database.entities.MerchantMapping
import kotlin.math.min

/** Result of category matching attempt. */
data class CategoryMatchResult(
        val categoryName: String,
        val confidence: Float, // 0.0 to 1.0
        val matchedKeyword: String? = null
) {
    val isConfident: Boolean
        get() = confidence >= 0.6f
}

/**
 * Intelligent category matcher that uses:
 * 1. Learned merchant mappings (highest priority)
 * 2. Keyword matching
 * 3. Fuzzy matching for similar merchant names
 */
class CategoryMatcher {

    /**
     * Attempts to match a merchant name to a category.
     *
     * @param merchantName The merchant name from SMS
     * @param categories List of available categories with keywords
     * @param merchantMappings Previously learned merchant→category associations
     * @return CategoryMatchResult with the best matching category and confidence score
     */
    fun matchCategory(
            merchantName: String,
            categories: List<Category>,
            merchantMappings: List<MerchantMapping>
    ): CategoryMatchResult {
        val normalizedMerchant = merchantName.lowercase().trim()

        // 1. Check learned mappings first (exact match)
        val exactMapping =
                merchantMappings.find { it.merchantName.lowercase() == normalizedMerchant }
        if (exactMapping != null) {
            return CategoryMatchResult(
                    categoryName = exactMapping.categoryName,
                    confidence = 0.95f,
                    matchedKeyword = "Learned: ${exactMapping.merchantName}"
            )
        }

        // 2. Check for fuzzy match in learned mappings
        val fuzzyMapping =
                merchantMappings.maxByOrNull {
                    calculateSimilarity(normalizedMerchant, it.merchantName.lowercase())
                }
        if (fuzzyMapping != null) {
            val similarity =
                    calculateSimilarity(normalizedMerchant, fuzzyMapping.merchantName.lowercase())
            if (similarity >= 0.8f) {
                return CategoryMatchResult(
                        categoryName = fuzzyMapping.categoryName,
                        confidence = similarity * 0.9f,
                        matchedKeyword = "Similar to: ${fuzzyMapping.merchantName}"
                )
            }
        }

        // 3. Check category keywords (exact contain match)
        for (category in categories) {
            val keywords = category.keywords.split(",").map { it.trim().lowercase() }
            for (keyword in keywords) {
                if (keyword.isNotEmpty() && normalizedMerchant.contains(keyword)) {
                    return CategoryMatchResult(
                            categoryName = category.name,
                            confidence = 0.85f,
                            matchedKeyword = keyword
                    )
                }
            }
        }

        // 4. Check if any keyword fuzzy matches
        var bestFuzzyMatch: CategoryMatchResult? = null
        var bestFuzzySimilarity = 0f

        for (category in categories) {
            val keywords = category.keywords.split(",").map { it.trim().lowercase() }
            for (keyword in keywords) {
                if (keyword.isEmpty()) continue

                val similarity = calculateSimilarity(normalizedMerchant, keyword)
                if (similarity > bestFuzzySimilarity && similarity >= 0.7f) {
                    bestFuzzySimilarity = similarity
                    bestFuzzyMatch =
                            CategoryMatchResult(
                                    categoryName = category.name,
                                    confidence = similarity * 0.7f,
                                    matchedKeyword = keyword
                            )
                }
            }
        }

        if (bestFuzzyMatch != null) {
            return bestFuzzyMatch
        }

        // 5. Default to "Others" with low confidence
        return CategoryMatchResult(
                categoryName = "Others",
                confidence = 0.3f,
                matchedKeyword = null
        )
    }

    /**
     * Calculates Jaro-Winkler similarity between two strings. Returns a value between 0.0 (no
     * similarity) and 1.0 (exact match).
     */
    private fun calculateSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f

        // Use simpler Levenshtein-based similarity for performance
        val maxLen = maxOf(s1.length, s2.length)
        val distance = levenshteinDistance(s1, s2)
        return (1.0f - distance.toFloat() / maxLen)
    }

    /** Calculates Levenshtein (edit) distance between two strings. */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost)
            }
        }

        return dp[len1][len2]
    }
}
