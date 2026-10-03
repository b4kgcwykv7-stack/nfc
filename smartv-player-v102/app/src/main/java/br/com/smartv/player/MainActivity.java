package br.com.smartv.player;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import java.net.URLEncoder;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final String PLAYER_URL = "https://nfcnatal.rf.gd/smartv/player.php";
    private WebView web;
    private SharedPreferences prefs;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("smartv", MODE_PRIVATE);
        ensureInstallKey();
        web = findViewById(R.id.web);
        configureWebView();
        if (state == null) loadPlayer();
    }

    private void ensureInstallKey() {
        if (!prefs.contains("install_key")) {
            prefs.edit().putString("install_key", UUID.randomUUID().toString().replace("-", "")).apply();
        }
    }

    private String versionName() {
        try {
            PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0);
            return p.versionName;
        } catch (Exception e) { return "1.0.8"; }
    }

    private void configureWebView() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setAllowContentAccess(true);
        s.setAllowFileAccess(true);
        s.setUserAgentString("Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + "; Android TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36 SmartvPlayer/1.0.8");
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) cm.setAcceptThirdPartyCookies(web, true);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        web.requestFocus();
    }

    private void loadPlayer() {
        try {
            String key = prefs.getString("install_key", "");
            String device = Build.MANUFACTURER + " " + Build.MODEL;
            String url = PLAYER_URL + "?install_key=" + enc(key)
                    + "&device_name=" + enc(device)
                    + "&android_version=" + enc(Build.VERSION.RELEASE)
                    + "&player_version=" + enc(versionName());
            web.loadUrl(url);
        } catch (Exception e) { web.loadUrl(PLAYER_URL); }
    }

    private String enc(String s) {
        try { return URLEncoder.encode(s == null ? "" : s, "UTF-8"); }
        catch (Exception e) { return ""; }
    }

    @Override public void onBackPressed() { finish(); }

    @Override protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
    }

    @Override protected void onPause() {
        if (web != null) web.onPause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (web != null) { web.stopLoading(); web.destroy(); }
        super.onDestroy();
    }
}
