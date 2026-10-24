package com.client.xvideos.x.feature.auth

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Скрипты, которыми вход X управляет страницей входа сайта в WebView.
 *
 * Как устроен вход на сайте (проверено на тестовом аккаунте 24.10.2026):
 *
 * - страница `/account` содержит форму `#signin-form`: скрытый
 *   `signin-form[csrf_token]`, `signin-form[login]` (email),
 *   `signin-form[password]`, `signin-form[rememberme]`;
 * - в форме стоит капча FriendlyCaptcha (`.frc-captcha`, `data-start="none"`):
 *   без её решения сайт отвечает «заполните CAPTCHA» даже на верный пароль.
 *   Капча — доказательство работы, её решает скрипт самой страницы, человеку
 *   ничего вводить не нужно. Решение появляется в скрытом поле
 *   `frc-captcha-response`; пока его нет, там состояние с точкой впереди:
 *   `.UNACTIVATED`, `.SOLVING`;
 * - форму сайт принимает POST-ом на `/account/signinform` и отвечает JSON:
 *   `form_valid: true` — вход выполнен; иначе в `form` лежит та же форма с
 *   ошибками (`.alert-danger` — неверная пара логин/пароль, `.error-block`
 *   у поля — ошибка этого поля, в том числе капчи);
 * - после входа сайт ставит cookie `session_token` и `session_token_auth`,
 *   а `POST /account/checklogin` отвечает `{"is_logged": true}`;
 * - выход — `POST /account/signout?no_redirect=1`.
 *
 * Капчу никто не обходит: её решает виджет сайта в WebView на устройстве, как
 * в браузере, — приложение только заполняет поля и нажимает «Войти».
 */
internal object XSignInScripts {

    /** Страница сайта с формой входа. */
    const val SIGN_IN_PAGE_PATH = "/account"

    /** Ответ [fillFormAndStartCaptcha]: поля заполнены, капча запущена. */
    const val FILL_STARTED = "started"

    /** Ответ [fillFormAndStartCaptcha]: скрипт капчи ещё не загрузился. */
    const val FILL_WAIT = "wait"

    /** Ответ [fillFormAndStartCaptcha]: на странице нет формы входа. */
    const val FILL_NO_FORM = "no-form"

    /** Ответ [fillFormAndStartCaptcha]: в форме входа нет капчи. */
    const val FILL_NO_CAPTCHA = "no-captcha"

    /**
     * Заполняет логин и пароль, отмечает «Запомнить меня» и запускает капчу формы.
     *
     * Пока виджет капчи не создан, ничего не заполняет и отвечает [FILL_WAIT]:
     * скрипт сайта подключает его с задержкой. Повторный вызов безопасен.
     *
     * Логин и пароль встраиваются литералами JSON — это корректные строки JS,
     * кавычки и обратные слэши в пароле скрипт не ломают.
     */
    fun fillFormAndStartCaptcha(login: String, password: String): String = """
        (function(login, password) {
          var form = document.getElementById('signin-form');
          if (!form) return '$FILL_NO_FORM';
          var box = form.querySelector('.frc-captcha');
          if (!box) return '$FILL_NO_CAPTCHA';
          var sdk = window.frcaptcha;
          if (!sdk || !sdk.widgets) return '$FILL_WAIT';
          var widget = null;
          sdk.widgets.forEach(function(w) { if (w.getElement && w.getElement() === box) widget = w; });
          if (!widget) return '$FILL_WAIT';
          function put(id, value) {
            var input = document.getElementById(id);
            if (!input) return;
            input.value = value;
            input.dispatchEvent(new Event('input', { bubbles: true }));
            input.dispatchEvent(new Event('change', { bubbles: true }));
          }
          put('signin-form_login', login);
          put('signin-form_password', password);
          var remember = document.getElementById('signin-form_rememberme');
          if (remember) remember.checked = true;
          widget.start();
          return '$FILL_STARTED';
        })(${jsString(login)}, ${jsString(password)})
    """.trimIndent()

    /** Значение скрытого поля капчи: решение или состояние с точкой впереди. */
    val CAPTCHA_RESPONSE = """
        (function() {
          var form = document.getElementById('signin-form');
          var input = form && form.querySelector('input[name="frc-captcha-response"]');
          return input ? input.value : '';
        })()
    """.trimIndent()

    /**
     * Отправляет форму так же, как её отправляет скрипт сайта, и складывает
     * итог в `window.__appSignIn` строкой JSON — её разбирает [parseSignInPayload].
     *
     * `evaluateJavascript` не ждёт промисов, поэтому итог забирается отдельно,
     * скриптом [SUBMIT_RESULT]. Форма с ошибками разбирается `DOMParser`-ом:
     * в отдельном документе скрипты и картинки ответа не выполняются и не грузятся.
     */
    val SUBMIT = """
        (function() {
          window.__appSignIn = null;
          var out = { answered: false, valid: false, logged: false, message: null, fieldErrors: [], formErrors: [] };
          var form = document.getElementById('signin-form');
          if (!form) { window.__appSignIn = JSON.stringify(out); return 'no-form'; }
          var headers = { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json, text/javascript' };
          fetch('/account/signinform', { method: 'POST', body: new FormData(form), credentials: 'include', headers: headers })
            .then(function(r) { return r.json(); })
            .then(function(j) {
              out.answered = true;
              out.valid = j.form_valid === true;
              if (typeof j.message === 'string') out.message = j.message;
              if (typeof j.form === 'string') {
                var doc = new DOMParser().parseFromString(j.form, 'text/html');
                doc.querySelectorAll('#signin-form .error-block').forEach(function(e) {
                  var group = e.closest('.form-group');
                  var text = e.textContent.trim();
                  if (text) out.fieldErrors.push({ field: group ? group.className : '', text: text });
                });
                doc.querySelectorAll('#signin-form .alert-danger').forEach(function(e) {
                  var text = e.textContent.trim();
                  if (text) out.formErrors.push(text);
                });
              }
              if (!out.valid) return null;
              return fetch('/account/checklogin', { method: 'POST', credentials: 'include', headers: headers })
                .then(function(r) { return r.json(); })
                .then(function(c) { out.logged = c.is_logged === true; });
            })
            .then(function() { window.__appSignIn = JSON.stringify(out); })
            .catch(function() { window.__appSignIn = JSON.stringify(out); });
          return 'sent';
        })()
    """.trimIndent()

    /** Итог [SUBMIT] строкой JSON; пустая строка — ответа ещё нет. */
    const val SUBMIT_RESULT = "(function() { return window.__appSignIn || ''; })()"

    private fun jsString(value: String): String = Json.encodeToString(String.serializer(), value)
}
