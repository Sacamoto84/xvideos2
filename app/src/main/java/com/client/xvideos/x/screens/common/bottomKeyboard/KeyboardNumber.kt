package com.client.xvideos.screens.common.bottomKeyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.R
import timber.log.Timber

/**
 * Тема для клавиатуры
 */
data class KeyboardNumberTheme(
    //Диалог
    val colorBackground: Color = Color(0xFF3D3F4A), //Цвет всего фона диалога
    val colorBorderBackground: Color = Color.Transparent, //Цвет фона границы


    val colorText: Color = Color(0xFFDEE1EF),

    //Кнопки
    val buttonColor: Color = Color(0xFF5A5D6C),
    val buttonWidth: Dp = 66.dp,
    val buttonHeight: Dp = 48.dp,
    val buttonCornerRadius: Int = 40,
    val buttonPadding: Dp = 4.dp,

    val colorButtonBorder: Color = Color.Transparent,
)

@Composable
fun KeyboardNumber(
    value: Int,
    max: Int,
    theme: KeyboardNumberTheme = KeyboardNumberTheme(),
    onClick: (Int) -> Unit,
) {

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(if (value == -1) "" else value.toString(), TextRange(value.toString().length)))
    }

    Column {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.width((300 - 8).dp)//.padding(end = 3.dp)//.fillMaxWidth()
        ) {
            //Циферблат
            Box(
                modifier = Modifier
                    .padding(
                        start = theme.buttonPadding,
                        top = theme.buttonPadding,
                        bottom = theme.buttonPadding / 2
                    )
                    .height(theme.buttonHeight)
                    .width(theme.buttonWidth * 3 + theme.buttonPadding * 4)
                    .clip(RoundedCornerShape(theme.buttonCornerRadius))
                    .border(2.dp, theme.colorBorderBackground, RoundedCornerShape(theme.buttonCornerRadius))
                    .background(theme.colorBackground),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    textFieldValue.text,
                    color = theme.colorText,
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "($max)",
                    color = theme.colorText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(modifier = Modifier.padding(bottom = 0.dp)) {

            //ButtonNumber("1", w =  48.dp, h = 48.dp * 4 + 1.dp + 1.dp, onClick =  { textFieldValue = TextFieldValue("1", TextRange(1)) })

            FlowRow(maxItemsInEachRow = 3) {
                KeyboardButtonNumber(
                    "7",
                    onClick = { textFieldValue = addCharToTextField(textFieldValue, "7") },
                    theme = theme
                )
                KeyboardButtonNumber(
                    "8",
                    onClick = { textFieldValue = addCharToTextField(textFieldValue, "8") },
                    theme = theme
                )
                KeyboardButtonNumber(
                    "9",
                    onClick = { textFieldValue = addCharToTextField(textFieldValue, "9") },
                    theme = theme
                )

                KeyboardButtonNumber(
                    "4",
                    onClick = { textFieldValue = addCharToTextField(textFieldValue, "4") },
                    theme = theme
                )
                KeyboardButtonNumber(
                    "5",
                    onClick = { textFieldValue = addCharToTextField(textFieldValue, "5") },
                    theme = theme
                )
                KeyboardButtonNumber(
                    "6",
                    onClick = { textFieldValue = addCharToTextField(textFieldValue, "6") },
                    theme = theme
                )

                KeyboardButtonNumber(
                    "1",
                    onClick = { textFieldValue = addCharToTextField(textFieldValue, "1") },
                    theme = theme
                )
                KeyboardButtonNumber(
                    "2",
                    onClick = { textFieldValue = addCharToTextField(textFieldValue, "2") },
                    theme = theme
                )
                KeyboardButtonNumber(
                    "3",
                    onClick = { textFieldValue = addCharToTextField(textFieldValue, "3") },
                    theme = theme
                )

                KeyboardButtonNumber(
                    "C",
                    onClick = { textFieldValue = TextFieldValue("", TextRange(0)) }, theme = theme
                )
                KeyboardButtonNumber(
                    "0",
                    onClick = { textFieldValue = addCharToTextField(textFieldValue, "0") },
                    theme = theme
                )
                KeyboardButtonNumber("<-", onClick = {
                    val newText = textFieldValue.text.dropLast(1)
                    val newCursorPosition = newText.length
                    textFieldValue = TextFieldValue(newText, TextRange(newCursorPosition))
                }, theme = theme)
            }

            //Энтер
            Box(
                modifier = Modifier
                    .padding(start = theme.buttonPadding, top = 4.dp, end = theme.buttonPadding)
                    .width(theme.buttonWidth)
                    .height(theme.buttonHeight * 4 + theme.buttonPadding * 6)
                    .clip(RoundedCornerShape(theme.buttonCornerRadius))
                    //.border(2.dp, Color(0xFF1F5AFF      ), RoundedCornerShape(theme.buttonCornerRadius))
                    .background(Color(0xFFFF7043))
                    .clickable {
                        val a = textFieldValue.text.toIntOrNull()
                        if (a != null) {
                            try {
                                val i = a.coerceIn(1, max)
                                onClick.invoke(i)
                            } catch (e: Exception) {
                                Timber.e(e.localizedMessage)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.enter),
                    tint = Color.White,
                    contentDescription = "TODO()",
                    modifier = Modifier
                )
            }


        }
    }

}

private fun addCharToTextField(textFieldValue: TextFieldValue, char: String): TextFieldValue {
    val newText = textFieldValue.text + char
    val newCursorPosition = newText.length  // Курсор перемещаем в конец текста
    return TextFieldValue(newText, TextRange(newCursorPosition))
}


@Preview
@Composable
private fun Prev() {
    KeyboardNumber(5, 10, onClick = {})
}
