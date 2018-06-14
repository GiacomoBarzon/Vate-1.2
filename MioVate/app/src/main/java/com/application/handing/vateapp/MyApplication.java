package com.application.handing.vateapp;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

//Base class for those who need to maintain global application state
//tutto ciò che è qui dentro dovrebbe essere globale per l'applicazione
public class MyApplication extends Application {
    static int cols = 201; // piani
    static int rows=7; // numero beacons
    public final static String[] primaRiga = new String[cols];
    public final static String[] secondaRiga = new String[cols];

    @Override
    public void onCreate() {
        super.onCreate();

        //DEFINIZIONE NOME ZONA BEACON
        primaRiga[1] = "Sei al piano terra";
        secondaRiga[1] = "Qui puoi trovare la cucina e il soggiorno.";
        primaRiga[100] = "Sei sul pianerottolo";
        secondaRiga[100] = "Puoi salire al primo piano o scendere al piano terra.";
        primaRiga[200] = "Sei al primo piano";
        secondaRiga[200] = "Qui puoi trovare le camere per dormire.";
        primaRiga[150] = "Sei al bar Al Todaro";
        secondaRiga[150] = "Qui puoi trovare la parte bar e la gelateria.";
        //GESTIONE MONITORING IN BACKGROUND
    }

    //GESTIONE NOTIFICHE
    //notification to show up whenever user enters the range of our monitored beacon
    public void showNotification(String title, String message) {
        Intent notifyIntent = new Intent(this, MainActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivities(this, 0,
                new Intent[]{notifyIntent}, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();
        notification.defaults |= Notification.DEFAULT_SOUND;
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }
    public void cancelNotification() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1);
    }
    //END GESTIONE NOTIFICHE
}
