package com.client.xvideos.r.ui.explorer.tab.saved.tab.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.r.auth.RAccountSession
import com.client.xvideos.r.auth.RAccountStatus

@Composable
internal fun signedInAccount(): RAccountStatus.SignedIn? {
    if (LocalInspectionMode.current) return null
    val account by RAccountSession.status.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { RAccountSession.restore() }
    return account as? RAccountStatus.SignedIn
}
