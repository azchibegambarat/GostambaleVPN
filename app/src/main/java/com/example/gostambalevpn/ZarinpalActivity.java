package com.example.gostambalevpn;

import android.graphics.Bitmap;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gostambalevpn.utils.App;
import com.example.gostambalevpn.utils.VpnStatus;
import com.example.gostambalevpn.utils.ZarinpalAuthority;
import com.example.gostambalevpn.utils.ZarinpalPost;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

public class ZarinpalActivity extends AppCompatActivity {

    private String __authority;
    private WebView webView;
    private String __amount;
    private String __capacity;
    private TextView loading;
    private ScrollView pooll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_zarinpal);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        this.webView = findViewById(R.id.webview);
        this.loading = findViewById(R.id.loading);
        this.pooll = findViewById(R.id.pool);

        Button r2gig = findViewById(R.id.r2gig);
        Button r5gig = findViewById(R.id.r5gig);
        Button r7gig = findViewById(R.id.r7gig);
        Button r10gig = findViewById(R.id.r10gig);
        Button r15gig = findViewById(R.id.r15gig);
        Button r20gig = findViewById(R.id.r20gig);
        Button r30gig = findViewById(R.id.r30gig);
        Button r40gig = findViewById(R.id.r40gig);
        Button r50gig = findViewById(R.id.r50gig);
        Button r60gig = findViewById(R.id.r60gig);
        Button r120gig = findViewById(R.id.r120gig);

        r2gig.setOnClickListener(v -> {charge_kon(14, 2);});
        r5gig.setOnClickListener(v -> {charge_kon(35, 5);});
        r7gig.setOnClickListener(v -> {charge_kon(45, 7);});
        r10gig.setOnClickListener(v -> {charge_kon(60, 10);});
        r15gig.setOnClickListener(v -> {charge_kon(87, 15);});
        r20gig.setOnClickListener(v -> {charge_kon(112, 20);});
        r30gig.setOnClickListener(v -> {charge_kon(150, 30);});
        r40gig.setOnClickListener(v -> {charge_kon(180, 40);});
        r50gig.setOnClickListener(v -> {charge_kon(210, 50);});
        r60gig.setOnClickListener(v -> {charge_kon(228, 60);});
        r120gig.setOnClickListener(v -> {charge_kon(250, 120);});
        initWeb();

        //test
//        long ok = 1024L * 1024L * 1024L * 10L;
//        charge_kon("102334", "" + ok);
    }
    private void charge_kon(long amount, long capacity) {
        amount = amount * 10000L;
        capacity = capacity * 1024L * 1024L * 1024L;
        App.startAnimation(this, pooll, R.anim.anim_slide_out_left, false);
        loading.setVisibility(View.VISIBLE);
        __capacity = capacity + "";
        __amount = amount + "";
        new Thread(() -> {
            try {
                URL url = new URL("https://payment.zarinpal.com/pg/v4/payment/request.json");
                URLConnection con = url.openConnection();
                HttpsURLConnection http = (HttpsURLConnection) con;
                http.setRequestMethod("POST"); // PUT is another valid option
                http.setDoOutput(true);
                http.setDoInput(true);
                String json = new ObjectMapper().writeValueAsString(new ZarinpalPost("1471553a-bc89-4a62-8e52-b7b4bdd208b8", Integer.parseInt(__amount), "https://www.zarinpal.com/", __capacity + "," + App.getUniquePseudoID(this)));
                http.setFixedLengthStreamingMode(json.length());
                http.setRequestProperty("Accept", "application/json");
                http.setRequestProperty("Content-Type", "application/json");
                http.connect();
                try (OutputStream os = http.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                byte[] bb = new byte[0xFFFF];
                int ret = http.getInputStream().read(bb);
                if (ret > 0) {
                    String res = new String(bb, 0, ret, StandardCharsets.UTF_8);
                    ZarinpalAuthority authority = new ObjectMapper().readValue(res, ZarinpalAuthority.class);
                    if (authority.getData() != null && authority.getData().getCode() == 100) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            __authority = authority.getData().getAuthority();
                            this.webView.loadUrl("https://payment.zarinpal.com/pg/StartPay/" + authority.getData().getAuthority());
                            this.loading.setVisibility(View.GONE);
                            this.webView.setVisibility(View.VISIBLE);
                        });
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }).start();
    }
    private String charge_id() throws IOException {
        String u = (App.TLS ? "https" : "http") + "://" + App.HOST_NAME + "/charge";
        URL okSir = new URL(u);
        //URL okSir = new URL("http://192.168.122.252/api/charge");

        URLConnection con = okSir.openConnection();
        HttpURLConnection http = (HttpURLConnection) con;
        http.setRequestMethod("POST"); // PUT is another valid option
        http.setDoOutput(true);
        http.setDoInput(true);
        String body = "capacity=" + __capacity + "&amount=" + __amount + "&authority=" + __authority;
        http.setFixedLengthStreamingMode(body.length());
        http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        http.setRequestProperty("Authorization", App.getUniquePseudoID(getApplicationContext()));
        http.setRequestProperty("Host", App.HOST_NAME);
        http.setRequestProperty("Origin", "https://" + App.HOST_NAME + "/");
        http.setRequestProperty("Accept-Encoding", "*");
        http.setRequestProperty("X-CAP", __capacity);
        http.setRequestProperty("X-ANM", __amount);
        http.connect();
        try (OutputStream os = http.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        byte[] bb = new byte[0xFFFF];
        int ret = http.getInputStream().read(bb);
        if (ret > 0) {
            String res = new String(bb, 0, ret, StandardCharsets.UTF_8);
            if (res.length() <= 3) return null;
            return res;
        }
        return null;
    }
    private void initWeb(){
        webView.setWebChromeClient(new WebChromeClient() {

        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                System.err.println(url);
                if (url.contains("www.zarinpal.com")) {
                    runOnUiThread(()->{
                        webView.setVisibility(View.GONE);
                        loading.setText("درحال پردازش...");
                        loading.setVisibility(View.VISIBLE);
                    });
                    Uri uri = Uri.parse(url);
                    String status = uri.getQueryParameter("Status");
                    if (status == null) {
                        return;
                    }
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        new Thread(() -> {
                            if (status.equalsIgnoreCase("ok")) {
                                try {
                                    for (int i = 0;i < 10; i++){
                                        String res = charge_id();
                                        if(res != null){
                                            JSONObject object = new JSONObject(res);
                                            VpnStatus.updateStatusChange(getApplicationContext(), VpnStatus.CH_OK, null);
                                            VpnStatus.updateBytes(object.getLong("capacity"), object.getLong("rx"), object.getLong("tx"));
                                            finish();
                                            return;
                                        }
                                    }
                                } catch (Throwable e) {
                                    VpnStatus.updateStatusChange(getApplicationContext(), VpnStatus.CH_NOT, null);
                                }
                            } else {
                                VpnStatus.updateStatusChange(getApplicationContext(), VpnStatus.CH_NOT, null);
                            }
                            finish();
                        }).start();
                    }, 2000);
                }
            }
        });
        WebSettings settings = webView.getSettings();
        if (Build.VERSION.SDK_INT >= 33) {
            settings.setAlgorithmicDarkeningAllowed(true);
        }
        settings.setSupportZoom(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setJavaScriptEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setUseWideViewPort(true);


        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);


        File databasePath = getDatabasePath("yourDbName");
        settings.setDatabasePath(databasePath.getPath());
        webView.isPrivateBrowsingEnabled();
    }
}