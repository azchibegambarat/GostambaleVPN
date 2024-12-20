package com.example.gostambalevpn.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.VpnService;

import com.example.gostambalevpn.core.GostambaleVpnService;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class  VpnStatus {
    public static final String VPN_CONNECTED = "connected";
    public static final String VPN_DISCONNECTED = "disconnected";
    public static final String VPN_CONNECTING = "connecting";
    public static final String VPN_ERROR = "error";
    public static final String VPN_EXPIRE = "expire";
    public static final String VPN_NOTCONNECTED = "notconnected";
    public static final String VPN_LOGIN = "login";
    public static final String VPN_INTERRUPT = "interrupt";
    private static String VPN_LAST_STATE = VPN_DISCONNECTED;
    private static List<VpnStatusChange> vpnStatusChangeList = new ArrayList<>();
    public synchronized  static void addVpnStatusChange(VpnStatusChange vpnStatusChange){
        vpnStatusChangeList.add(vpnStatusChange);
    }
    public synchronized  static void remove(VpnStatusChange vpnStatusChange) {
        if(!vpnStatusChangeList.isEmpty())
            vpnStatusChangeList.remove(vpnStatusChange);
    }
    public static String getLastStatus(Context context){
        SharedPreferences sp_settings = context.getSharedPreferences("sp_settings", 0);
        String cmd =  sp_settings.getString("cmd", VPN_DISCONNECTED);
        if(cmd.equalsIgnoreCase(VPN_CONNECTED)){
            if(GostambaleVpnService.isRunning())return VPN_CONNECTED;
        }
        if(cmd.equalsIgnoreCase(VPN_LAST_STATE))return VPN_LAST_STATE;
        return VPN_DISCONNECTED;
    }
    public static void updateStatusChange(Context context, String cmd, String message){
        VPN_LAST_STATE = cmd;
        if(!cmd.equalsIgnoreCase(VPN_NOTCONNECTED) && !cmd.equalsIgnoreCase(VPN_LOGIN) && !cmd.equalsIgnoreCase(VPN_INTERRUPT)){
            SharedPreferences sp_settings = context.getSharedPreferences("sp_settings", 0);
            sp_settings.edit().putString("cmd", cmd).apply();
        }
        vpnStatusChangeList.forEach(vpnStatusChange -> {
            vpnStatusChange.statusChange(cmd, message);
        });
    }

    public static void updateBytes(long capacity, long rx, long tx) {
        vpnStatusChangeList.forEach(vpnStatusChange -> {
            vpnStatusChange.updateBytes(capacity, rx, tx);
        });
    }
    public static String humanReadableByteCountSI(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }
    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }



    public static interface VpnStatusChange{
        void statusChange(String cmd, String msg);

        void updateBytes(long capacity, long rx, long tx);
    }
    public static interface HttpCallback{
        void onFinish(int requestCode, JSONObject object, int status, String error) throws JSONException;
        void onBeforeSend(int requestCode);
    }
}
