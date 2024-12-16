package com.example.gostambalevpn.core;

import android.content.Context;
import android.content.Intent;
import android.net.VpnService;

import com.example.gostambalevpn.core.cloudflare.CloudflareVPNManagement;
import com.example.gostambalevpn.utils.VpnStatus;

import java.util.Date;

public class GostambaleVpnService extends VpnService {
    private VPNManagement management = null;
    private Thread mainThread = null;
    private Thread vpnRunning = null;
    private static boolean running = false;
    private static boolean msg;

    public static boolean isRunning() {
        return running;
    }
    public static void startVPN(Context context){
        Intent intent = new Intent(context, GostambaleVpnService.class);
        intent.setAction("START");
        context.startService(intent);
        running = true;

    }
    public static void stopVPN(Context context, boolean msdg){
        msg = msdg;
        Intent intent = new Intent(context, GostambaleVpnService.class);
        intent.setAction("STOP");
        context.startService(intent);
        running = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        VpnStatus.updateStatusChange(this, VpnStatus.VPN_CONNECTING, null);
        if(intent.getAction() == null)return START_NOT_STICKY;
        if(intent.getAction().equalsIgnoreCase("START")){
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
                       Thread.sleep(2000);
                       running = management.running();
                       if(!running){
                           management.vpnStop();
                           mainThread.interrupt();
                           VpnStatus.updateStatusChange(this, VpnStatus.VPN_INTERRUPT, null);
                           break;
                       }
                   } catch (InterruptedException e) {
                       running = false;
                       break;
                   }

               }
            });
            vpnRunning.start();
            return START_STICKY;
        }else if(intent.getAction().equalsIgnoreCase("STOP")){
            if(msg)VpnStatus.updateStatusChange(this, VpnStatus.VPN_DISCONNECTED, null);
            running = false;
            if(vpnRunning != null){
                vpnRunning.interrupt();
            }
            if(management != null){
                management.vpnStop();
                mainThread.interrupt();
            }
            stopSelf();

        }
        return START_NOT_STICKY;
    }

}
