package com.ltmfedory.roaminghelper;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQUEST_READ_PHONE_STATE = 101;

    private LinearLayout rootLayout;
    private TextView statusText;
    private TextView hintText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        buildUi();
        refreshStatus();
    }

    private void configureWindow() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(color("#F6F8FC"));
            window.setNavigationBarColor(color("#F6F8FC"));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(color("#F6F8FC"));

        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(dp(16), dp(16), dp(16), dp(16));
        scrollView.addView(rootLayout, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        applySafeInsets(scrollView);

        TextView title = new TextView(this);
        title.setText("Помощник роуминга");
        title.setTextColor(color("#172033"));
        title.setTextSize(25);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        rootLayout.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Быстрый переход к режимам 4G/3G, ручному выбору оператора и инструкции для SMS в роуминге.");
        subtitle.setTextColor(color("#5F6B7A"));
        subtitle.setTextSize(15);
        subtitle.setLineSpacing(dp(2), 1.0f);
        rootLayout.addView(subtitle, topMargin(matchWrap(), 6));

        LinearLayout statusCard = card();
        statusText = new TextView(this);
        statusText.setTextColor(color("#172033"));
        statusText.setTextSize(15);
        statusText.setLineSpacing(dp(3), 1.0f);
        statusCard.addView(statusText, matchWrap());

        Button refresh = secondaryButton("Обновить состояние");
        refresh.setOnClickListener(v -> refreshStatusWithPermission());
        statusCard.addView(refresh, topMargin(matchWrap(), 12));
        rootLayout.addView(statusCard, topMargin(matchWrap(), 16));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        grid.setUseDefaultMargins(false);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        rootLayout.addView(grid, topMargin(matchWrap(), 12));

        addGridButton(grid, "4G + 3G", true, () -> showNetworkModeGuide(
                "4G + 3G авто",
                "Основной режим для поездки. Выберите LTE/WCDMA, LTE/UMTS или похожий пункт без 2G и без 5G.",
                "LTE/WCDMA"
        ));
        addGridButton(grid, "Только 4G", false, () -> showNetworkModeGuide(
                "Только 4G",
                "Для интернета. Если SMS или звонки не приходят, вернитесь на 4G + 3G.",
                "LTE only"
        ));
        addGridButton(grid, "Только 3G", false, () -> showNetworkModeGuide(
                "Только 3G",
                "Аварийный режим, когда SMS с кодом не приходит или 4G плохо регистрируется.",
                "WCDMA only"
        ));
        addGridButton(grid, "Оператор", false, this::openOperatorSelectionGuide);
        addGridButton(grid, "SMS не приходит", false, this::showSmsHelp);
        addGridButton(grid, "Роуминг РФ", false, this::showRussiaRoamingGuide);

        Button dataRoaming = secondaryButton("Открыть настройки роуминга данных");
        dataRoaming.setOnClickListener(v -> openSettingsIntent(Settings.ACTION_DATA_ROAMING_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS));
        rootLayout.addView(dataRoaming, topMargin(matchWrap(), 14));

        Button restore = secondaryButton("Вернуть обычный режим 4G + 3G");
        restore.setOnClickListener(v -> showNetworkModeGuide(
                "Вернуть 4G + 3G",
                "Откройте выбор типа сети и выберите LTE/WCDMA, LTE/UMTS или похожий автоматический режим без 2G и без 5G.",
                "LTE/WCDMA"
        ));
        rootLayout.addView(restore, topMargin(matchWrap(), 8));

        hintText = new TextView(this);
        hintText.setText("Важно: приложение не обходит ограничения операторов и не читает SMS. Оно помогает быстро открыть нужные настройки и не запутаться в роуминге.");
        hintText.setTextColor(color("#5F6B7A"));
        hintText.setTextSize(13);
        hintText.setLineSpacing(dp(3), 1.0f);
        rootLayout.addView(hintText, topMargin(matchWrap(), 18));

        setContentView(scrollView);
    }

    private void applySafeInsets(View view) {
        final int baseLeft = view.getPaddingLeft();
        final int baseTop = view.getPaddingTop();
        final int baseRight = view.getPaddingRight();
        final int baseBottom = view.getPaddingBottom();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            view.setOnApplyWindowInsetsListener((v, insets) -> {
                int left;
                int top;
                int right;
                int bottom;

                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();

                v.setPadding(baseLeft + left, baseTop + top, baseRight + right, baseBottom + bottom);
                return insets;
            });
            view.requestApplyInsets();
        }
    }

    private void refreshStatusWithPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
            return;
        }
        refreshStatus();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_PHONE_STATE) {
            refreshStatus();
        }
    }

    private void refreshStatus() {
        StringBuilder builder = new StringBuilder();
        builder.append("Текущее состояние\n");

        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) {
                statusText.setText("Телефонный модуль не найден. Возможно, устройство без SIM-модуля.");
                return;
            }

            String simOperator = emptyToDash(tm.getSimOperatorName());
            String networkOperator = emptyToDash(tm.getNetworkOperatorName());
            builder.append("SIM: ").append(simOperator).append("\n");
            builder.append("Сеть: ").append(networkOperator).append("\n");
            builder.append("Роуминг: ").append(tm.isNetworkRoaming() ? "да" : "нет/не определён").append("\n");

            if (hasReadPhoneState()) {
                int dataType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? tm.getDataNetworkType() : tm.getNetworkType();
                int voiceType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? tm.getVoiceNetworkType() : tm.getNetworkType();
                builder.append("Интернет: ").append(networkTypeName(dataType)).append("\n");
                builder.append("Голос/SMS: ").append(networkTypeName(voiceType));
            } else {
                builder.append("Тип сети: нажмите “Обновить состояние” и разрешите доступ к состоянию телефона.");
            }
        } catch (SecurityException e) {
            builder.append("Нет разрешения на чтение состояния сети. Основные кнопки всё равно работают.");
        } catch (Exception e) {
            builder.append("Не удалось прочитать состояние сети. Основные кнопки всё равно работают.");
        }

        statusText.setText(builder.toString());
    }

    private boolean hasReadPhoneState() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    private String networkTypeName(int type) {
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G / LTE";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
            case TelephonyManager.NETWORK_TYPE_GSM:
                return "2G — не используется в этом помощнике";
            case TelephonyManager.NETWORK_TYPE_NR:
                return "5G — не используется в этом помощнике";
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
            default:
                return "не определено";
        }
    }

    private void showNetworkModeGuide(String title, String text, String valueToCopy) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(text + "\n\nЕсли скрытое меню не откроется на вашем телефоне, используйте обычные настройки мобильной сети.")
                .setPositiveButton("Открыть выбор", (dialog, which) -> openRadioInfo())
                .setNegativeButton("Настройки сети", (dialog, which) -> openMobileNetworkSettings())
                .setNeutralButton("Копировать режим", (dialog, which) -> copyText(valueToCopy))
                .show();
    }

    private void openOperatorSelectionGuide() {
        new AlertDialog.Builder(this)
                .setTitle("Выбор оператора вручную")
                .setMessage("Отключите автоматический выбор сети, дождитесь поиска и выберите доступного российского оператора. После успешной регистрации не меняйте сеть без причины: SMS/интернет в роуминге могут снова уйти на проверку.")
                .setPositiveButton("Открыть", (dialog, which) -> openSettingsIntent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showSmsHelp() {
        new AlertDialog.Builder(this)
                .setTitle("SMS с кодом не приходит")
                .setMessage("1. Поставьте режим 4G + 3G.\n" +
                        "2. Включите режим самолёта на 10 секунд.\n" +
                        "3. Выключите режим самолёта и подождите регистрацию в сети.\n" +
                        "4. Если SMS не пришла — выберите оператора вручную.\n" +
                        "5. Если всё равно нет SMS — попробуйте Только 3G.\n" +
                        "6. Не переключайте операторов подряд без необходимости.\n\n" +
                        "Приложение не читает SMS и не может принудительно снять операторскую блокировку. Оно только помогает выбрать более стабильный режим.")
                .setPositiveButton("4G + 3G", (dialog, which) -> showNetworkModeGuide(
                        "4G + 3G авто",
                        "Выберите LTE/WCDMA, LTE/UMTS или похожий режим без 2G и без 5G.",
                        "LTE/WCDMA"
                ))
                .setNegativeButton("Оператор", (dialog, which) -> openOperatorSelectionGuide())
                .setNeutralButton("Только 3G", (dialog, which) -> showNetworkModeGuide(
                        "Только 3G",
                        "Выберите WCDMA only или UMTS only.",
                        "WCDMA only"
                ))
                .show();
    }

    private void showRussiaRoamingGuide() {
        new AlertDialog.Builder(this)
                .setTitle("Роуминг в России")
                .setMessage("Перед поездкой:\n" +
                        "• проверьте, что роуминг подключён у белорусского оператора;\n" +
                        "• сохраните важные номера офлайн;\n" +
                        "• включите push-уведомления банков, если они доступны;\n" +
                        "• основной режим сети — 4G + 3G.\n\n" +
                        "На месте:\n" +
                        "• дождитесь регистрации в российской сети;\n" +
                        "• если пришла SMS от оператора с инструкцией — выполните её;\n" +
                        "• если связь прыгает — выберите оператора вручную.")
                .setPositiveButton("Выбор оператора", (dialog, which) -> openOperatorSelectionGuide())
                .setNegativeButton("Закрыть", null)
                .show();
    }

    private void openRadioInfo() {
        Intent[] intents = new Intent[]{
                new Intent(Intent.ACTION_MAIN).setClassName("com.android.settings", "com.android.settings.RadioInfo"),
                new Intent(Intent.ACTION_MAIN).setClassName("com.android.phone", "com.android.phone.settings.RadioInfo"),
                new Intent(Intent.ACTION_MAIN).setClassName("com.android.settings", "com.android.settings.Settings$RadioInfoActivity"),
                new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)
        };

        for (Intent intent : intents) {
            if (tryStart(intent)) {
                return;
            }
        }

        showTestingCodeDialog();
    }

    private void showTestingCodeDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Скрытое меню не открылось")
                .setMessage("На некоторых телефонах производитель блокирует прямой переход. Откройте звонилку и введите код:\n\n*#*#4636#*#*\n\nЗатем выберите “Информация о телефоне” и нужный тип сети.")
                .setPositiveButton("Открыть звонилку", (dialog, which) -> {
                    Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode("*#*#4636#*#*")));
                    if (!tryStart(dial)) {
                        copyText("*#*#4636#*#*");
                    }
                })
                .setNegativeButton("Скопировать код", (dialog, which) -> copyText("*#*#4636#*#*"))
                .show();
    }

    private void openMobileNetworkSettings() {
        openSettingsIntent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS);
    }

    private void openSettingsIntent(String primaryAction, String fallbackAction) {
        if (tryStart(new Intent(primaryAction))) {
            return;
        }
        if (tryStart(new Intent(fallbackAction))) {
            return;
        }
        tryStart(new Intent(Settings.ACTION_SETTINGS));
    }

    private boolean tryStart(Intent intent) {
        try {
            startActivity(intent);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void copyText(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("network_mode", text));
            Toast.makeText(this, "Скопировано: " + text, Toast.LENGTH_SHORT).show();
        }
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(getResources().getIdentifier("card", "drawable", getPackageName()));
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        return card;
    }

    private void addGridButton(GridLayout grid, String text, boolean primary, Runnable action) {
        Button button = primary ? primaryButton(text) : secondaryButton(text);
        button.setAllCaps(false);
        button.setOnClickListener(v -> action.run());

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dp(54);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        grid.addView(button, params);
    }

    private Button primaryButton(String text) {
        Button button = baseButton(text);
        button.setTextColor(Color.WHITE);
        button.setBackgroundResource(getResources().getIdentifier("button_primary", "drawable", getPackageName()));
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = baseButton(text);
        button.setTextColor(color("#172033"));
        button.setBackgroundResource(getResources().getIdentifier("button_secondary", "drawable", getPackageName()));
        return button;
    }

    private Button baseButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(14);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(48));
        button.setMinWidth(0);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setAllCaps(false);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams topMargin(LinearLayout.LayoutParams params, int dpValue) {
        params.topMargin = dp(dpValue);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int color(String hex) {
        return Color.parseColor(hex);
    }

    private String emptyToDash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "—";
        }
        return value.trim();
    }
}
