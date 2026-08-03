@file:OptIn(ExperimentalCompatApi::class)

package compat

import android.app.Activity
import android.content.Context
import android.text.InputType
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.trendyol.design.compat.annotation.ExperimentalCompatApi
import com.trendyol.design.compat.inputfield.KPCompatBasicTextField
import com.trendyol.design.ui.theme.TrendyolTheme
import core.InteractionTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@InteractionTest
class KPCompatBasicTextFieldInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun stringOverload_typing_updatesState() {
        var latest = ""
        composeTestRule.setContent {
            TrendyolTheme {
                var text by remember { mutableStateOf("") }
                KPCompatBasicTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        latest = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("compat_tf"),
                    singleLine = true,
                )
            }
        }

        composeTestRule.onNodeWithTag("compat_tf").performTextInput("abc")
        composeTestRule.waitForIdle()
        assertEquals("abc", latest)
        composeTestRule.onNodeWithTag("compat_tf").assertTextEquals("abc")
    }

    @Test
    fun textFieldValue_programmaticSelection_isApplied() {
        val focusRequester = FocusRequester()
        composeTestRule.setContent {
            TrendyolTheme {
                var value by remember {
                    mutableStateOf(TextFieldValue("hello", TextRange(0, 5)))
                }
                KPCompatBasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("compat_tf"),
                    singleLine = true,
                )
            }
        }
        composeTestRule.runOnIdle { focusRequester.requestFocus() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("compat_tf").performTextReplacement("x")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("compat_tf").assertTextEquals("x")
    }

    @Test
    fun imeSearch_invokesKeyboardAction() {
        var searchClicked = false
        composeTestRule.setContent {
            TrendyolTheme {
                var text by remember { mutableStateOf("query") }
                KPCompatBasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("compat_tf"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { searchClicked = true }),
                )
            }
        }
        composeTestRule.onNodeWithTag("compat_tf").performImeAction()
        composeTestRule.waitForIdle()
        assertTrue(searchClicked)
    }

    @Test
    fun decimalKeyboard_usesNumberInputWithDecimalFlag() {
        lateinit var activity: Activity
        composeTestRule.setContent {
            activity = LocalView.current.context as Activity
            TrendyolTheme {
                KPCompatBasicTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        }

        composeTestRule.runOnIdle {
            val inputType = activity.findViewById<EditText>(com.trendyol.design.compat.R.id.kp_compat_basic_text_field).inputType
            assertEquals(
                InputType.TYPE_CLASS_NUMBER,
                inputType and InputType.TYPE_MASK_CLASS,
            )
            assertTrue(inputType and InputType.TYPE_NUMBER_FLAG_DECIMAL != 0)
        }
    }

    @Test
    fun platformFocusChanges_areMirroredToComposeFocus() {
        var isComposeFocused = false
        lateinit var activity: Activity

        composeTestRule.setContent {
            activity = LocalView.current.context as Activity
            TrendyolTheme {
                KPCompatBasicTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isComposeFocused = it.hasFocus }
                        .testTag("compat_tf"),
                    singleLine = true,
                )
            }
        }

        lateinit var competingEditText: EditText
        composeTestRule.runOnUiThread {
            val compatEditText = activity.findViewById<EditText>(com.trendyol.design.compat.R.id.kp_compat_basic_text_field)
            compatEditText.requestFocus()
        }
        composeTestRule.waitForIdle()
        assertTrue(isComposeFocused)

        composeTestRule.runOnUiThread {
            val decorView = activity.window.decorView as ViewGroup
            competingEditText = EditText(activity)
            decorView.addView(competingEditText)
            competingEditText.requestFocus()
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            val compatEditText = activity.findViewById<EditText>(com.trendyol.design.compat.R.id.kp_compat_basic_text_field)
            assertFalse(isComposeFocused)
            assertFalse(compatEditText.hasFocus())
        }
        composeTestRule.runOnUiThread {
            (competingEditText.parent as ViewGroup).removeView(competingEditText)
        }
    }

    @Test
    fun focusRequester_focusesPlatformEditText() {
        val focusRequester = FocusRequester()
        lateinit var activity: Activity

        composeTestRule.setContent {
            activity = LocalView.current.context as Activity
            TrendyolTheme {
                Column {
                    KPCompatBasicTextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag("compat_tf"),
                        singleLine = true,
                    )
                }
            }
        }

        composeTestRule.runOnIdle { focusRequester.requestFocus() }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            val compatEditText = activity.findViewById<EditText>(com.trendyol.design.compat.R.id.kp_compat_basic_text_field)
            assertTrue(compatEditText.hasFocus())
        }
    }

    @Test
    fun nonEmptyField_clickFocusesEditorAndShowsIme() {
        lateinit var activity: Activity
        lateinit var compatEditText: EditText

        composeTestRule.setContent {
            activity = LocalView.current.context as Activity
            TrendyolTheme {
                KPCompatBasicTextField(
                    value = "query",
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("compat_tf"),
                    singleLine = true,
                )
            }
        }

        composeTestRule.runOnUiThread {
            compatEditText = activity.findViewById<EditText>(com.trendyol.design.compat.R.id.kp_compat_basic_text_field)
            compatEditText.clearFocus()
            val inputMethodManager =
                activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(
                activity.window.decorView.windowToken,
                0,
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread {
            compatEditText.performClick()
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            compatEditText.hasFocus() && isImeVisible(activity)
        }
    }

    private fun isImeVisible(activity: Activity): Boolean {
        return ViewCompat.getRootWindowInsets(activity.window.decorView)
            ?.isVisible(WindowInsetsCompat.Type.ime()) == true
    }
}
