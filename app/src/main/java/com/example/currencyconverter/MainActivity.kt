package com.example.currencyconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.currencyconverter.ui.theme.CurrencyConverterTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CurrencyConverterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CurrencyConverterScreen()
                }
            }
        }
    }
}

// Data class for currency information
data class Currency(
    val code: String,
    val rate: Double,
    val symbol: String = code
)

// Currency data repository
object CurrencyRepository {
    private val currencies = listOf(
        Currency("USD", 16789.0, "$"),
        Currency("EUR", 19071.0, "€"),
        Currency("JPY", 117.1, "¥"),
        Currency("GBP", 22114.70, "£"),
        Currency("AUD", 10640.0, "A$"),
        Currency("CAD", 12080.0, "C$"),
        Currency("SGD", 12750.0, "S$"),
        Currency("MYR", 3812.0, "RM"),
        Currency("THB", 500.3, "฿"),
        Currency("CNY", 2294.0, "¥")
    )

    fun getAllCurrencies(): List<Currency> = currencies

    fun getCurrencyByCode(code: String): Currency? =
        currencies.find { it.code == code }
}

// UI State data class
data class CurrencyConverterState(
    val idrAmount: String = "",
    val selectedCurrency: Currency = CurrencyRepository.getAllCurrencies().first(),
    val conversionResult: String? = null,
    val isDropdownExpanded: Boolean = false,
    val errorMessage: String? = null
)

// Conversion logic
object CurrencyConverter {
    fun convertFromIDR(idrAmount: Double, targetCurrency: Currency): Double {
        return idrAmount / targetCurrency.rate
    }

    fun formatAmount(amount: Double, currency: Currency): String {
        return String.format(Locale.US, "%.2f %s", amount, currency.code)
    }

    fun validateInput(input: String): ValidationResult {
        return when {
            input.isBlank() -> ValidationResult.Error("Masukkan jumlah IDR")
            input.toDoubleOrNull() == null -> ValidationResult.Error("Format angka tidak valid")
            input.toDouble() <= 0 -> ValidationResult.Error("Jumlah harus lebih dari 0")
            else -> ValidationResult.Success
        }
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

@Composable
fun CurrencyConverterScreen() {
    var uiState by remember { mutableStateOf(CurrencyConverterState()) }
    val currencies = remember { CurrencyRepository.getAllCurrencies() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppHeader()

        Spacer(modifier = Modifier.height(24.dp))

        IDRInputField(
            value = uiState.idrAmount,
            onValueChange = { newValue ->
                if (newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                    uiState = uiState.copy(
                        idrAmount = newValue,
                        conversionResult = null,
                        errorMessage = null
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        CurrencySelector(
            selectedCurrency = uiState.selectedCurrency,
            currencies = currencies,
            isExpanded = uiState.isDropdownExpanded,
            onExpandedChange = { expanded ->
                uiState = uiState.copy(isDropdownExpanded = expanded)
            },
            onCurrencySelected = { currency ->
                uiState = uiState.copy(
                    selectedCurrency = currency,
                    isDropdownExpanded = false,
                    conversionResult = null
                )
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        ConvertButton(
            onClick = {
                val validationResult = CurrencyConverter.validateInput(uiState.idrAmount)
                when (validationResult) {
                    is ValidationResult.Success -> {
                        val idrAmount = uiState.idrAmount.toDouble()
                        val convertedAmount = CurrencyConverter.convertFromIDR(
                            idrAmount,
                            uiState.selectedCurrency
                        )
                        val formattedResult = CurrencyConverter.formatAmount(
                            convertedAmount,
                            uiState.selectedCurrency
                        )
                        uiState = uiState.copy(
                            conversionResult = formattedResult,
                            errorMessage = null
                        )
                    }
                    is ValidationResult.Error -> {
                        uiState = uiState.copy(
                            errorMessage = validationResult.message,
                            conversionResult = null
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        ResultDisplay(
            result = uiState.conversionResult,
            error = uiState.errorMessage
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun AppHeader() {
    Text(
        text = "Konverter Mata Uang (IDR)",
        fontSize = 24.sp,
        style = MaterialTheme.typography.headlineMedium
    )
}

@Composable
private fun IDRInputField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Jumlah IDR") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CurrencySelector(
    selectedCurrency: Currency,
    currencies: List<Currency>,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCurrencySelected: (Currency) -> Unit
) {
    Text(
        text = "Konversi ke:",
        modifier = Modifier.fillMaxWidth()
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedCurrency.code,
            onValueChange = {},
            label = { Text("Mata Uang Tujuan") },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { onExpandedChange(true) }) {
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = "Pilih Mata Uang"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth()
        ) {
            currencies.forEach { currency ->
                DropdownMenuItem(
                    onClick = {
                        onCurrencySelected(currency)
                    },
                    text = {
                        Text("${currency.code} (${currency.symbol})")
                    }
                )
            }
        }
    }
}

@Composable
private fun ConvertButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Konversi")
    }
}

@Composable
private fun ResultDisplay(
    result: String?,
    error: String?
) {
    when {
        error != null -> {
            Text(
                text = "Error: $error",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        result != null -> {
            Text(
                text = "Hasil: $result",
                fontSize = 20.sp,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CurrencyConverterPreview() {
    CurrencyConverterTheme {
        CurrencyConverterScreen()
    }
}