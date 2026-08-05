package com.client.xvideos.common.applock

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

object AccessCodeVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            text = AnnotatedString("*".repeat(text.text.length)),
            offsetMapping = OffsetMapping.Identity
        )
    }
}
