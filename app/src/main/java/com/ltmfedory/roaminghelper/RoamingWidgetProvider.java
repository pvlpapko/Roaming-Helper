package com.ltmfedory.roaminghelper;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

public class RoamingWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.roaming_widget);

            views.setOnClickPendingIntent(R.id.widget_mode_4g3g,
                    openApp(context, MainActivity.ACTION_MODE_4G_3G, null, 1001));
            views.setOnClickPendingIntent(R.id.widget_sms,
                    openApp(context, MainActivity.ACTION_SMS_HELP, null, 1002));
            views.setOnClickPendingIntent(R.id.widget_operator_mts,
                    openApp(context, MainActivity.ACTION_SELECT_OPERATOR, "МТС Россия", 1101));
            views.setOnClickPendingIntent(R.id.widget_operator_beeline,
                    openApp(context, MainActivity.ACTION_SELECT_OPERATOR, "Билайн", 1102));
            views.setOnClickPendingIntent(R.id.widget_operator_megafon,
                    openApp(context, MainActivity.ACTION_SELECT_OPERATOR, "МегаФон", 1103));
            views.setOnClickPendingIntent(R.id.widget_operator_t2,
                    openApp(context, MainActivity.ACTION_SELECT_OPERATOR, "T2 / Tele2", 1104));

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private PendingIntent openApp(Context context, String action, String operatorName, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(action);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (operatorName != null) {
            intent.putExtra(MainActivity.EXTRA_OPERATOR_NAME, operatorName);
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, requestCode, intent, flags);
    }
}
