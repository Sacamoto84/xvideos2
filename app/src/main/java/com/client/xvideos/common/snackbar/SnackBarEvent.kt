package com.client.xvideos.common.snackbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.client.xvideos.common.eventBus.Event
import com.client.xvideos.common.eventBus.EventBus
import com.client.xvideos.r.common.ThemeRed

object SnackBar{
    //------------------------------------------------------------------------------
    fun info(message: String) {
        EventBus.postEvent( Event.ShowSnackBar(UiMessage.Info(message)))
    }

    fun error(message: String) {
        EventBus.postEvent( Event.ShowSnackBar(UiMessage.Error(message)))
    }

    fun success(message: String) {
        EventBus.postEvent( Event.ShowSnackBar(UiMessage.Success(message)))
    }

    fun warning(message: String) {
        EventBus.postEvent( Event.ShowSnackBar(UiMessage.Warning(message)))
    }
}


//------------------------------------------------------------------------------

@Preview
@Composable
fun RenderSnackBarPreview() {
    RenderSnackBarFilled(UiMessage.Success("This is a success message"))
}

@Preview
@Composable
fun RenderSnackBar1Preview() {
    RenderSnackBarFilled(UiMessage.Success("Добавлен лайк"))
}


@Preview
@Composable
fun RenderSnackBarErrorPreview() {
    RenderSnackBarFilled(UiMessage.Error("This is an error message"))
}

@Preview
@Composable
fun RenderSnackBarInfoPreview() {
    RenderSnackBarFilled(UiMessage.Info("This is an info message"))
}

@Preview
@Composable
fun RenderSnackBarWarningPreview() {
    RenderSnackBarFilled(UiMessage.Warning("This is an info message"))
}



@Preview
@Composable
fun RenderSnackBar3Preview() {
    RenderSnackBar3(UiMessage.Success("This is a success message"))
}

@Preview
@Composable
fun RenderSnackBar3ErrorPreview() {
    RenderSnackBar3(UiMessage.Error("This is an error message"))
}

@Preview
@Composable
fun RenderSnackBar3InfoPreview() {
    RenderSnackBar3(UiMessage.Info("This is an info message"))
}


@Preview
@Composable
fun RenderSnackBar2Preview() {
    RenderSnackBar2(UiMessage.Success("This is a success message"))
}

@Preview
@Composable
fun RenderSnackBar2ErrorPreview() {
    RenderSnackBar2(UiMessage.Error("This is an error message"))
}

@Preview
@Composable
fun RenderSnackBar2InfoPreview() {
    RenderSnackBar2(UiMessage.Info("This is an info message"))
}







@Composable
fun RenderSnackBarFilled (uiMsg: UiMessage){

    val (bg, fg, icon) = when (uiMsg) {
        is UiMessage.Success -> Triple(Color(0xFF01B671), Color.Black, Icons.Rounded.Check)
        is UiMessage.Error -> Triple(Color(0xFFE5553A), Color.Black, Icons.Rounded.ErrorOutline)
        is UiMessage.Info -> Triple(Color(0xFF4276FE), Color.Black, Icons.Rounded.Info)
        is UiMessage.Warning -> Triple(Color(0xFFFF8E0C), Color.Black, Icons.Rounded.WarningAmber)
    }

    Surface(
        modifier = Modifier
            .wrapContentWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .zIndex(Float.MAX_VALUE),
            verticalAlignment = Alignment.CenterVertically
        )
        {

            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x30000000)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }

            Spacer(Modifier.width(8.dp))

            Text( uiMsg.text, Modifier, fontFamily = ThemeRed.fontFamilyPopinsMedium , color = Color.Black)
//            data.visuals.actionLabel?.let { label ->
//                TextButton(onClick = { data.performAction() }) { Text(label) }
//            }
        }
    }


}



/////////////////////////


@Composable
fun RenderSnackBar2 (uiMsg: UiMessage){

    val (bg, fg, icon) = when (uiMsg) {
        is UiMessage.Success -> Triple(Color(0xFF43C558), Color.Black, Icons.Outlined.CheckCircleOutline)
        is UiMessage.Error -> Triple(Color(0xFFFD6969), Color.Black, Icons.Default.Error)
        is UiMessage.Info -> Triple(Color(0xFFBDDBFD), Color.Black, Icons.Default.Info)
        is UiMessage.Warning -> Triple(Color(0xFFFF8E0C), Color.Black, Icons.Default.Warning)
    }

    Surface(
        modifier = Modifier
            .wrapContentWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier.heightIn(min = 48.dp)
                .padding(start = 12.dp)
                .background(Color.White).padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        )
        {
            Box(Modifier.width(8.dp)
                .background(Color(0xFF22C55C)
                )){}


                    Icon(icon, contentDescription = null, tint = Color(0xFF22C55C))


            Spacer(Modifier.width(8.dp))
            Text( uiMsg.text, Modifier, fontFamily = ThemeRed.fontFamilyDMsanss )
//            data.visuals.actionLabel?.let { label ->
//                TextButton(onClick = { data.performAction() }) { Text(label) }
//            }
        }
    }


}
///

@Composable
fun RenderSnackBar3 (uiMsg: UiMessage){

    val (bg, fg, icon) = when (uiMsg) {
        is UiMessage.Success -> Triple(Color(0xFF43C558), Color.Black, Icons.Outlined.CheckCircleOutline)
        is UiMessage.Error -> Triple(Color(0xFFFD6969), Color.Black, Icons.Default.Error)
        is UiMessage.Info -> Triple(Color(0xFFBDDBFD), Color.Black, Icons.Default.Info)
        is UiMessage.Warning -> Triple(Color(0xFFFF8E0C), Color.Black, Icons.Default.Warning)
    }

    Surface(
        modifier = Modifier
            .wrapContentWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier.heightIn(min = 48.dp)
                .padding(start = 12.dp)
                .background(Color.White).padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        )
        {
            Box(Modifier.width(8.dp)
                .background(Color(0xFF22C55C)
                )){}


            Icon(icon, contentDescription = null, tint = Color(0xFF22C55C))


            Spacer(Modifier.width(8.dp))
            Text( uiMsg.text, Modifier, fontFamily = ThemeRed.fontFamilyDMsanss )
//            data.visuals.actionLabel?.let { label ->
//                TextButton(onClick = { data.performAction() }) { Text(label) }
//            }
        }
    }


}



