package com.example.gostambalevpn;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gostambalevpn.core.GostambaleVpnService;
import com.example.gostambalevpn.utils.App;
import com.example.gostambalevpn.utils.MessageColor;
import com.example.gostambalevpn.utils.VpnStatus;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ArrayBlockingQueue;

public class MainActivity extends AppCompatActivity implements VpnStatus.HttpCallback, VpnStatus.VpnStatusChange {

    private View ll_message;
    private View ll_main;
    private TextView message_txt;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ArrayBlockingQueue<MessageColor> messages = new ArrayBlockingQueue<>(1000);
    private Thread uiThread;
    private Button btn_connect;
    private Button btn_disconnect;
    private TextView emoji_txt;
    private TextView usage_txt;
    private ProgressBar progressBar;


    @Override
    protected void onResume() {
        VpnStatus.updateStatusChange(this, VpnStatus.getLastStatus(this), null);
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
        ll_message = findViewById(R.id.ll_message);
        ll_main = findViewById(R.id.ll_main);
        message_txt = findViewById(R.id.message_txt);
        btn_connect = findViewById(R.id.btn_connect);
        btn_disconnect = findViewById(R.id.btn_disconnect);
        emoji_txt = findViewById(R.id.emoji_txt);
        usage_txt = findViewById(R.id.usage_txt);
        progressBar = findViewById(R.id.progressBar);

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

        VpnStatus.addVpnStatusChange(this);
        App.appInit(this);
        App.startAnimation(this, findViewById(R.id.GUID), R.anim.anim_slide_down, true);
        App.login(App.LOGIN_CODE, this);

        btn_connect.setOnClickListener(v -> {
            GostambaleVpnService.startVPN(this);
        });
        btn_disconnect.setOnClickListener(v -> {
            GostambaleVpnService.stopVPN(this, true);
        });

        uiThread = new Thread(() -> {

            while (!Thread.interrupted()) {
                try {
                    MessageColor messageColor = messages.take();
                    handler.post(()->{
                        App.startAnimation(this, ll_message, R.anim.slide_down_800, true);
                        message_txt.setTextColor(messageColor.getColor());
                        message_txt.setText(messageColor.getMessage());
                        App.startAnimation(this, ll_message, R.anim.slide_up_800, true);
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        uiThread.start();
    }

    @Override
    public void onFinish(int requestCode, JSONObject object, int status, String error) throws JSONException {
        if(requestCode == App.LOGIN_CODE){
            if(status == 200){
                handler.post(()->{
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
    private void showMessage(String message, @ColorInt int color){
        messages.offer(new MessageColor(message, color));
    }

    @Override
    public void statusChange(String cmd, String kms) {
        handler.post(()->{
            String msg = kms;
            switch (cmd) {
                case VpnStatus.VPN_DISCONNECTED:
                    if (msg == null)
                        msg = "آماده به کار";
                    showMessage(msg, Color.WHITE);
                    btn_disconnect.setVisibility(View.GONE);
                    btn_connect.setVisibility(View.VISIBLE);
                    emoji_txt.setText("\uD83D\uDE21");
                    break;
                case VpnStatus.VPN_CONNECTED:
                    if (msg == null)
                        msg = "متصل شدید!";
                    showMessage(msg, Color.WHITE);
                    btn_disconnect.setVisibility(View.VISIBLE);
                    btn_connect.setVisibility(View.GONE);
                    emoji_txt.setText("\uD83D\uDE03");
                    break;
                case VpnStatus.VPN_CONNECTING:
                    if (msg == null)
                        msg = "درحال اتصال...";
                    btn_disconnect.setVisibility(View.GONE);
                    btn_connect.setVisibility(View.GONE);
                    showMessage(msg, Color.WHITE);
                    emoji_txt.setText("\uD83E\uDDD0");
                    break;
                case VpnStatus.VPN_INTERRUPT:
                    if (msg == null)
                        msg = "ایران قطع کرد VPN رو!!!";
                    showMessage(msg, Color.WHITE);
                    emoji_txt.setText("\uD83E\uDDD0");
                    GostambaleVpnService.stopVPN(this, false);
                    break;
                case VpnStatus.VPN_LOGIN:
                    if (msg == null)
                        msg = "درحال احراز هویت...";
                    btn_disconnect.setVisibility(View.GONE);
                    btn_connect.setVisibility(View.GONE);
                    showMessage(msg, Color.WHITE);
                    emoji_txt.setText("\uD83D\uDE37");
                    break;
                case VpnStatus.VPN_ERROR:

                    break;
                case VpnStatus.VPN_EXPIRE:
                    if (msg == null)
                        msg = "حجم بسته انیترنتی شما به پایان رسیده";
                    btn_disconnect.setVisibility(View.GONE);
                    btn_connect.setVisibility(View.GONE);
                    showMessage(msg, Color.RED);
                    emoji_txt.setText("\uD83E\uDD75");
                    break;
                case VpnStatus.VPN_NOTCONNECTED:
                    if (msg == null)
                        msg = "نشد که وصل بشه!";
                    btn_disconnect.setVisibility(View.GONE);
                    btn_connect.setVisibility(View.VISIBLE);
                    showMessage(msg, Color.RED);
                    emoji_txt.setText("\uD83E\uDD74");
                    break;

            }
        });
    }

    @Override
    public void updateBytes(long capacity, long rx, long tx) {
        handler.post(()->{
            long percent = ((rx + tx) * 100L) / capacity;
            usage_txt.setText(String.format("%s / %s (%s)" , VpnStatus.humanReadableByteCountBin(rx + tx), VpnStatus.humanReadableByteCountBin(capacity), percent + "%"));
            progressBar.setProgress((int) percent);
        });
    }
}