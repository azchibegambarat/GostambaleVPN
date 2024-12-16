package com.example.gostambalevpn.core.cloudflare;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.companion.WifiDeviceFilter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import com.example.gostambalevpn.MainActivity;
import com.example.gostambalevpn.core.GostambaleVpnService;
import com.example.gostambalevpn.core.VPNManagement;
import com.example.gostambalevpn.utils.App;
import com.example.gostambalevpn.utils.VpnStatus;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CloudflareVPNManagement implements VPNManagement, VpnStatus.HttpCallback {
    private String HOST;
    private long CAPACITY;
    private final GostambaleVpnService vpnService;
    private WebSocketClient websocket;
    private ParcelFileDescriptor iface;
    private PendingIntent mConfigureIntent;
    private FileChannel in_vpn;
    private FileChannel out_vpn;
    private long total_rx,total_tx;
    private boolean running;
    private Thread updateUIThread;
    private Thread sndThread;

    public CloudflareVPNManagement(GostambaleVpnService gostambaleVpnService) {
        this.vpnService = gostambaleVpnService;
        mConfigureIntent = PendingIntent.getActivity(vpnService, 0, new Intent(vpnService, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void vpnStop() {
        running = false;
        if (this.websocket != null) {
            this.websocket.close();
        }
        try {
            if (in_vpn != null) in_vpn.close();
            if (out_vpn != null) out_vpn.close();
            if (iface != null) iface.close();
            if(updateUIThread != null)updateUIThread.interrupt();
            if(sndThread != null)sndThread.interrupt();

        } catch (IOException e) {

        }
    }

    @Override
    public void vpnReconnect() {

    }

    @Override
    public boolean running() {
        return running;
    }

    @Override
    public void run() {
        App.login(App.LOGIN_CODE, this);
    }

    @Override
    public void onFinish(int requestCode, JSONObject object, int status, String error) throws JSONException {
        if(status == 200){
            if(object.getBoolean("expire")){
                VpnStatus.updateStatusChange(vpnService, VpnStatus.VPN_EXPIRE, null);
            }else{
                this.HOST = object.getString("server");
                this.CAPACITY = object.getLong("capacity");
                this.total_rx = object.getLong("rx");
                this.total_tx = object.getLong("tx");
                connectCloudflare();
            }
        }else if(status == 400){
            VpnStatus.updateStatusChange(vpnService, VpnStatus.VPN_ERROR, null);
        }
    }
    private void configure(String ip_address) throws PackageManager.NameNotFoundException {

        VpnService.Builder builder = this.vpnService.new Builder();
        builder.setBlocking(true);
        builder.addAddress(ip_address, 24);
        System.err.println("ip: " + ip_address);
        builder.addDnsServer("4.2.2.4");
        builder.addDnsServer("1.1.1.1");
        builder.addRoute("0.0.0.0", 0);
        builder.addDisallowedApplication(this.vpnService.getPackageName());
//        List<ListModel> apps = ApplicationListActivity.getApplicationList(this.openVPNService);
//        if (apps != null) {
//            apps.forEach(app -> {
//                try {
//                    builder.addDisallowedApplication(app.getPackageName());
//                } catch (PackageManager.NameNotFoundException e) {
//
//                }
//            });
//        }
        builder.setMtu(1480);
        synchronized (this.vpnService) {
            iface = builder
                    .setSession("mServer")
                    .setConfigureIntent(mConfigureIntent)
                    .establish();
        }
        in_vpn = new FileInputStream(iface.getFileDescriptor()).getChannel();
        out_vpn = new FileOutputStream(iface.getFileDescriptor()).getChannel();

    }
    private void connectCloudflare() {
        try{
            VpnStatus.updateStatusChange(vpnService, VpnStatus.VPN_CONNECTING,"درحال اتصال به آسمان شعله‌ور");
            this.websocket = new WebSocketClient(new URI("wss://" + HOST + "/websocket")){
                @Override
                public void onOpen(ServerHandshake handshakedata, String x_ip) {
                    try {
                        VpnStatus.updateStatusChange(vpnService, VpnStatus.VPN_CONNECTING," درحال ثبت آیپی " + x_ip);
                        configure(x_ip);
                        running = true;
                        VpnStatus.updateStatusChange(vpnService, VpnStatus.VPN_CONNECTED, null);
                        updateUIThread = new Thread(() -> {
                            while (running) {
                                try {
                                    Thread.sleep(1000);
                                    VpnStatus.updateBytes(CAPACITY, total_rx, total_tx);
                                } catch (InterruptedException e) {
                                    break;
                                }
                            }
                        });
                        sndThread = new Thread(()->{
                            while (running){
                                ByteBuffer from_tun = ByteBuffer.allocate(Short.MAX_VALUE);
                                while (running) {
                                    from_tun.position(0);
                                    from_tun.clear();
                                    if (Thread.interrupted()) {
                                        return;
                                    }
                                    try {
                                        int length_in = in_vpn.read(from_tun);
                                        if (length_in > 0) {
                                            from_tun.flip();
                                            if(websocket.isOpen()) {
                                                total_tx += length_in;
                                                websocket.send(from_tun);
                                            }
                                        }
                                    } catch (IOException e) {
                                        // e.printStackTrace();
                                        break;
                                    }
                                }
                            }
                        });
                        updateUIThread.start();
                        sndThread.start();
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }

                @Override
                public void onMessage(ByteBuffer bytes) {
                    try {
                        int nread = out_vpn.write(bytes);
                        if(nread > 0){
                            total_rx += nread;
                        }
                    } catch (IOException e) {
                        // toastkKon(e.getMessage());
                    }
                }

                @Override
                public void onMessage(String message) {

                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    running = false;
                }

                @Override
                public void onError(Exception ex) {

                }
            };
            websocket.addHeader("Host", HOST);
            websocket.addHeader("Origin", "https://" + HOST + "/");
            websocket.addHeader("Authorization", App.device_id);;
            websocket.setTcpNoDelay(true);
            websocket.setConnectionLostTimeout(0);

            boolean res = websocket.connectBlocking(30, TimeUnit.SECONDS);
            if(res){
                vpnService.protect(websocket.getSocket());
            }
        }catch (Throwable e){

        }
    }

    @Override
    public void onBeforeSend(int requestCode) {
        VpnStatus.updateStatusChange(vpnService, VpnStatus.VPN_LOGIN, null);
    }
}
