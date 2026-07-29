@file:OptIn(ExperimentalCompatApi::class)

package com.trendyol.design.ui.component.compat

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.trendyol.design.compat.annotation.ExperimentalCompatApi
import com.trendyol.design.compat.inputfield.KPCompatBasicTextField
import com.trendyol.design.ui.component.common.Component
import com.trendyol.design.ui.component.common.Group
import com.trendyol.design.ui.theme.TrendyolTheme

private const val SAMPLE_WIDTH = 320
private const val MAX_PRICE_LENGTH = 8
private const val MAX_WEIGHT_LENGTH = 3
private const val MAX_WEIGHT_KG = 500
private const val MAX_PHONE_LENGTH = 10
private const val MAX_REASON_LENGTH = 250
private val DECIMAL_AMOUNT_REGEX = Regex("""\d{0,7}([.,]\d{0,2})?""")

@Preview(showBackground = true)
@ShowkaseComposable(
    group = Group.COMPAT,
    name = Component.COMPAT_BASIC_TEXT_FIELD,
    styleName = "3.Search"
)
@Composable
internal fun Compat_BasicTextField_3_Search() = TrendyolTheme {
    var query by remember { mutableStateOf("") }
    var submittedQuery by remember { mutableStateOf<String?>(null) }

    SampleScaffold(title = "Search") {
        KPCompatBasicTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(
                onSearch = { submittedQuery = query },
            ),
            decorationBox = { innerTextField ->
                SampleDecoration(
                    text = query,
                    placeholder = "Ürün, kategori veya marka ara",
                    trailingText = query.takeIf { it.isNotEmpty() }?.let { "Temizle" },
                    onTrailingClick = { query = "" },
                    innerTextField = innerTextField,
                )
            },
        )
        submittedQuery?.let {
            Text(
                text = "Aranan: $it",
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@ShowkaseComposable(
    group = Group.COMPAT,
    name = Component.COMPAT_BASIC_TEXT_FIELD,
    styleName = "4.Price"
)
@Composable
internal fun Compat_BasicTextField_4_Price() = TrendyolTheme {
    var price by remember { mutableStateOf("") }
    val isError = price.any { !it.isDigit() } || price.length > MAX_PRICE_LENGTH

    SampleScaffold(title = "Integer price") {
        KPCompatBasicTextField(
            value = price,
            onValueChange = { price = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            decorationBox = { innerTextField ->
                SampleDecoration(
                    text = price,
                    placeholder = "Fiyat girin",
                    trailingText = "TL",
                    supportingText = if (isError) {
                        "Yalnızca $MAX_PRICE_LENGTH haneye kadar rakam girin"
                    } else {
                        "En fazla $MAX_PRICE_LENGTH hane"
                    },
                    isError = isError,
                    innerTextField = innerTextField,
                )
            },
        )
    }
}

@Preview(showBackground = true)
@ShowkaseComposable(
    group = Group.COMPAT,
    name = Component.COMPAT_BASIC_TEXT_FIELD,
    styleName = "5.DecimalAmount"
)
@Composable
internal fun Compat_BasicTextField_5_DecimalAmount() = TrendyolTheme {
    var amount by remember { mutableStateOf(TextFieldValue("")) }
    val normalizedAmount = amount.text.replace(',', '.')
    val amountError = when {
        amount.text.isEmpty() -> null
        !DECIMAL_AMOUNT_REGEX.matches(amount.text) -> "En fazla 7 tam ve 2 ondalık basamak girin"
        normalizedAmount.toDoubleOrNull()?.let { it > 0.0 } != true -> "Tutar 0'dan büyük olmalı"
        else -> null
    }

    SampleScaffold(title = "Decimal amount") {
        KPCompatBasicTextField(
            value = amount,
            onValueChange = { amount = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            decorationBox = { innerTextField ->
                SampleDecoration(
                    text = amount.text,
                    placeholder = "0,00",
                    trailingText = "TL",
                    supportingText = amountError ?: "En fazla 2 ondalık basamak",
                    isError = amountError != null,
                    innerTextField = innerTextField,
                )
            },
        )
    }
}

@Preview(showBackground = true)
@ShowkaseComposable(
    group = Group.COMPAT,
    name = Component.COMPAT_BASIC_TEXT_FIELD,
    styleName = "6.Weight"
)
@Composable
internal fun Compat_BasicTextField_6_Weight() = TrendyolTheme {
    var weight by remember { mutableStateOf("") }
    val parsedWeight = weight.takeIf { value -> value.all(Char::isDigit) }?.toIntOrNull()
    val isError = weight.isNotEmpty() && (
        weight.length > MAX_WEIGHT_LENGTH ||
            parsedWeight == null ||
            parsedWeight !in 1..MAX_WEIGHT_KG
        )

    SampleScaffold(title = "Weight") {
        KPCompatBasicTextField(
            value = weight,
            onValueChange = { weight = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            decorationBox = { innerTextField ->
                SampleDecoration(
                    text = weight,
                    placeholder = "Kilo girin",
                    trailingText = "kg",
                    supportingText = if (isError) "1-$MAX_WEIGHT_KG kg arasında girin" else null,
                    isError = isError,
                    innerTextField = innerTextField,
                )
            },
        )
    }
}

@Preview(showBackground = true)
@ShowkaseComposable(
    group = Group.COMPAT,
    name = Component.COMPAT_BASIC_TEXT_FIELD,
    styleName = "7.Phone"
)
@Composable
internal fun Compat_BasicTextField_7_Phone() = TrendyolTheme {
    var phone by remember { mutableStateOf(TextFieldValue("")) }
    val isError = phone.text.any { !it.isDigit() } || phone.text.length > MAX_PHONE_LENGTH

    SampleScaffold(title = "Phone") {
        KPCompatBasicTextField(
            value = phone,
            onValueChange = { phone = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done,
            ),
            decorationBox = { innerTextField ->
                SampleDecoration(
                    text = phone.text,
                    placeholder = "5XX XXX XX XX",
                    leadingText = "+90",
                    supportingText = if (isError) {
                        "Yalnızca $MAX_PHONE_LENGTH haneli telefon numarası girin"
                    } else {
                        "${phone.text.length}/$MAX_PHONE_LENGTH"
                    },
                    isError = isError,
                    innerTextField = innerTextField,
                )
            },
        )
    }
}

@Preview(showBackground = true)
@ShowkaseComposable(
    group = Group.COMPAT,
    name = Component.COMPAT_BASIC_TEXT_FIELD,
    styleName = "8.MultilineReason"
)
@Composable
internal fun Compat_BasicTextField_8_MultilineReason() = TrendyolTheme {
    var reason by remember { mutableStateOf("") }
    val isError = reason.length > MAX_REASON_LENGTH

    SampleScaffold(title = "Multiline reason") {
        KPCompatBasicTextField(
            value = reason,
            onValueChange = { reason = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp),
            singleLine = false,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default,
            ),
            decorationBox = { innerTextField ->
                SampleDecoration(
                    text = reason,
                    placeholder = "İade nedeninizi açıklayın",
                    supportingText = "${reason.length}/$MAX_REASON_LENGTH",
                    minHeight = 112.dp,
                    isError = isError,
                    innerTextField = innerTextField,
                )
            },
        )
    }
}

@Composable
private fun SampleScaffold(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(SAMPLE_WIDTH.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1,
        )
        content()
    }
}

@Composable
private fun SampleDecoration(
    text: String,
    placeholder: String,
    innerTextField: @Composable () -> Unit,
    leadingText: String? = null,
    trailingText: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    minHeight: Dp = 48.dp,
    onTrailingClick: (() -> Unit)? = null,
) {
    val borderColor = if (isError) {
        MaterialTheme.colors.error
    } else {
        MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
    }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingText?.let {
                Text(text = it, style = MaterialTheme.typography.body1)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.body1,
                    )
                }
                innerTextField()
            }
            trailingText?.let {
                Spacer(modifier = Modifier.width(8.dp))
                if (onTrailingClick == null) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .clickable(
                                role = Role.Button,
                                onClick = onTrailingClick,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.primary,
                        )
                    }
                }
            }
        }
        supportingText?.let {
            Text(
                text = it,
                color = if (isError) {
                    MaterialTheme.colors.error
                } else {
                    MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                },
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
            )
        }
    }
}
