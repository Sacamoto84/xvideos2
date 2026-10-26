package com.client.xvideos.xlive.ui.room.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.xlive.record.XLiveRecordingUi
import com.client.xvideos.xlive.ui.room.XLiveFollowState
import com.client.xvideos.xlive.ui.room.atom.XLiveFollowButton
import com.client.xvideos.xlive.ui.room.atom.XLiveRecordButton
import com.client.xvideos.xlive.ui.room.atom.XLiveSaveButton

/**
 * Круглые кнопки справа в шапке комнаты: «подписаться», «в мои», запись.
 *
 * Кнопки по 48dp, не меньше: `IconButton` сам дотягивает себя до 48dp зоны
 * нажатия, и круг меньшего размера рисовался бы на 48dp поверх отступа —
 * кнопки стояли бы впритык.
 *
 * @param follow подписка на сайте.
 * @param onToggleFollow «подписаться» со входом: подписаться или отписаться.
 * @param onSignIn «подписаться» без входа: страница входа.
 * @param isSaved комната в «Моих».
 * @param recording запись эфира.
 */
@Composable
fun XLiveRoomButtons(
    follow: XLiveFollowState,
    onToggleFollow: () -> Unit,
    onSignIn: () -> Unit,
    isSaved: Boolean,
    onToggleSaved: () -> Unit,
    recording: XLiveRecordingUi,
    onToggleRecording: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        XLiveFollowButton(
            signedIn = follow.isSignedIn,
            following = follow.following,
            busy = follow.isBusy,
            onClick = if (follow.isSignedIn) onToggleFollow else onSignIn,
        )
        XLiveSaveButton(saved = isSaved, onClick = onToggleSaved)
        XLiveRecordButton(
            recording = recording.isRecording,
            finishing = recording.isFinishing,
            onClick = onToggleRecording,
        )
    }
}

@Preview
@Composable
private fun XLiveRoomButtonsPreview() {
    XLiveRoomButtons(
        follow = XLiveFollowState(isSignedIn = true, following = true),
        onToggleFollow = {},
        onSignIn = {},
        isSaved = true,
        onToggleSaved = {},
        recording = XLiveRecordingUi(isRecording = true),
        onToggleRecording = {},
    )
}
