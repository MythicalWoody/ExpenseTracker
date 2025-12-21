package com.example.expencetrackerapp.data.database.dao

import androidx.room.*
import com.example.expencetrackerapp.data.database.entities.MerchantMapping
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantDao {

    @Query("SELECT * FROM merchant_mappings ORDER BY usageCount DESC")
    fun getAllMappings(): Flow<List<MerchantMapping>>

    @Query("SELECT * FROM merchant_mappings WHERE merchantName = :merchantName")
    suspend fun getMappingByMerchant(merchantName: String): MerchantMapping?

    @Query("SELECT * FROM merchant_mappings WHERE merchantName LIKE '%' || :query || '%'")
    suspend fun searchMappings(query: String): List<MerchantMapping>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: MerchantMapping)

    @Query(
            "UPDATE merchant_mappings SET usageCount = usageCount + 1, lastUsed = :timestamp WHERE merchantName = :merchantName"
    )
    suspend fun incrementUsageWithTimestamp(merchantName: String, timestamp: Long)

    @Query(
            "UPDATE merchant_mappings SET usageCount = usageCount + 1, lastUsed = :timestamp WHERE merchantName = :merchantName AND categoryName = :categoryName"
    )
    suspend fun incrementUsage(
            merchantName: String,
            categoryName: String,
            timestamp: Long = System.currentTimeMillis()
    )

    @Delete suspend fun deleteMapping(mapping: MerchantMapping)

    @Query("DELETE FROM merchant_mappings") suspend fun deleteAllMappings()

    @Query("SELECT * FROM merchant_mappings ORDER BY usageCount DESC")
    fun getAllMappingsSync(): List<MerchantMapping>
}
