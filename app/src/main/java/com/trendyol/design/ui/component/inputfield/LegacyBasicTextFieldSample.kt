@file:OptIn(ExperimentalLegacyApi::class)

package com.trendyol.design.ui.component.inputfield

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.trendyol.design.legacy.annotation.ExperimentalLegacyApi
import com.trendyol.design.legacy.inputfield.KPLegacyBasicTextField
import com.trendyol.design.ui.component.common.Component
import com.trendyol.design.ui.component.common.Group
import com.trendyol.design.ui.theme.TrendyolTheme
import com.trendyol.theme.KPDesign

private const val MIN_WIDTH = 280

/**
 * Manual crash-repro sample for [KPLegacyBasicTextField].
 *
 * Try: type quickly, then double-tap to select-all — platform EditText selection
 * should not crash (unlike Compose Foundation BasicTextField in some cases).
 */
@Preview(showBackground = true)
@ShowkaseComposable(
    group = Group.INPUT_FIELD,
    name = Component.INPUT_FIELD_LEGACY_BASIC,
    styleName = "1.Interactive"
)
@Composable
internal fun Input_Field_LegacyBasicTextField_1_Interactive() = TrendyolTheme {
    Column(
        modifier = Modifier
            .width(MIN_WIDTH.dp)
            .padding(16.dp),
    ) {
        var value by remember { mutableStateOf(TextFieldValue("")) }
        Text(
            text = "Type fast, then double-tap select-all",
            style = KPDesign.typography.body2,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        KPLegacyBasicTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Text(
            text = "selection=${value.selection}",
            style = KPDesign.typography.body2,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Preview(showBackground = true)
@ShowkaseComposable(
    group = Group.INPUT_FIELD,
    name = Component.INPUT_FIELD_LEGACY_BASIC,
    styleName = "2.Prefilled"
)
@Composable
internal fun Input_Field_LegacyBasicTextField_2_Prefilled() = TrendyolTheme {
    Column(
        modifier = Modifier
            .width(MIN_WIDTH.dp)
            .padding(16.dp),
    ) {
        var value by remember {
            mutableStateOf(TextFieldValue("Prefilled text for selection"))
        }
        KPLegacyBasicTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}

@Preview(showBackground = true)
@ShowkaseComposable(
    group = Group.INPUT_FIELD,
    name = Component.INPUT_FIELD_LEGACY_BASIC,
    styleName = "2b.PasswordMask"
)
@Composable
internal fun Input_Field_LegacyBasicTextField_2b_PasswordMask() = TrendyolTheme {
    Column(
        modifier = Modifier
            .width(MIN_WIDTH.dp)
            .padding(16.dp),
    ) {
        var value by remember { mutableStateOf("") }
        val passwordVisualTransformation = remember { PasswordVisualTransformation() }
        Text(
            text = "PasswordVisualTransformation",
            style = KPDesign.typography.body2,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        KPLegacyBasicTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = passwordVisualTransformation,
        )
    }
}
