package mcc.mcc18.Notifications;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

import mcc.mcc18.R;

public class MessageService extends FirebaseMessagingService {
    private static final String TAG = "FCM Service";
    public static String activeChat = null;
    public static Activity coiso = null;
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated.
        super.onMessageReceived(remoteMessage);

        System.out.println("From: " + remoteMessage.getFrom());
        System.out.println("Notification Message Body: " + remoteMessage.getNotification().getBody());

        showNotification(remoteMessage.getNotification());
    }

    private void showNotification(final RemoteMessage.Notification notification){
        new Thread() {
            public void run() {
                if(coiso == null) {
                    System.out.println("Pop up not available here");
                    return;
                }
                coiso.runOnUiThread(new Runnable() {
                    public void run() {
                        if(activeChat == null || notification.getTitle().indexOf(activeChat) == -1){
                            String text = notification.getTitle() + '\n' + notification.getBody();
                            Toast.makeText(getApplicationContext(),text,Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }.start();
    }

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
    }
}