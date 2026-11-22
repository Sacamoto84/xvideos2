# Запуск Android-приложения

Холодный запуск из лаунчера до рабочего меню входит в Startup Profile и Baseline Profile. Переход из меню на экран R входит в Baseline Profile. Сценарий R заканчивается после появления его интерфейса и работает без ответа сервера. Для замера полной загрузки ленты нужны одинаковые данные и сетевые условия.

## Генерация

На тестовом устройстве с Android 13+ и чистыми данными приложения запустите

```powershell
.\gradlew.bat :app:generateBaselineProfile "-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile"
```

Используется модуль [baselineprofile](../baselineprofile/build.gradle). Достаточно одного подключённого тестового устройства. Замок и режим калькулятора должны быть выключены в этой тестовой установке. Задача сохраняет профили в `app/src/main/generated/baselineProfiles`. Коммитьте обновлённые файлы вместе с изменениями сценариев. Перегенерируйте их после изменений в запуске или навигации.

Сборка `:app:assembleRelease` использует сохранённые профили без устройства. R8 и сжатие ресурсов включены. AGP применяет Startup Profile при сборке и упаковывает Baseline Profile в `assets/dexopt/baseline.prof`. Для AAB проверьте `r8.json`. У DEX с применённым Startup Profile появится `"startup": true`. Размер двоичного Baseline Profile должен оставаться меньше 1,5 МБ.

## A/B холодного запуска

На физическом Android-устройстве сравните один и тот же минфицированный APK без AOT-компиляции (`CompilationMode.None`) и с обязательным Baseline Profile (`CompilationMode.Partial`). Оба режима используют одну раскладку DEX. Такое сравнение измеряет эффект предкомпиляции ART. Для отдельного замера Startup Profile соберите ещё один APK из того же коммита без `startup-prof.txt`, сохранив `baseline-prof.txt` и остальные параметры сборки.

```powershell
.\gradlew.bat :baselineprofile:connectedBenchmarkReleaseAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.client.xvideos.baselineprofile.StartupBenchmark"
```

Сравните медиану и разброс десяти холодных запусков по TTID и TTFD. Приложение сообщает TTFD после отрисовки открытого интерфейса или экрана замка. Macrobenchmark выше измеряет меню. Для PIN и калькулятора запишите отдельные прогоны на тестовой копии с включённой защитой.

При установке прямо из APK ProfileInstaller подготавливает профиль к фоновой компиляции ART. Раскладка DEX применяется уже при сборке и работает с первого запуска. Для локальной проверки на тестовом устройстве можно принудительно применить предкомпиляцию по профилю.

```powershell
adb shell am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE -n com.client.xvideos/androidx.profileinstaller.ProfileInstallReceiver
adb shell am force-stop com.client.xvideos
adb shell cmd package compile -f -m speed-profile com.client.xvideos
adb shell pm art dump com.client.xvideos
```

`pm art dump` доступна на Android 14+. Наличие файла в APK подтверждает упаковку, а `speed-profile` после этих команд подтверждает применение на тестовом устройстве. При распространении через Play AOT-профиль может применяться при установке через Dex Metadata.

## Локальная проверка релиза

На чистом эмуляторе Android 16 x86-64 без сети выполнено по семь холодных запусков после одного пробного старта. Измерялся первый кадр MainActivity по `am start -S -W`. Режимы `verify` и `speed-profile` заданы явно. Для старого APK `reportFullyDrawn()` ещё не был подключён.

| APK и режим ART | Медиана, мс | Диапазон, мс |
| --- | ---: | ---: |
| Старый, `verify` | 454 | 406–482 |
| Новый, `verify` | 410 | 269–2403 |
| Новый, `speed-profile` | 355 | 337–379 |

Отдельно прогнан Macrobenchmark на том же эмуляторе, по 10 запусков на режим. Медианы TTID и TTFD составили 1284 мс в `CompilationMode.None` и 982 мс в `CompilationMode.Partial` с обязательным Baseline Profile. Эти серии сравниваются между собой. Для оценки на телефоне нужен прогон на физическом устройстве.

ProfileInstaller вернул `result=1`, а ART подтвердил `speed-profile`. Релизный APK содержит `baseline.prof` размером 12 378 байт. В отчёте R8 внутри AAB первому DEX присвоено `startup=true`.

[Профили Android](https://developer.android.com/topic/performance/baselineprofiles/difference-baseline-startup) · [Проверка применения](https://developer.android.com/topic/performance/baselineprofiles/debug-baseline-profiles)
