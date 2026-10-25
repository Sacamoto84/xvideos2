package com.client.xvideos.xlive.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.xlive.auth.X_LIVE_SIGN_IN_URL
import com.client.xvideos.xlive.ui.XLiveColors
import com.client.xvideos.xlive.ui.login.molecule.XLiveLoginPage
import com.client.xvideos.xlive.ui.login.molecule.XLiveLoginTopBar

/**
 * Вход на сайт XLive: страница входа сайта в WebView со всеми её шагами.
 * Экран закрывается сам, когда сайт подтвердил вход
 * ([com.client.xvideos.xlive.auth.XLiveSession.confirmSignIn]).
 */
class ScreenXLiveLogin : Screen {

    override val key: ScreenKey = "ScreenXLiveLogin"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = rememberScreenModel { XLiveLoginSM() }
        val ready by vm.ready.collectAsStateWithLifecycle()
        val done by vm.done.collectAsStateWithLifecycle()

        LaunchedEffect(done) {
            if (done) navigator.pop()
        }

        ScreenXLiveLoginContent(
            ready = ready,
            onBack = { navigator.pop() },
            onLeftSignInPages = vm::onLeftSignInPages,
        )
    }
}

/**
 * @param ready форму можно открывать: прежний выход закончился.
 * @param onLeftSignInPages WebView ушёл со служебных страниц входа с cookie сессии.
 */
@Composable
fun ScreenXLiveLoginContent(
    ready: Boolean,
    onBack: () -> Unit,
    onLeftSignInPages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XLiveColors.Background)
            .imePadding()
    ) {
        XLiveLoginTopBar(onBack = onBack)
        Box(modifier = Modifier.weight(1f)) {
            if (ready) XLiveLoginPage(url = X_LIVE_SIGN_IN_URL, onLeftSignInPages = onLeftSignInPages)
        }
    }
}

@Preview(device = "id:pixel_9_pro")
@Composable
private fun ScreenXLiveLoginPreview() {
    ScreenXLiveLoginContent(ready = false, onBack = {}, onLeftSignInPages = {})
}
