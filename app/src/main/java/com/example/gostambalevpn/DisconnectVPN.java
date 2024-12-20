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
import android.os.IBinder;
import android.os.RemoteException;

import androidx.core.app.NotificationManagerCompat;

import com.example.gostambalevpn.core.GostambaleVpnService;


/**
 * Created by arne on 13.10.13.
 */
public class DisconnectVPN extends Activity implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
    private IGostambaleVPNService mService;
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

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, GostambaleVpnService.class);
        intent.setAction(GostambaleVpnService.START_SERVICE);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        showDisconnectDialog();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);
    }

    private void showDisconnectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("بیخیال");
        builder.setMessage("آیا میخواهید مرا تمام کنید؟ تموم بشه؟");
        builder.setNegativeButton("بیخیال", this);
        builder.setPositiveButton("قطع کن", this);
     //  builder.setNeutralButton("دوباره وصل شوید", this);
        builder.setOnCancelListener(this);

        builder.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (mService != null) {
                try {
                    GostambaleVpnService.stopVPNKON(true);
                    mService.stopVPN(false);
                    NotificationManager nMgr = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE);
                    nMgr.cancel(NOTIFICATION_CHANNEL_BG_ID.hashCode());
                    nMgr.cancelAll();
                } catch (RemoteException e) {

                }
            }
        } else if (which == DialogInterface.BUTTON_NEUTRAL) {
            GostambaleVpnService.startVPN(this);
        }
        finish();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }
}
