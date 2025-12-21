package com.example.expencetrackerapp.data.repository

import com.example.expencetrackerapp.data.database.dao.CategoryDao
import com.example.expencetrackerapp.data.database.dao.CategorySpending
import com.example.expencetrackerapp.data.database.dao.ExpenseDao
import com.example.expencetrackerapp.data.database.dao.MerchantDao
import com.example.expencetrackerapp.data.database.entities.Category
import com.example.expencetrackerapp.data.database.entities.Expense
import com.example.expencetrackerapp.data.database.entities.MerchantMapping
import kotlinx.coroutines.flow.Flow

/**
 * Repository that acts as single source of truth for all expense data. Abstracts data access from
 * ViewModels.
 */
class ExpenseRepository(
        private val expenseDao: ExpenseDao,
        private val categoryDao: CategoryDao,
        private val merchantDao: MerchantDao
) {

        // ===== Expenses =====

        val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

        fun getExpensesBetweenDates(startDate: Long, endDate: Long): Flow<List<Expense>> =
                expenseDao.getExpensesBetweenDates(startDate, endDate)

        fun getExpensesByCategory(category: String): Flow<List<Expense>> =
                expenseDao.getExpensesByCategory(category)

        fun getTotalSpentBetweenDates(startDate: Long, endDate: Long): Flow<Double?> =
                expenseDao.getTotalSpentBetweenDates(startDate, endDate)

        fun getSpendingByCategory(startDate: Long, endDate: Long): Flow<List<CategorySpending>> =
                expenseDao.getSpendingByCategory(startDate, endDate)

        fun searchExpenses(query: String): Flow<List<Expense>> = expenseDao.searchExpenses(query)

        fun getRecentExpenses(limit: Int = 10): Flow<List<Expense>> =
                expenseDao.getRecentExpenses(limit)

        fun getExpenseCount(): Flow<Int> = expenseDao.getExpenseCount()

        suspend fun getExpenseById(id: Long): Expense? = expenseDao.getExpenseById(id)

        suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)

        suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

        suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

        suspend fun deleteExpenseById(id: Long) = expenseDao.deleteExpenseById(id)

        // ===== Categories =====

        val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

        suspend fun getCategoryById(id: Long): Category? = categoryDao.getCategoryById(id)

        suspend fun getCategoryByName(name: String): Category? = categoryDao.getCategoryByName(name)

        suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)

        suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)

        suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

        // ===== Merchant Mappings =====

        val allMerchantMappings: Flow<List<MerchantMapping>> = merchantDao.getAllMappings()

        suspend fun getMerchantMapping(merchantName: String): MerchantMapping? =
                merchantDao.getMappingByMerchant(merchantName)

        suspend fun searchMerchantMappings(query: String): List<MerchantMapping> =
                merchantDao.searchMappings(query)

        suspend fun saveMerchantMapping(merchantName: String, categoryName: String) {
                val existing = merchantDao.getMappingByMerchant(merchantName)
                if (existing != null) {
                        merchantDao.insertMapping(
                                existing.copy(
                                        categoryName = categoryName,
                                        usageCount = existing.usageCount + 1,
                                        lastUsed = System.currentTimeMillis()
                                )
                        )
                } else {
                        merchantDao.insertMapping(
                                MerchantMapping(
                                        merchantName = merchantName,
                                        categoryName = categoryName
                                )
                        )
                }
        }

        suspend fun deleteAllMerchantMappings() = merchantDao.deleteAllMappings()
}
