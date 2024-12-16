package com.example.gostambalevpn.utils;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class App {
    public static final int LOGIN_CODE = 1024;
    public static String HOST_NAME = "tls.gostambale.ir";
    //public static String HOST_NAME = "192.168.122.252";
    public static boolean TLS = true;
    public static String device_id;
    private static Thread httpThread = null;
    public static final int START_VPN_PROFILE = 70;
    public static void appInit(Context context){
        device_id = getUniquePseudoID(context);
    }
    public static String getUniquePseudoID(Context context) {
        String m_szDevIDShort = "35" + (Build.BOARD.length() % 10) + (Build.BRAND.length() % 10) + (Build.CPU_ABI.length() % 10) + (Build.DEVICE.length() % 10) + (Build.MANUFACTURER.length() % 10) + (Build.MODEL.length() % 10) + (Build.PRODUCT.length() % 10);
        String serial = null;
        try {
            //serial = Objects.requireNonNull(Build.class.getField("SERIAL").get(null)).toString() + Settings.Secure.getString(context.getContentResolver(),
            //Settings.Secure.ANDROID_ID);
            //return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
            return UUID.nameUUIDFromBytes(Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID).getBytes()).toString();

        } catch (Exception exception) {
            exception.printStackTrace();
            serial = "serial"; // some value
        }
        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    }
    public static void login(int requestCode, @NotNull VpnStatus.HttpCallback callback){
        if(httpThread != null)httpThread.interrupt();
        callback.onBeforeSend(requestCode);
        httpThread = new Thread(()->{
            String u = (App.TLS ? "https" : "http") + "://" + App.HOST_NAME + "/login";
            URL url = null;
            try {
                url = new URL(u);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.addRequestProperty("Host", App.HOST_NAME);
                con.addRequestProperty("Authorization", App.device_id);
                byte[] res = new byte[2048];
                InputStream ip = con.getInputStream();
                JSONObject object = new JSONObject(new String(res, 0, ip.read(res), StandardCharsets.UTF_8));
                callback.onFinish(requestCode, object, 200, null);
            } catch (IOException | JSONException e) {
                try {
                    callback.onFinish(requestCode, null, 400, e.getMessage());
                } catch (JSONException ex) {

                }
            }
        });
        httpThread.start();
    }
    public static void startAnimation(Context ctx, View Element, int animation, boolean show) {
        if (show) {
            Element.setVisibility(View.VISIBLE);
        } else {
            Element.setVisibility(View.INVISIBLE);
        }
        Animation anim = AnimationUtils.loadAnimation(ctx, animation);
        Element.startAnimation(anim);
    }
}
