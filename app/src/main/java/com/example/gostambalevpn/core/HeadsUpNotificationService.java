package com.example.gostambalevpn.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;


import com.example.gostambalevpn.R;

import java.util.Objects;

public class HeadsUpNotificationService {
    private static String CHANNEL_ID = "VoipChannel";
    private  static String CHANNEL_NAME = "Voip Channel";


    public static Notification onStartCommand(Context context) {
//        Bundle data = null;
//        if (intent != null && intent.getExtras() != null) {
//            data = intent.getBundleExtra(ConstantApp.FCM_DATA_KEY);
//        }
        try {
            Intent receiveCallAction = new Intent(context, HeadsUpNotificationActionReceiver.class);
            receiveCallAction.putExtra("CALL_RESPONSE_ACTION_KEY", "CALL_RECEIVE_ACTION");
            //receiveCallAc‍tion.putExtra(ConstantApp.FCM_DATA_KEY, data);
            receiveCallAction.setAction("RECEIVE_CALL");

            Intent cancelCallAction = new Intent(context, HeadsUpNotificationActionReceiver.class);
            cancelCallAction.putExtra("CALL_RESPONSE_ACTION_KEY", "CALL_CANCEL_ACTION");
            //cancelCallAction.putExtra(ConstantApp.FCM_DATA_KEY, data);
            cancelCallAction.setAction("CANCEL_CALL");

            PendingIntent receiveCallPendingIntent = PendingIntent.getBroadcast(context, 1200, receiveCallAction, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            PendingIntent cancelCallPendingIntent = PendingIntent.getBroadcast(context, 1201, cancelCallAction, PendingIntent.FLAG_UPDATE_CURRENT| PendingIntent.FLAG_IMMUTABLE);

            createChannel(context);
            NotificationCompat.Builder notificationBuilder = null;
            //if (data != null) {
                notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setContentTitle("VPN قطع شد")
                        .setContentText("ایران اتصال VPN شما را قطع کرد.")
                        .setSmallIcon(R.drawable.ic_stat_vpn_empty_halo)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
//                        .addAction(R.drawable.ic_stat_vpn_offline, "Receive Call", receiveCallPendingIntent)
//                        .addAction(R.drawable.ic_stat_vpn_outline, "Cancel call", cancelCallPendingIntent)
                        .setAutoCancel(true)
//                        .setSound(Uri.parse("android.resource://" + getContext().getPackageName() + "/" + R.raw.voip_ringtone))
                        .setFullScreenIntent(receiveCallPendingIntent, true);
            //}

            Notification incomingCallNotification = null;
            if (notificationBuilder != null) {
                incomingCallNotification = notificationBuilder.build();
            }
            return  incomingCallNotification;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    /*
    Create noticiation channel if OS version is greater than or eqaul to Oreo
    */
    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Call Notifications");
            Objects.requireNonNull(context.getSystemService(NotificationManager.class)).createNotificationChannel(channel);
        }
    }
}