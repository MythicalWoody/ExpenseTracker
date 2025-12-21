package com.example.expencetrackerapp.data.database.dao

import androidx.room.*
import com.example.expencetrackerapp.data.database.entities.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>
    
    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?
    
    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesBetweenDates(startDate: Long, endDate: Long): Flow<List<Expense>>
    
    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY date DESC")
    fun getExpensesByCategory(category: String): Flow<List<Expense>>
    
    @Query("SELECT SUM(amount) FROM expenses WHERE transactionType = 'DEBIT' AND date BETWEEN :startDate AND :endDate")
    fun getTotalSpentBetweenDates(startDate: Long, endDate: Long): Flow<Double?>
    
    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE transactionType = 'DEBIT' AND date BETWEEN :startDate AND :endDate GROUP BY category")
    fun getSpendingByCategory(startDate: Long, endDate: Long): Flow<List<CategorySpending>>
    
    @Query("SELECT * FROM expenses WHERE merchant LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchExpenses(query: String): Flow<List<Expense>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long
    
    @Update
    suspend fun updateExpense(expense: Expense)
    
    @Delete
    suspend fun deleteExpense(expense: Expense)
    
    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)
    
    @Query("SELECT COUNT(*) FROM expenses")
    fun getExpenseCount(): Flow<Int>
    
    @Query("SELECT * FROM expenses ORDER BY date DESC LIMIT :limit")
    fun getRecentExpenses(limit: Int): Flow<List<Expense>>
    
    @Query("SELECT * FROM expenses ORDER BY date DESC LIMIT :limit")
    fun getRecentExpensesSync(limit: Int): List<Expense>

    @Query("SELECT * FROM expenses WHERE amount = :amount AND date BETWEEN :startTime AND :endTime LIMIT 1")
    suspend fun checkPotentialDuplicates(amount: Double, startTime: Long, endTime: Long): Expense?
}

data class CategorySpending(
    val category: String,
    val total: Double
)
