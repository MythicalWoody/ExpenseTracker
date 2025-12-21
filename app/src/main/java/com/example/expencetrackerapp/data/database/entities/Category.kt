package com.example.expencetrackerapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an expense category with keywords for auto-matching.
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Display name of the category */
    val name: String,
    
    /** Material icon name for the category */
    val icon: String,
    
    /** Color hex code for the category (e.g., "#FF6B6B") */
    val color: String,
    
    /** Comma-separated keywords for auto-matching merchants */
    val keywords: String,
    
    /** Whether this is a default category (can't be deleted) */
    val isDefault: Boolean = true
)
