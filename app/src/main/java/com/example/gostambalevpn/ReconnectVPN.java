package com.example.gostambalevpn;

import static com.example.gostambalevpn.core.GostambaleVpnService.NOTIFICATION_CHANNEL_BG_ID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.gostambalevpn.core.GostambaleVpnService;

import org.w3c.dom.Text;

public class ReconnectVPN extends Activity{
    private IGostambaleVPNService mService;
    private CountDownTimer ConnectionTimer;
    private int retry = 0;
    private final ServiceConnection mConnection = new ServiceConnection() {



        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            mService = IGostambaleVPNService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
        }

    };
    private TextView reconnect_txt;

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, GostambaleVpnService.class);
        intent.setAction(GostambaleVpnService.START_SERVICE);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        retry = 0;
        if(mService != null){
            try {
                mService.removeNotification();
            } catch (RemoteException e) {

            }
        }
        if(ConnectionTimer != null)ConnectionTimer.cancel();
        ConnectionTimer = new CountDownTimer(70_000, 17000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if(GostambaleVpnService.isRunning()){
                    ConnectionTimer.cancel();
                    finish();
                }else{
                    retry++;
                    new Handler(Looper.getMainLooper()).post(()->{
                        if(reconnect_txt != null)reconnect_txt.setText(String.format("درحال اتصال...(%s)", retry));
                    });
                    stop_vpn(false);
                    start_vpn();
                }
            }

            @Override
            public void onFinish() {
                stop_vpn(true);
                finish();
            }
        };
        new Handler(Looper.getMainLooper()).post(()->{
            if(reconnect_txt != null)reconnect_txt.setText(String.format("درحال اتصال...(%s)", retry));
        });
        ConnectionTimer.start();

    }
    private void start_vpn(){
        GostambaleVpnService.startVPN(this);
    }
    private void stop_vpn(boolean msg) {
        GostambaleVpnService.stopVPNKON(msg);
        if(mService != null){
            try {
                mService.stopVPN(true);
            } catch (RemoteException e) {

            }
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        ConnectionTimer.cancel();
        unbindService(mConnection);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reconnect);
        Button disconnect_kon = findViewById(R.id.disconnect_kon);
        reconnect_txt = findViewById(R.id.reconnect_txt);
        disconnect_kon.setOnClickListener(v -> {
            ConnectionTimer.cancel();
            stop_vpn(false);
            finish();
        });
    }
}


