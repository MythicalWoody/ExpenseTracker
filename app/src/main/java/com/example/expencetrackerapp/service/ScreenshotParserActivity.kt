package com.example.expencetrackerapp.service

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expencetrackerapp.data.database.ExpenseDatabase
import com.example.expencetrackerapp.data.database.entities.Expense
import com.example.expencetrackerapp.data.database.entities.TransactionType
import com.example.expencetrackerapp.ocr.ScreenshotTextExtractor
import com.example.expencetrackerapp.ui.theme.ExpenceTrackerAppTheme
import com.example.expencetrackerapp.ui.theme.Primary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Activity that displays captured screenshot and auto-extracts
 * payment details using OCR (ML Kit Text Recognition).
 */
class ScreenshotParserActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val screenshotPath = intent.getStringExtra("screenshot_path")
        val appPackage = intent.getStringExtra("app_package")
        
        val bitmap = screenshotPath?.let { 
            try {
                BitmapFactory.decodeFile(it) 
            } catch (e: Exception) {
                null
            }
        }
        
        setContent {
            ExpenceTrackerAppTheme {
                val scope = rememberCoroutineScope()
                var isLoading by remember { mutableStateOf(true) }
                var extractedAmount by remember { mutableStateOf("") }
                var extractedMerchant by remember { mutableStateOf("") }
                var ocrRawText by remember { mutableStateOf("") }
                
                // Duplicate detection state
                var showDuplicateDialog by remember { mutableStateOf(false) }
                var duplicateExpense by remember { mutableStateOf<Expense?>(null) }
                var pendingSaveData by remember { mutableStateOf<Triple<Double, String, String>?>(null) }
                
                // Run OCR on launch
                LaunchedEffect(bitmap) {
                    if (bitmap != null) {
                        try {
                            // Pass the package name to use app-specific strategy
                            val result = ScreenshotTextExtractor.extractFromBitmap(bitmap, appPackage)
                            extractedAmount = result.amount?.toString() ?: ""
                            extractedMerchant = result.merchant ?: getAppName(appPackage)
                            ocrRawText = result.rawText
                            
                            if (result.amount != null) {
                                Toast.makeText(
                                    this@ScreenshotParserActivity,
                                    "Found: ₹${result.amount}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            extractedMerchant = getAppName(appPackage)
                        }
                        isLoading = false
                    } else {
                        isLoading = false
                    }
                }
                
                if (showDuplicateDialog && duplicateExpense != null) {
                    AlertDialog(
                        onDismissRequest = { showDuplicateDialog = false },
                        title = { Text("Potential Duplicate Found") },
                        text = { 
                            Text("A transaction of ₹${duplicateExpense?.amount} was already recorded on ${java.text.SimpleDateFormat("dd MMM, HH:mm").format(duplicateExpense?.date ?: 0L)}.\n\nDo you still want to save this expense?") 
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDuplicateDialog = false
                                    pendingSaveData?.let { (amt, mer, cat) ->
                                        saveExpense(amt, mer, cat, appPackage)
                                        screenshotPath?.let { File(it).delete() }
                                        finish()
                                    }
                                }
                            ) {
                                Text("Save Anyway")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDuplicateDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                
                ScreenshotParserScreen(
                    screenshotPath = screenshotPath,
                    initialAmount = extractedAmount,
                    initialMerchant = extractedMerchant,
                    appSource = getAppName(appPackage),
                    isLoading = isLoading,
                    ocrRawText = ocrRawText,
                    onSave = { amount, merchant, category ->
                        scope.launch {
                            val duplicate = checkForDuplicate(amount)
                            if (duplicate != null) {
                                duplicateExpense = duplicate
                                pendingSaveData = Triple(amount, merchant, category)
                                showDuplicateDialog = true
                            } else {
                                saveExpense(amount, merchant, category, appPackage)
                                screenshotPath?.let { File(it).delete() }
                                finish()
                            }
                        }
                    },
                    onCancel = {
                        screenshotPath?.let { File(it).delete() }
                        finish()
                    }
                )
            }
        }
    }

    private suspend fun checkForDuplicate(amount: Double): Expense? {
        val database = ExpenseDatabase.getDatabase(applicationContext)
        val currentTime = System.currentTimeMillis()
        val startTime = currentTime - 24 * 60 * 60 * 1000 // 24 hours ago
        val endTime = currentTime + 24 * 60 * 60 * 1000 // 24 hours ahead (just in case)
        
        return database.expenseDao().checkPotentialDuplicates(amount, startTime, endTime)
    }
    
    private fun getAppName(packageName: String?): String {
        return when (packageName) {
            "com.samsung.android.spay", "com.samsung.android.samsungpay" -> "Samsung Wallet"
            "com.phonepe.app" -> "PhonePe"
            "com.google.android.apps.nbu.paisa.user" -> "Google Pay"
            "com.dreamplug.androidapp" -> "CRED"
            else -> "UPI Payment"
        }
    }
    
    private fun saveExpense(amount: Double, merchant: String, category: String, appPackage: String?) {
        val database = ExpenseDatabase.getDatabase(applicationContext)
        
        CoroutineScope(Dispatchers.IO).launch {
            val expense = Expense(
                amount = amount,
                merchant = merchant,
                category = category,
                date = System.currentTimeMillis(),
                smsBody = "[Screenshot] Captured from ${getAppName(appPackage)}",
                accountNumber = getAppName(appPackage),
                isAutoCategorized = false,
                transactionType = TransactionType.DEBIT
            )
            database.expenseDao().insertExpense(expense)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotParserScreen(
    screenshotPath: String?,
    initialAmount: String,
    initialMerchant: String,
    appSource: String,
    isLoading: Boolean,
    ocrRawText: String,
    onSave: (amount: Double, merchant: String, category: String) -> Unit,
    onCancel: () -> Unit
) {
    var amount by remember { mutableStateOf(initialAmount) }
    var merchant by remember { mutableStateOf(initialMerchant) }
    var selectedCategory by remember { mutableStateOf("Others") }
    var showRawText by remember { mutableStateOf(false) }
    
    // Update when OCR completes
    LaunchedEffect(initialAmount, initialMerchant) {
        if (initialAmount.isNotEmpty()) amount = initialAmount
        if (initialMerchant.isNotEmpty()) merchant = initialMerchant
    }
    
    val categories = listOf(
        "Food & Dining", "Shopping", "Transport", "Fashion",
        "Entertainment", "Bills & Utilities", "Health", "Others"
    )
    
    val bitmap = remember {
        screenshotPath?.let {
            try {
                BitmapFactory.decodeFile(it)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    val isValid = amount.toDoubleOrNull() != null && 
                  amount.toDouble() > 0 &&
                  merchant.isNotBlank()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📸 Add from Screenshot",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Source: $appSource",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Screenshot preview
            bitmap?.let { bmp ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Screenshot",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Loading overlay
                        if (isLoading) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(color = Primary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "🔍 Reading text...",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Status message
            if (!isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (amount.isNotEmpty()) 
                            Primary.copy(alpha = 0.1f) 
                        else 
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    onClick = { showRawText = !showRawText }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (amount.isNotEmpty()) 
                                "✅ Found amount: ₹$amount" 
                            else 
                                "⚠️ Could not auto-detect amount. Please enter manually.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (amount.isNotEmpty()) Primary else MaterialTheme.colorScheme.error
                        )
                        
                        if (showRawText && ocrRawText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "OCR Text (tap to hide):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = ocrRawText.take(300) + if (ocrRawText.length > 300) "..." else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { 
                    if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                        amount = it
                    }
                },
                label = { Text("Amount") },
                prefix = { Text("₹ ") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                enabled = !isLoading
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Merchant
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Paid To") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Category chips
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.chunked(4).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { 
                                    Text(
                                        category.split(" ").first(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                enabled = !isLoading
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Save button
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    if (amountValue > 0 && merchant.isNotBlank()) {
                        onSave(amountValue, merchant, selectedCategory)
                    }
                },
                enabled = isValid && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Expense", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
