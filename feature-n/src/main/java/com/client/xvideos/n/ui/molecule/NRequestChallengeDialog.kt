package com.client.xvideos.n.ui.molecule

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.n.net.NCloudflareState
import com.client.xvideos.n.repository.CloudflareBlockedException

@Composable
internal fun NRequestChallengeDialog(
    challenge: CloudflareBlockedException,
    onDismiss: () -> Unit,
    onVerified: () -> Unit,
) {
    NCloudflareChallengeDialog(
        url = challenge.url,
        rejectedClearance = challenge.sentClearance,
        onDismiss = onDismiss,
        onSuccess = { cookies ->
            NCloudflareState.onChallengeCompleted(cookies)
            onVerified()
        },
    )
}

@Preview
@Composable
private fun NRequestChallengeDialogPreview() {
    NRequestChallengeDialog(
        challenge = CloudflareBlockedException(url = "https://nhentai.net"),
        onDismiss = {},
        onVerified = {},
    )
}
