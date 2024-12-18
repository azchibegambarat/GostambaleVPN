package com.example.gostambalevpn.core;

import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.IBinder;
import android.os.RemoteException;

import com.example.gostambalevpn.IGostambaleVPNService;
import com.example.gostambalevpn.core.cloudflare.CloudflareVPNManagement;
import com.example.gostambalevpn.utils.VpnStatus;

public class GostambaleVpnService extends VpnService implements IGostambaleVPNService {
    public static final String START_SERVICE = "com.example.gostambalevpn.core.START_SERVICE";
    private VPNManagement management = null;
    private Thread mainThread = null;
    private Thread vpnRunning = null;
    private static boolean running = false;
    private static boolean msg;
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
        }
        return START_NOT_STICKY;
    }

    @Override
    public void userPause(boolean b) throws RemoteException {

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
}
