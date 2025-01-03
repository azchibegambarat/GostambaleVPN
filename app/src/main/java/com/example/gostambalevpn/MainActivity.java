package com.example.gostambalevpn;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gostambalevpn.core.GostambaleVpnService;
import com.example.gostambalevpn.core.HeadsUpNotificationService;
import com.example.gostambalevpn.utils.App;
import com.example.gostambalevpn.utils.MessageColor;
import com.example.gostambalevpn.utils.VpnStatus;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;

public class MainActivity extends AppCompatActivity implements VpnStatus.HttpCallback, VpnStatus.VpnStatusChange {

    private View ll_message;
    private View ll_main;
    private TextView message_txt;
    private ArrayBlockingQueue<MessageColor> messages = new ArrayBlockingQueue<>(1000);
    //private Thread uiThread;
    private Button btn_connect;
    private Button btn_disconnect;
    private Button btn_apps;
    private Button btn_charge;
    private TextView device_id;
    private TextView emoji_txt;
    private CheckBox reconnect_auto;
    private TextView usage_txt;
    private ProgressBar progressBar;
    private CountDownTimer ConnectionTimer;
    private IGostambaleVPNService mService;
    private Thread uiThread;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = IGostambaleVPNService.Stub.asInterface(service);

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
        }
    };



    @Override
    protected void onDestroy() {
        super.onDestroy();
        VpnStatus.remove(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);
    }

    @Override
    protected void onResume() {
        VpnStatus.updateStatusChange(this, VpnStatus.getLastStatus(this), null);
        Intent intent = new Intent(this, GostambaleVpnService.class);
        intent.setAction(GostambaleVpnService.START_SERVICE);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        SharedPreferences s = getSharedPreferences("khar", 0);
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{POST_NOTIFICATIONS}, 666);
            } else {
                //Toast.makeText(this, "مجوز قبلا داده شده است", Toast.LENGTH_SHORT).show();

            }
        }
        ll_message = findViewById(R.id.ll_message);
        ll_main = findViewById(R.id.ll_main);
        message_txt = findViewById(R.id.message_txt);
        btn_connect = findViewById(R.id.btn_connect);
        btn_disconnect = findViewById(R.id.btn_disconnect);
        btn_charge = findViewById(R.id.btn_charge);
        btn_apps = findViewById(R.id.btn_apps);
        emoji_txt = findViewById(R.id.emoji_txt);
        usage_txt = findViewById(R.id.usage_txt);
        device_id = findViewById(R.id.device_id);
        progressBar = findViewById(R.id.progressBar);
        reconnect_auto = findViewById(R.id.reconnect_auto);


        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            // Start the query
            try {
                startActivityForResult(intent, App.START_VPN_PROFILE);
            } catch (ActivityNotFoundException ane) {
                // Shame on you Sony! At least one user reported that
                // an official Sony Xperia Arc S image triggers this exception
            }
        } else {
            onActivityResult(App.START_VPN_PROFILE, Activity.RESULT_OK, null);
        }

        createNotificationChannels();
        VpnStatus.addVpnStatusChange(this);
        App.appInit(this);
        this.device_id.setText(App.device_id.substring(0, 10));
        App.startAnimation(this, findViewById(R.id.GUID), R.anim.anim_slide_down, true);
        App.login(App.LOGIN_CODE, this);
        reconnect_auto.setChecked(s.getBoolean("reconnect_auto", false));
        btn_connect.setOnClickListener(v ->  {
            if(ConnectionTimer != null)ConnectionTimer.cancel();
            ConnectionTimer = new CountDownTimer(17_000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    if(GostambaleVpnService.isRunning())ConnectionTimer.cancel();
                }

                @Override
                public void onFinish() {
                    stop_vpn(true);
                }
            };
            ConnectionTimer.start();
            GostambaleVpnService.startVPN(this);
        });

        btn_disconnect.setOnClickListener(v -> {
            //GostambaleVpnService.stopVPNKON(this, true);
            if(ConnectionTimer != null )ConnectionTimer.cancel();
            stop_vpn(true);
        });
        btn_charge.setOnClickListener(v->{
            if(ConnectionTimer != null )ConnectionTimer.cancel();
            stop_vpn(true);
            startActivity(new Intent(this, ZarinpalActivity.class));
        });
        btn_apps.setOnClickListener(v -> {
            if(ConnectionTimer != null )ConnectionTimer.cancel();
            stop_vpn(true);
            startActivity(new Intent(this, ApplicationListActivity.class));
        });
        reconnect_auto.setOnClickListener(v -> {
            s.edit().putBoolean("reconnect_auto", reconnect_auto.isChecked()).apply();
        });
        if(uiThread != null)uiThread.interrupt();
        uiThread = new Thread(() -> {
            Handler handler = new Handler(Looper.getMainLooper());
            while (!Thread.interrupted()) {
                try {
                    MessageColor messageColor = messages.take();
                    handler.post(()->{
                        App.startAnimation(this, ll_message, R.anim.slide_down_800, true);
                        message_txt.setTextColor(messageColor.getColor());
                        message_txt.setText(messageColor.getMessage());
                        App.startAnimation(this, ll_message, R.anim.slide_up_800, true);
                    });
                    //Thread.sleep(1500);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        uiThread.start();
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
    public void onFinish(int requestCode, JSONObject object, int status, String error) throws JSONException {
        if(requestCode == App.LOGIN_CODE){
            if(status == 200){
                //handler.post(()->{
                runOnUiThread(()->{
                    App.startAnimation(this, ll_main, R.anim.fade_in_1000, true);
                });
                //VpnStatus.updateStatusChange(this, VpnStatus.VPN_DISCONNECTED, "اطلاعات دریافت شد!");
                if(object.getBoolean("expire")){
                    VpnStatus.updateStatusChange(this, VpnStatus.VPN_EXPIRE, "حجم بسته انیترنتی شما به پایان رسیده");
                }
                VpnStatus.updateBytes(object.getLong("capacity"), object.getLong("rx"), object.getLong("tx"));
            }else{
                VpnStatus.updateStatusChange(this, VpnStatus.VPN_DISCONNECTED, error);
            }
        }
    }

    @Override
    public void onBeforeSend(int requestCode) {
        if(requestCode == App.LOGIN_CODE){
            //showMessage("درحال دریافت اطلاعات");
        }

    }
    private void showMessage(String message, @ColorInt int color) {
        messages.offer(new MessageColor(message, color));
        //handler.postDelayed(() -> {
//        runOnUiThread(()->{
//            App.startAnimation(this, ll_message, R.anim.slide_down_800, true);
//            message_txt.setTextColor(color);
//            message_txt.setText(message);
//            App.startAnimation(this, ll_message, R.anim.slide_up_800, true);
//        });
    }

    @Override
    public void statusChange(String cmd, String kms) {
        //handler.post(()->{
        runOnUiThread(()->{
            String msg = kms;
            switch (cmd) {
                case VpnStatus.VPN_DISCONNECTED:
                    if (msg == null)
                        msg = "آماده به کار";
                    showMessage(msg, Color.WHITE);
                    btn_disconnect.setVisibility(View.GONE);
                    btn_connect.setVisibility(View.VISIBLE);
                    emoji_txt.setText("\uD83D\uDE21");
                    App.startAnimation(this, emoji_txt, R.anim.amin_up_down, true);
                    break;
                case VpnStatus.VPN_CONNECTED:
                    if (msg == null)
                        msg = "متصل شدید!";
                    showMessage(msg, Color.WHITE);
                    btn_disconnect.setVisibility(View.VISIBLE);
                    btn_connect.setVisibility(View.GONE);
                    emoji_txt.setText("\uD83D\uDE03");
                    //App.startAnimation(this, emoji_txt, R.anim.amin_heart, true);
                    //App.startAnimation(this, emoji_txt, R.anim.amin_up_down, true);
                    break;
                case VpnStatus.VPN_CONNECTING:
                    if (msg == null)
                        msg = "درحال اتصال...";
                    btn_disconnect.setVisibility(View.VISIBLE);
                    btn_connect.setVisibility(View.GONE);
                    showMessage(msg, Color.WHITE);
                    emoji_txt.setText("\uD83E\uDDD0");
                    //App.startAnimation(this, emoji_txt, R.anim.anim_slide_in_right, true);
                    //App.startAnimation(this, emoji_txt, R.anim.anim_slide_in_right, true);
                    App.startAnimation(this, emoji_txt, R.anim.fade_in_100, true);
                    break;
                case VpnStatus.VPN_INTERRUPT:
                    if (msg == null)
                        msg = "ایران قطع کرد VPN رو!!!";
                    showMessage(msg, Color.WHITE);
                    emoji_txt.setText("\uD83E\uDDD0");
                    stop_vpn(false);
                    btn_disconnect.setVisibility(View.GONE);
                    btn_connect.setVisibility(View.VISIBLE);
                    App.startAnimation(this, emoji_txt, R.anim.fade_in_100, true);
                    break;
                case VpnStatus.VPN_LOGIN:
                    if (msg == null)
                        msg = "درحال احراز هویت...";
                    btn_disconnect.setVisibility(View.GONE);
                    btn_connect.setVisibility(View.GONE);
                    showMessage(msg, Color.WHITE);
                    emoji_txt.setText("\uD83D\uDE37");
                    App.startAnimation(this, emoji_txt, R.anim.fade_in_100, true);
                    break;
                case VpnStatus.VPN_ERROR:

                    break;
                case VpnStatus.VPN_EXPIRE:
                    if (msg == null)
                        msg = "حجم بسته انیترنتی شما به پایان رسیده";
                    btn_disconnect.setVisibility(View.GONE);
                    btn_connect.setVisibility(View.GONE);
                    showMessage(msg, Color.parseColor("#efcbe3"));
                    emoji_txt.setText("\uD83E\uDD75");
                    App.startAnimation(this, emoji_txt, R.anim.anim_slide_down, true);
                    new Handler(Looper.getMainLooper()).postDelayed(()->{
                        App.startAnimation(this, emoji_txt, R.anim.amin_shake, true);
                    }, 900);
                    //
                    break;
                case VpnStatus.VPN_NOTCONNECTED:
                    if (msg == null)
                        msg = "نشد که وصل بشه!";
                    btn_disconnect.setVisibility(View.GONE);
                    btn_connect.setVisibility(View.VISIBLE);
                    showMessage(msg, Color.RED);
                    emoji_txt.setText("\uD83E\uDD74");
                    App.startAnimation(this, emoji_txt, R.anim.fade_in_100, true);
                    break;
                case VpnStatus.CH_OK:
                    showMessage("باموفقیت شارژ شده! آماده به کار.", Color.WHITE);
                    btn_disconnect.setVisibility(View.GONE);
                    btn_connect.setVisibility(View.VISIBLE);
                    emoji_txt.setText("\uD83E\uDD29");
                    App.startAnimation(this, emoji_txt, R.anim.amin_up_down, true);
                    break;
                case VpnStatus.CH_NOT:
                    showMessage(" ببخشید شارژ نشد! به آیدی gostambale@ پیام بدین.همه جا هستم.", Color.parseColor("#FFB200"));
                    btn_disconnect.setVisibility(View.GONE);
                    btn_connect.setVisibility(View.VISIBLE);
                    emoji_txt.setText("\uD83E\uDD15");
                    App.startAnimation(this, emoji_txt, R.anim.amin_up_down, true);
                    break;
            }

        });
    }
    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannels() {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Background message
        CharSequence name = "آمار اتصال";
        NotificationChannel mChannel = new NotificationChannel(GostambaleVpnService.NOTIFICATION_CHANNEL_BG_ID,
                name, NotificationManager.IMPORTANCE_MIN);

        mChannel.setDescription("آمار در حال انجام اتصال GostambaleVPN ایجاد شده");
        mChannel.enableLights(false);

        mChannel.setLightColor(Color.DKGRAY);
        mNotificationManager.createNotificationChannel(mChannel);

    }
    @Override
    public void updateBytes(long capacity, long rx, long tx) {
        //handler.post(()->{
        runOnUiThread(()->{
            long percent = ((rx + tx) * 100L) / capacity;
            usage_txt.setText(String.format("%s / %s (%s)" , VpnStatus.humanReadableByteCountBin(rx + tx), VpnStatus.humanReadableByteCountBin(capacity), percent + "%"));
            progressBar.setProgress((int) percent);
        });
    }
}