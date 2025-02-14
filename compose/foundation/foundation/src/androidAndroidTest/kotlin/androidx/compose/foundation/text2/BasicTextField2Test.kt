/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation.text2

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardHelper
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties.TextSelectionRange
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class BasicTextField2Test {
    @get:Rule
    val rule = createComposeRule()

    private val Tag = "BasicTextField2"

    @Test
    fun textField_rendersEmptyContent() {
        var textLayoutResult: TextLayoutResult? = null
        rule.setContent {
            val state = remember { TextFieldState() }
            BasicTextField2(
                state = state,
                modifier = Modifier.fillMaxSize(),
                onTextLayout = { textLayoutResult = it }
            )
        }

        rule.runOnIdle {
            assertThat(textLayoutResult).isNotNull()
            assertThat(textLayoutResult?.layoutInput?.text).isEqualTo(AnnotatedString(""))
        }
    }

    @Test
    fun textField_contentChange_updatesState() {
        val state = TextFieldState(TextFieldValue("Hello ", TextRange(Int.MAX_VALUE)))
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performTextInput("World!")

        rule.runOnIdle {
            assertThat(state.value.text).isEqualTo("Hello World!")
        }

        rule.onNodeWithTag(Tag).assertTextEquals("Hello World!")
        val selection = rule.onNodeWithTag(Tag).fetchSemanticsNode()
            .config.getOrNull(TextSelectionRange)
        assertThat(selection).isEqualTo(TextRange("Hello World!".length))
    }

    /**
     * This is a goal that we set for ourselves. Only updating the editing buffer should not cause
     * BasicTextField to recompose.
     */
    @Test
    fun textField_imeUpdatesDontCauseRecomposition() {
        val state = TextFieldState()
        var compositionCount = 0
        var textLayoutResultCount = 0
        rule.setContent {
            compositionCount++
            BasicTextField2(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag),
                onTextLayout = { textLayoutResultCount++ }
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("hello")
        }

        rule.runOnIdle {
            assertThat(compositionCount).isEqualTo(1)
            assertThat(textLayoutResultCount).isEqualTo(2)
        }
    }

    @Test
    fun textField_textStyleFontSizeChange_relayouts() {
        val state = TextFieldState(TextFieldValue("Hello ", TextRange(Int.MAX_VALUE)))
        var style by mutableStateOf(TextStyle(fontSize = 20.sp))
        val textLayoutResults = mutableListOf<TextLayoutResult>()
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag),
                textStyle = style,
                onTextLayout = { textLayoutResults += it }
            )
        }

        style = TextStyle(fontSize = 30.sp)

        rule.runOnIdle {
            assertThat(textLayoutResults.size).isEqualTo(2)
            assertThat(textLayoutResults.map { it.layoutInput.style.fontSize })
                .isEqualTo(listOf(20.sp, 30.sp))
        }
    }

    @Test
    fun textField_textStyleColorChange_doesNotRelayout() {
        val state = TextFieldState(TextFieldValue("Hello"))
        var style by mutableStateOf(TextStyle(color = Color.Red))
        val textLayoutResults = mutableListOf<TextLayoutResult>()
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag),
                textStyle = style,
                onTextLayout = { textLayoutResults += it }
            )
        }

        style = TextStyle(color = Color.Blue)

        rule.runOnIdle {
            assertThat(textLayoutResults.size).isEqualTo(2)
            assertThat(textLayoutResults[0].multiParagraph)
                .isSameInstanceAs(textLayoutResults[1].multiParagraph)
        }
    }

    @Test
    fun textField_contentChange_relayouts() {
        val state = TextFieldState(TextFieldValue("Hello ", TextRange(Int.MAX_VALUE)))
        val textLayoutResults = mutableListOf<TextLayoutResult>()
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag),
                onTextLayout = { textLayoutResults += it }
            )
        }

        rule.onNodeWithTag(Tag).performTextInput("World!")

        rule.runOnIdle {
            assertThat(textLayoutResults.size).isEqualTo(2)
            assertThat(textLayoutResults.map { it.layoutInput.text.text })
                .isEqualTo(listOf("Hello ", "Hello World!"))
        }
    }

    @Test
    fun textField_focus_showsSoftwareKeyboard() {
        val state = TextFieldState()
        val keyboardHelper = KeyboardHelper(rule)
        rule.setContent {
            keyboardHelper.initialize()
            BasicTextField2(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performClick()
        rule.onNodeWithTag(Tag).assertIsFocused()

        keyboardHelper.waitForKeyboardVisibility(true)

        rule.runOnIdle {
            assertThat(keyboardHelper.isSoftwareKeyboardShown()).isTrue()
        }
    }

    @Ignore // b/273412941
    @Test
    fun textField_focus_doesNotShowSoftwareKeyboard_ifDisabled() {
        val state = TextFieldState()
        val keyboardHelper = KeyboardHelper(rule)
        rule.setContent {
            keyboardHelper.initialize()
            BasicTextField2(
                state = state,
                enabled = false,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).assertIsNotEnabled()
        rule.onNodeWithTag(Tag).performClick()
        rule.onNodeWithTag(Tag).assertIsNotFocused()

        keyboardHelper.waitForKeyboardVisibility(false)

        rule.runOnIdle {
            assertThat(keyboardHelper.isSoftwareKeyboardShown()).isFalse()
        }
    }

    @Test
    fun textField_whenStateObjectChanges_newTextIsRendered() {
        val state1 = TextFieldState(TextFieldValue("Hello"))
        val state2 = TextFieldState(TextFieldValue("World"))
        var toggleState by mutableStateOf(true)
        val state by derivedStateOf { if (toggleState) state1 else state2 }
        rule.setContent {
            BasicTextField2(
                state = state,
                enabled = true,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).assertTextEquals("Hello")
        toggleState = !toggleState
        rule.onNodeWithTag(Tag).assertTextEquals("World")
    }

    @Test
    fun textField_whenStateObjectChanges_restartsInput() {
        val state1 = TextFieldState(TextFieldValue("Hello"))
        val state2 = TextFieldState(TextFieldValue("World"))
        var toggleState by mutableStateOf(true)
        val state by derivedStateOf { if (toggleState) state1 else state2 }
        rule.setContent {
            BasicTextField2(
                state = state,
                enabled = true,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextReplacement("Compose")
            assertTextEquals("Compose")
        }
        toggleState = !toggleState
        with(rule.onNodeWithTag(Tag)) {
            performTextReplacement("Compose2")
            assertTextEquals("Compose2")
        }
        assertThat(state1.value.text).isEqualTo("Compose")
        assertThat(state2.value.text).isEqualTo("Compose2")
    }
}
