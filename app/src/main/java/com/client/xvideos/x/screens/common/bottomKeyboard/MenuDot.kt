package com.client.xvideos.screens.common.bottomKeyboard

import android.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.composables.core.HorizontalSeparator
import com.composeunstyled.Button

@Preview
@Composable
private fun MenuPreview() {
    MenuDot(value = 1, onChange = {}, max = 10)
}


private val height = 48.dp

//Черный цвет текста
private val colorTextBlack = Color(0xFF2C2C2C)

private val colorAccent = Color(0xFFFF9000)

private val colorTextWhite = Color(0xFFCCCCCC)

private val colorBlackBackground = Color(0xff3b3b3b)

/**
 * Кнопка с вызываемым диалогом
 */
@Composable
fun MenuDot(modifier: Modifier = Modifier, value: Int, onChange: (Int) -> Unit, max: Int) {

//    val state = rememberMenuState(expanded = false)
//
//    Box(
//        Modifier
//            .padding(horizontal = (0.5).dp)
//            .then(modifier)
//            .height(height)
//    ) {
//
//        Menu(
//            modifier = Modifier,
//            state = state
//        ) {
//
//            //Сама кнопка для вызова диалога
//            MenuButton(
//                Modifier
//                    .fillMaxSize()
//                    .background(colorBlackBackground)
//            ) {
//                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    BasicText(
//                        "...",
//                        style = TextStyle(
//                            fontWeight = FontWeight.Medium,
//                            color = colorTextWhite,
//                            fontSize = 24.sp
//                        )
//                    )
//                }
//            }
//
//            MenuContent(
//                modifier = Modifier
//                    .padding(bottom = 0.dp)
//                    .width(312.dp)
//                    .clip(RoundedCornerShape(26.dp))
//                    .background(Color(0xFF23242A)),
//                // exit = fadeOut()
//                //, enter = fadeIn()
//            ) {
//                Box(
//                    modifier = Modifier
//                        .padding(top = 8.dp)
//                        .padding(horizontal = 8.dp), contentAlignment = Alignment.Center
//                ) {
//                    //Клавиатура возвращает число
//                    KeyboardNumber(
//                        value = value, max = max,
//                        onClick = { onChange.invoke(it); state.expanded = false },
//                    )
//                }
//
//            }
//
//        }
//
//    }

}


@Composable
fun MenuDotConfig(modifier: Modifier = Modifier, setShowDialog: (Boolean) -> Unit) {

    val txtFieldError = remember { mutableStateOf("") }
    val txtField = remember { mutableStateOf("value") }


    Dialog(
        onDismissRequest = { setShowDialog(false) },
    ) {

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Set value",
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Icon(
                            imageVector = Icons.Filled.Cancel,
                            contentDescription = "",
                            tint = colorResource(R.color.darker_gray),
                            modifier = Modifier
                                .width(30.dp)
                                .height(30.dp)
                                .clickable {
                                    setShowDialog(false)
                                }
                        )
                    }

                    HorizontalSeparator(color = Color(0xFF9E9E9E))


                    Box(modifier = Modifier.padding(40.dp, 0.dp, 40.dp, 0.dp)) {
                        Button(
                            onClick = {
                                if (txtField.value.isEmpty()) {
                                    txtFieldError.value = "Field can not be empty"
                                    return@Button
                                }

                                //setValue(txtField.value)
                                //setShowDialog(false)

                            },
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(text = "Done")
                        }
                    }
                }
            }
        }
    }

}

