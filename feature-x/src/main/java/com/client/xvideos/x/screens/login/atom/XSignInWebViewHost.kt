package com.client.xvideos.x.screens.login.atom

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Держит WebView идущего входа в окне формы — невидимым, размером в 1dp.
 *
 * WebView без окна считает страницу скрытой, как вкладку в фоне, а таймеры
 * скрытой страницы браузер притормаживает; капчу сайта решают как раз скрипты
 * страницы. В окне страница видима, как в браузере.
 * WebView принадлежит входу (`XSession`), а не форме: при повороте экрана и
 * раскрытии складного вход продолжается, а новая форма снова берёт его в окно.
 *
 * @param webView WebView входа; `null` — вход не идёт, держать нечего.
 */
@Composable
fun XSignInWebViewHost(
    webView: WebView?,
    modifier: Modifier = Modifier,
) {
    if (webView == null) return
    key(webView) {
        AndroidView(
            factory = {
                // Прежняя форма (до поворота) могла ещё не отпустить его.
                (webView.parent as? ViewGroup)?.removeView(webView)
                webView
            },
            modifier = modifier
                .size(1.dp)
                .alpha(0f),
            // Уничтожает WebView сам вход; форма только отпускает его.
            onRelease = { view -> (view.parent as? ViewGroup)?.removeView(view) },
        )
    }
}

@Preview
@Composable
private fun XSignInWebViewHostPreview() {
    XSignInWebViewHost(webView = null)
}
