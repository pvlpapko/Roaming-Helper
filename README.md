# Помощник роуминга

Android-приложение для пожилых пользователей, которые едут из Беларуси в Россию и хотят проще настроить связь в роуминге.

Видимое название приложения: **Помощник роуминга**  
Внутренняя информация проекта: **LTM_Fedory / Roaming Helper**  
Package name: `com.ltmfedory.roaminghelper`

## Что умеет приложение

- открывает скрытый или системный экран выбора режима сети;
- помогает выбрать режимы **4G + 3G**, **только 4G**, **только 3G**;
- не использует 5G и не предлагает 2G;
- открывает ручной выбор оператора;
- открывает настройки роуминга данных;
- показывает краткое состояние SIM/сети/роуминга;
- содержит офлайн-инструкцию “SMS не приходит”;
- не читает SMS и не требует SMS-разрешений;
- учитывает Android 15+ edge-to-edge и не прячет интерфейс под статус-баром.

## Важное ограничение

Обычная APK без root не может гарантированно заставить SIM зарегистрироваться в конкретной сети и не может принудительно снять операторскую блокировку SMS/интернета. Приложение безопасно открывает нужные настройки и ведёт пользователя по шагам.

## Сборка на GitHub

В проект уже добавлен workflow:

```text
.github/workflows/android-build.yml
```

Он собирает:

- debug APK;
- release AAB для Google Play.

### Как собрать

1. Создайте новый репозиторий на GitHub.
2. Загрузите все файлы проекта.
3. Откройте вкладку **Actions**.
4. Запустите **Android Build** вручную или сделайте push в `main`.
5. Готовые файлы будут в **Artifacts**:
   - `roaming-helper-debug-apk`;
   - `roaming-helper-release-aab`.

## Подпись AAB для Play Market

Для публикации в Google Play нужно добавить upload-keystore в GitHub Secrets.

### 1. Создать keystore

```bash
keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### 2. Закодировать keystore в base64

Linux/macOS:

```bash
base64 -w 0 upload-keystore.jks > upload-keystore.base64.txt
```

Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-keystore.jks")) | Out-File upload-keystore.base64.txt
```

### 3. Добавить GitHub Secrets

В репозитории: **Settings → Secrets and variables → Actions → New repository secret**.

Добавьте:

```text
UPLOAD_KEYSTORE_BASE64
UPLOAD_KEYSTORE_PASSWORD
UPLOAD_KEY_ALIAS
UPLOAD_KEY_PASSWORD
```

После этого `android-build.yml` будет собирать подписанный release AAB.

## Черновая публикация в Google Play

Добавлен отдельный workflow:

```text
.github/workflows/play-internal-draft.yml
```

Он загружает AAB в Google Play Internal testing как draft.

Для него нужны Secrets:

```text
UPLOAD_KEYSTORE_BASE64
UPLOAD_KEYSTORE_PASSWORD
UPLOAD_KEY_ALIAS
UPLOAD_KEY_PASSWORD
GOOGLE_PLAY_SERVICE_ACCOUNT_JSON
```

Также приложение с package name `com.ltmfedory.roaminghelper` должно быть заранее создано в Play Console.

## Настройки версии

Версия задаётся в файле:

```text
app/build.gradle
```

```gradle
versionCode 1
versionName '1.0.0'
```

Перед каждой публикацией увеличивайте `versionCode`.

## Проверка на телефоне

Сначала установите debug APK из GitHub Actions. На разных телефонах скрытое меню сети может называться по-разному или быть заблокировано производителем. В этом случае приложение откроет обычные настройки сети или покажет код `*#*#4636#*#*`.
