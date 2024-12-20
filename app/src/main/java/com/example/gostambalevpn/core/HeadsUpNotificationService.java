package com.example.gostambalevpn.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;


import com.example.gostambalevpn.DisconnectVPN;
import com.example.gostambalevpn.R;
import com.example.gostambalevpn.ReconnectVPN;

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
            Intent disconnectVPN = new Intent(context, DisconnectVPN.class);
            disconnectVPN.setAction("DISCONNECT_VPN");
            PendingIntent disconnectPendingIntent = PendingIntent.getActivity(context, 0, disconnectVPN, PendingIntent.FLAG_IMMUTABLE);

            Intent reconnectVPN = new Intent(context, ReconnectVPN.class);
            reconnectVPN.setAction("RECONNECT_VPN");
            PendingIntent reconnectPendingIntent = PendingIntent.getActivity(context, 0, reconnectVPN, PendingIntent.FLAG_IMMUTABLE);



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
                        .setAutoCancel(false)
                        .setOngoing(true)
//                        .setSound(Uri.parse("android.resource://" + getContext().getPackageName() + "/" + R.raw.voip_ringtone))
                        .addAction(R.drawable.ic_stat_vpn_outline, "اتصال دوباره", reconnectPendingIntent)
                        .addAction(R.drawable.ic_stat_vpn_offline, "قطع کن بره", disconnectPendingIntent);
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