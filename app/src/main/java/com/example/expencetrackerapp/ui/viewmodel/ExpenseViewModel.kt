package com.example.expencetrackerapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.expencetrackerapp.data.database.ExpenseDatabase
import com.example.expencetrackerapp.data.database.dao.CategorySpending
import com.example.expencetrackerapp.data.database.entities.Category
import com.example.expencetrackerapp.data.database.entities.Expense
import com.example.expencetrackerapp.data.database.entities.TransactionType
import com.example.expencetrackerapp.data.repository.ExpenseRepository
import com.example.expencetrackerapp.domain.categorization.CategoryMatcher
import com.example.expencetrackerapp.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Main ViewModel for the expense tracker app. Holds UI state and handles business logic. */
class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ExpenseDatabase.getDatabase(application)
    private val repository =
            ExpenseRepository(database.expenseDao(), database.categoryDao(), database.merchantDao())
    private val categoryMatcher = CategoryMatcher()

    // ===== State Flows =====

    val allExpenses: StateFlow<List<Expense>> =
            repository.allExpenses.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    emptyList()
            )

    val allCategories: StateFlow<List<Category>> =
            repository.allCategories.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    emptyList()
            )

    val recentExpenses: StateFlow<List<Expense>> =
            repository
                    .getRecentExpenses(10)
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyTotal: StateFlow<Double> =
            repository
                    .getTotalSpentBetweenDates(
                            DateUtils.getStartOfMonth(),
                            DateUtils.getEndOfMonth()
                    )
                    .map { it ?: 0.0 }
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlySpendingByCategory: StateFlow<List<CategorySpending>> =
            repository
                    .getSpendingByCategory(DateUtils.getStartOfMonth(), DateUtils.getEndOfMonth())
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenseCount: StateFlow<Int> =
            repository
                    .getExpenseCount()
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ===== Category Selection in Stats =====

    private val _selectedCategoryName = MutableStateFlow<String?>(null)
    val selectedCategoryName = _selectedCategoryName.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val categoryTransactions: StateFlow<List<Expense>> =
            _selectedCategoryName
                    .flatMapLatest { category ->
                        if (category == null) {
                            flowOf(emptyList())
                        } else {
                            repository.getExpensesByCategory(category)
                        }
                    }
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(categoryName: String?) {
        if (_selectedCategoryName.value == categoryName) {
            _selectedCategoryName.value = null
        } else {
            _selectedCategoryName.value = categoryName
        }
    }

    // ===== Search =====

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Expense>> =
            _searchQuery
                    .debounce(300)
                    .flatMapLatest { query ->
                        if (query.isBlank()) {
                            flowOf(emptyList())
                        } else {
                            repository.searchExpenses(query)
                        }
                    }
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // ===== Expense Operations =====

    suspend fun getExpenseById(id: Long): Expense? {
        return repository.getExpenseById(id)
    }

    fun addExpense(
            amount: Double,
            merchant: String,
            category: String,
            date: Long = System.currentTimeMillis(),
            note: String? = null,
            transactionType: TransactionType = TransactionType.DEBIT
    ) {
        viewModelScope.launch {
            val expense =
                    Expense(
                            amount = amount,
                            merchant = merchant,
                            category = category,
                            date = date,
                            note = note,
                            transactionType = transactionType,
                            isAutoCategorized = false
                    )
            repository.insertExpense(expense)

            // Learn the mapping for future auto-categorization
            repository.saveMerchantMapping(merchant, category)
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)

            // Update merchant mapping if manually categorized
            if (!expense.isAutoCategorized) {
                repository.saveMerchantMapping(expense.merchant, expense.category)
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    fun deleteExpenseById(id: Long) {
        viewModelScope.launch { repository.deleteExpenseById(id) }
    }

    // ===== Category Operations =====

    fun addCategory(name: String, icon: String, color: String, keywords: String) {
        viewModelScope.launch {
            val category =
                    Category(
                            name = name,
                            icon = icon,
                            color = color,
                            keywords = keywords,
                            isDefault = false
                    )
            repository.insertCategory(category)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch { repository.updateCategory(category) }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            if (!category.isDefault) {
                repository.deleteCategory(category)
            }
        }
    }

    // ===== Smart Categorization =====

    suspend fun suggestCategory(merchantName: String): String {
        val categories = allCategories.value
        val mappings = repository.allMerchantMappings.first()

        val result = categoryMatcher.matchCategory(merchantName, categories, mappings)
        return result.categoryName
    }

    // ===== SMS Import =====

    private val _smsImportState = MutableStateFlow<SmsImportState>(SmsImportState.Idle)
    val smsImportState = _smsImportState.asStateFlow()

    sealed class SmsImportState {
        object Idle : SmsImportState()
        object Importing : SmsImportState()
        data class Success(
                val totalSmsRead: Int,
                val bankSmsFound: Int,
                val transactionsImported: Int,
                val duplicatesSkipped: Int
        ) : SmsImportState()
        data class Error(val message: String) : SmsImportState()
    }

    fun importHistoricalSms(daysBack: Int = 365) {
        viewModelScope.launch {
            _smsImportState.value = SmsImportState.Importing
            try {
                val importer = com.example.expencetrackerapp.data.sms.SmsImporter(getApplication())
                val result = importer.importHistoricalSms(daysBack)
                _smsImportState.value =
                        SmsImportState.Success(
                                totalSmsRead = result.totalSmsRead,
                                bankSmsFound = result.bankSmsFound,
                                transactionsImported = result.transactionsImported,
                                duplicatesSkipped = result.duplicatesSkipped
                        )
            } catch (e: Exception) {
                _smsImportState.value = SmsImportState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetImportState() {
        _smsImportState.value = SmsImportState.Idle
    }
}
