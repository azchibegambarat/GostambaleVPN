package com.example.gostambalevpn.core;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK;

import static androidx.core.app.NotificationCompat.PRIORITY_MIN;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.example.gostambalevpn.DisconnectVPN;
import com.example.gostambalevpn.IGostambaleVPNService;
import com.example.gostambalevpn.R;
import com.example.gostambalevpn.core.cloudflare.CloudflareVPNManagement;
import com.example.gostambalevpn.utils.App;
import com.example.gostambalevpn.utils.VpnStatus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class GostambaleVpnService extends VpnService implements IGostambaleVPNService, VpnStatus.VpnStatusChange {
    public static final String START_SERVICE = "com.example.gostambalevpn.core.START_SERVICE";
    public static final String NOTIFICATION_CHANNEL_BG_ID = "gostambalevpn";
    private VPNManagement management = null;
    private Thread mainThread = null;
    private Thread vpnRunning = null;
    private static boolean running = false;
    private static boolean msg;
    private long mConnecttime;
    private String lastChannel;
    private final IBinder mBinder = new IGostambaleVPNService.Stub(){

        @Override
        public boolean protect(int fd) throws RemoteException {
            return GostambaleVpnService.this.protect(fd);
        }

        @Override
        public void userPause(boolean b) throws RemoteException {
            GostambaleVpnService.this.userPause(b);
        }

        @Override
        public boolean stopVPN(boolean replaceConnection) throws RemoteException {
            return GostambaleVpnService.this.stopVPN(replaceConnection);
        }
    };

    public static boolean isRunning() {
        return running;
    }
    public static void startVPN(Context context){
        Intent intent = new Intent(context, GostambaleVpnService.class);
        intent.setAction("START");
        context.startService(intent);
        running = true;

    }
    public static void stopVPNKON(boolean msdg){
        msg = msdg;
        running = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(START_SERVICE))
            return mBinder;
        else
            return super.onBind(intent);
    }
    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mConnecttime = 0;
        stopForeground(true);
        if(intent.getAction() == null)return START_NOT_STICKY;
        if(intent.getAction().equalsIgnoreCase("START")){
            VpnStatus.updateStatusChange(this, VpnStatus.VPN_CONNECTING, null);
            if(vpnRunning != null){
                vpnRunning.interrupt();
            }
            if(management != null){
                management.vpnStop();
                mainThread.interrupt();
            }
            management = new CloudflareVPNManagement(this);
            mainThread = new Thread(management);
            mainThread.start();

            vpnRunning = new Thread(()->{
               while (running){
                   try {
                       Thread.sleep(1000);
                       VPNManagement.VPNConnectionState state = management.running();
                       if(state == VPNManagement.VPNConnectionState.Connected){
                           if(mConnecttime == 0)mConnecttime = System.currentTimeMillis();
                           continue;
                       }
                       if(state == VPNManagement.VPNConnectionState.Exit){
                           running = false;
                           break;
                       }
                       if(state == VPNManagement.VPNConnectionState.RemoteClosed){
                           if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                               startForeground(120, HeadsUpNotificationService.onStartCommand(this));
                           } else {
                               startForeground(120, Objects.requireNonNull(HeadsUpNotificationService.onStartCommand(this)), FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
                           }
                           VpnStatus.updateStatusChange(this, VpnStatus.VPN_INTERRUPT, null);
                           new Thread(()->{
                               try {
                                   Thread.sleep(2000);
                               } catch (InterruptedException e) {

                               }
                               management.vpnStop();
                               mainThread.interrupt();
                           }).start();
                       }
                   } catch (InterruptedException e) {
                       running = false;
                       break;
                   }

               }
            });
            vpnRunning.start();
            VpnStatus.addVpnStatusChange(this);
            return START_STICKY;
        }
        return START_NOT_STICKY;
    }

    @Override
    public void userPause(boolean b) throws RemoteException {

    }

    PendingIntent getGraphPendingIntent() {
        // Let the configure Button show the Log


        Intent intent = new Intent();
        intent.setComponent(new ComponentName(this, "com.example.gostambalevpn.MainActivity"));

        intent.putExtra("PAGE", "graph");
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent startLW = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return startLW;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        VpnStatus.remove(this);
    }

    private void jbNotificationExtras(int priority,
                                      android.app.Notification.Builder nbuilder) {
        try {
            if (priority != 0) {
                Method setpriority = nbuilder.getClass().getMethod("setPriority", int.class);
                setpriority.invoke(nbuilder, priority);

                Method setUsesChronometer = nbuilder.getClass().getMethod("setUsesChronometer", boolean.class);
                setUsesChronometer.invoke(nbuilder, true);

            }

            //ignore exception
        } catch (NoSuchMethodException | IllegalArgumentException |
                 InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }

    }
    private void addVpnActionsToNotification(Notification.Builder nbuilder) {
        Intent disconnectVPN = new Intent(this, DisconnectVPN.class);
        disconnectVPN.setAction("DISCONNECT_VPN");
        PendingIntent disconnectPendingIntent = PendingIntent.getActivity(this, 0, disconnectVPN, PendingIntent.FLAG_IMMUTABLE);

        nbuilder.addAction(R.drawable.ic_stat_vpn_empty_halo, "قطع کن", disconnectPendingIntent);

/*        Intent pauseVPN = new Intent(this, GostambaleVpnService.class);
        if (mDeviceStateReceiver == null || !mDeviceStateReceiver.isUserPaused()) {
            pauseVPN.setAction(PAUSE_VPN);
            PendingIntent pauseVPNPending = PendingIntent.getService(this, 0, pauseVPN, PendingIntent.FLAG_IMMUTABLE);
            nbuilder.addAction(R.drawable.yandex,
                    getString(R.string.pauseVPN), pauseVPNPending);

        } else {
            pauseVPN.setAction(RESUME_VPN);
            PendingIntent resumeVPNPending = PendingIntent.getService(this, 0, pauseVPN, PendingIntent.FLAG_IMMUTABLE);
            nbuilder.addAction(R.drawable.youtube,
                    getString(R.string.resumevpn), resumeVPNPending);
        }*/
    }
    private void lpNotificationExtras(Notification.Builder nbuilder, String category) {
        nbuilder.setCategory(category);
        nbuilder.setLocalOnly(true);

    }
    @SuppressLint("ForegroundServiceType")
    private void showNotification(final String msg, String tickerText, @NonNull String channel, long when, Intent intent) {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        android.app.Notification.Builder nbuilder = new Notification.Builder(this);

        int priority;
        priority = PRIORITY_MIN;


        nbuilder.setContentTitle("GostambaleVPN");

        nbuilder.setContentText(msg);
        nbuilder.setOnlyAlertOnce(true);
        nbuilder.setOngoing(true);

        nbuilder.setSmallIcon(R.drawable.ic_stat_vpn_outline);
        nbuilder.setContentIntent(getGraphPendingIntent());

        if (when != 0)
            nbuilder.setWhen(when);


        // Try to set the priority available since API 16 (Jellybean)
        jbNotificationExtras(priority, nbuilder);
        addVpnActionsToNotification(nbuilder);
        lpNotificationExtras(nbuilder, Notification.CATEGORY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //noinspection NewApi
            nbuilder.setChannelId(channel);
                //noinspection NewApi
                nbuilder.setShortcutId(App.device_id);

        }

        if (tickerText != null && !tickerText.equals(""))
            nbuilder.setTicker(tickerText);

        @SuppressWarnings("deprecation")
        Notification notification = nbuilder.getNotification();

        int notificationId = channel.hashCode();

        mNotificationManager.notify(notificationId, notification);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            startForeground(notificationId, notification);
        } else {
            startForeground(notificationId, notification,
                    FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        }


        if (lastChannel != null && !channel.equals(lastChannel)) {
            // Cancel old notification
            mNotificationManager.cancel(lastChannel.hashCode());
        }


    }

    @Override
    public boolean stopVPN(boolean replaceConnection) throws RemoteException {
        if(msg)VpnStatus.updateStatusChange(this, VpnStatus.VPN_DISCONNECTED, null);
        running = false;
        if(vpnRunning != null){
            vpnRunning.interrupt();
        }
        if(management != null){
            management.vpnStop();
            mainThread.interrupt();
        }
        if (getManagement() != null)
            return getManagement().vpnStop();
        else
            return false;
    }
    public VPNManagement getManagement() {
        return management;
    }
    @Override
    public IBinder asBinder() {
        return mBinder;
    }

    @Override
    public void statusChange(String cmd, String msg) {

    }

    @Override
    public void updateBytes(long capacity, long rx, long tx) {
        long percent = ((rx + tx) * 100L) / capacity;
        String msg = (String.format("%s / %s (%s)" , VpnStatus.humanReadableByteCountBin(rx + tx), VpnStatus.humanReadableByteCountBin(capacity), percent + "%"));
        showNotification(msg, null, NOTIFICATION_CHANNEL_BG_ID, mConnecttime, null);
    }
}
