package com.example.expencetrackerapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores learned mappings between merchant names and categories. When a user manually categorizes
 * an expense, we remember the merchant→category association.
 */
@Entity(tableName = "merchant_mappings")
data class MerchantMapping(
        @PrimaryKey val merchantName: String,

        /** The category ID this merchant is associated with */
        val categoryName: String,

        /** Number of times this mapping was used (for confidence scoring) */
        val usageCount: Int = 1,

        /** Last time this mapping was used/updated */
        val lastUsed: Long = System.currentTimeMillis()
)
