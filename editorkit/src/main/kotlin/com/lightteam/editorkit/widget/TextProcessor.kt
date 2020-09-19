/*
 * Licensed to the Light Team Software (Light Team) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The Light Team licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lightteam.editorkit.widget

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.core.content.getSystemService
import com.lightteam.editorkit.R
import com.lightteam.editorkit.feature.gotoline.LineException
import com.lightteam.editorkit.feature.shortcuts.Shortcut
import com.lightteam.editorkit.feature.shortcuts.ShortcutListener
import com.lightteam.editorkit.internal.CodeSuggestsEditText

class TextProcessor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.autoCompleteTextViewStyle
) : CodeSuggestsEditText(context, attrs, defStyleAttr) {

    companion object {
        private const val LABEL_CUT = "CUT"
        private const val LABEL_COPY = "COPY"
    }

    var shortcutListener: ShortcutListener? = null

    private val clipboardManager = context.getSystemService<ClipboardManager>()!!

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && shortcutListener != null) {
            val shortcut = Shortcut(
                ctrl = event.isCtrlPressed,
                shift = event.isShiftPressed,
                alt = event.isAltPressed,
                keyCode = keyCode
            )

            // Shortcuts can be handled only if one of these keys is pressed
            if (shortcut.ctrl || shortcut.shift || shortcut.alt) {
                if (shortcutListener!!.onShortcut(shortcut)) {
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    fun insert(delta: CharSequence) {
        text.replace(selectionStart, selectionEnd, delta)
    }

    fun cut() {
        val clipData = ClipData.newPlainText(LABEL_CUT, selectedText())
        clipboardManager.setPrimaryClip(clipData)
        text.replace(selectionStart, selectionEnd, "")
    }

    fun copy() {
        val clipData = ClipData.newPlainText(LABEL_COPY, selectedText())
        clipboardManager.setPrimaryClip(clipData)
    }

    fun paste() {
        val clipData = clipboardManager.primaryClip?.getItemAt(0)
        val clipText = clipData?.coerceToText(context)
        text.replace(selectionStart, selectionEnd, clipText)
    }

    fun selectLine() {
        val currentLine = lines.getLineForIndex(selectionStart)
        val lineStart = getIndexForStartOfLine(currentLine)
        val lineEnd = getIndexForEndOfLine(currentLine)
        setSelection(lineStart, lineEnd)
    }

    fun deleteLine() {
        val currentLine = lines.getLineForIndex(selectionStart)
        val lineStart = getIndexForStartOfLine(currentLine)
        val lineEnd = getIndexForEndOfLine(currentLine)
        text.delete(lineStart, lineEnd)
    }

    fun duplicateLine() {
        val currentLine = lines.getLineForIndex(selectionStart)
        val lineStart = getIndexForStartOfLine(currentLine)
        val lineEnd = getIndexForEndOfLine(currentLine)
        val lineText = text.subSequence(lineStart, lineEnd)
        text.insert(lineEnd, "\n" + lineText)
    }

    fun moveCaretToStartOfLine(): Boolean {
        val currentLine = lines.getLineForIndex(selectionStart)
        val lineStart = getIndexForStartOfLine(currentLine)
        setSelection(lineStart)
        return true
    }

    fun moveCaretToEndOfLine(): Boolean {
        val currentLine = lines.getLineForIndex(selectionEnd)
        val lineEnd = getIndexForEndOfLine(currentLine)
        setSelection(lineEnd)
        return true
    }

    fun moveCaretToPrevWord(): Boolean {
        if (selectionStart > 0) {
            val currentChar = text[selectionStart - 1]
            val isLetterDigitOrUnderscore = currentChar.isLetterOrDigit() || currentChar == '_'
            if (isLetterDigitOrUnderscore) {
                for (i in selectionStart downTo 0) {
                    val char = text[i - 1]
                    if (!char.isLetterOrDigit() && char != '_') {
                        setSelection(i)
                        break
                    }
                }
            } else {
                for (i in selectionStart downTo 0) {
                    val char = text[i - 1]
                    if (char.isLetterOrDigit() || char == '_') {
                        setSelection(i)
                        break
                    }
                }
            }
        }
        return true
    }

    fun moveCaretToNextWord(): Boolean {
        if (selectionStart < text.length) {
            val currentChar = text[selectionStart]
            val isLetterDigitOrUnderscore = currentChar.isLetterOrDigit() || currentChar == '_'
            if (isLetterDigitOrUnderscore) {
                for (i in selectionStart until text.length) {
                    val char = text[i]
                    if (!char.isLetterOrDigit() && char != '_') {
                        setSelection(i)
                        break
                    }
                }
            } else {
                for (i in selectionStart until text.length) {
                    val char = text[i]
                    if (char.isLetterOrDigit() || char == '_') {
                        setSelection(i)
                        break
                    }
                }
            }
        }
        return true
    }

    @Throws(LineException::class)
    fun gotoLine(lineNumber: Int) {
        val line = lineNumber - 1
        if (line < 0 || line >= lines.lineCount - 1) {
            throw LineException(lineNumber)
        }
        setSelection(lines.getIndexForLine(line))
    }

    fun hasPrimaryClip(): Boolean {
        return clipboardManager.hasPrimaryClip()
    }

    private fun selectedText(): CharSequence {
        return text.subSequence(selectionStart, selectionEnd)
    }
}