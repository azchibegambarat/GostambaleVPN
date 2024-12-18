package com.example.gostambalevpn.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class HeadsUpNotificationActionReceiver extends BroadcastReceiver {
    private VPNManagement mManagement;
    public HeadsUpNotificationActionReceiver(VPNManagement mManagement) {
        this.mManagement = mManagement;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent != null && intent.getExtras() != null) {
            String action = intent.getStringExtra("CALL_RESPONSE_ACTION_KEY");
            Bundle data = intent.getBundleExtra("FCM_DATA_KEY");

            if (action != null) {
                performClickAction(context, action, data);
            }

            // Close the notification after the click action is performed.

            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(it);
            context.stopService(new Intent(context, HeadsUpNotificationService.class));
        }
    }

    private void performClickAction(Context context, String action, Bundle data) {
        if (action.equals("CALL_RECEIVE_ACTION") && data != null && data.get("type").equals("voip")) {

        } else if (action.equals("CALL_RECEIVE_ACTION") && data != null && data.get("type").equals("video")) {
            mManagement.vpnReconnect();
        } else if (action.equals("CALL_CANCEL_ACTION")) {
            GostambaleVpnService.stopVPNKON(true);
            mManagement.vpnStop();
            context.stopService(new Intent(context, HeadsUpNotificationService.class));
            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(it);
        }
    }
}