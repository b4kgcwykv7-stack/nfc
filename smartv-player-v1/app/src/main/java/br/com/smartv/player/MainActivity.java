package br.com.smartv.player;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import android.graphics.BitmapFactory;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;

import org.json.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final String BASE_URL = "https://nfcnatal.rf.gd/smartv";

    private final Handler ui = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private TextView pairCode, statusText;
    private LinearLayout pairPanel;
    private ImageView image;
    private VideoView video;

    private final List<JSONObject> items = new ArrayList<>();
    private int current = 0;
    private boolean running = true;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("smartv", MODE_PRIVATE);

        pairCode = findViewById(R.id.pairCode);
        statusText = findViewById(R.id.statusText);
        pairPanel = findViewById(R.id.pairPanel);
        image = findViewById(R.id.image);
        video = findViewById(R.id.video);

        ensureInstallKey();
        new Thread(this::registerAndLoop).start();
    }

    private void ensureInstallKey() {
        if (!prefs.contains("install_key")) {
            prefs.edit()
                    .putString("install_key", UUID.randomUUID().toString().replace("-", ""))
                    .apply();
        }
    }

    private String ver() {
        try {
            PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0);
            return p.versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    private void registerAndLoop() {
        while (running) {
            try {
                JSONObject req = new JSONObject();
                req.put("install_key", prefs.getString("install_key", ""));
                req.put("device_name", Build.MANUFACTURER + " " + Build.MODEL);
                req.put("android_version", Build.VERSION.RELEASE);
                req.put("player_version", ver());

                JSONObject r = post(BASE_URL + "/api/register.php", req);
                String key = r.optString("install_key");
                if (!key.isEmpty()) {
                    prefs.edit().putString("install_key", key).apply();
                }

                String code = r.optString("pairing_code", "------");
                boolean paired = r.optBoolean("paired", false);

                ui.post(() -> {
                    pairCode.setText(code);
                    statusText.setText(
                            paired ? "Sincronizando programação..." : "Aguardando vínculo no painel..."
                    );
                });

                sync();
                heartbeat();
                Thread.sleep(60000);
            } catch (Exception e) {
                ui.post(() -> statusText.setText("Sem conexão. Usando programação salva..."));
                playCachedIfNeeded();
                try {
                    Thread.sleep(15000);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void sync() throws Exception {
        String key = prefs.getString("install_key", "");
        JSONObject j = get(
                BASE_URL + "/api/sync.php?install_key="
                        + URLEncoder.encode(key, "UTF-8")
        );

        if (!j.optBoolean("paired", false)) return;

        JSONArray a = j.optJSONArray("items");
        if (a == null) return;

        List<JSONObject> fresh = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            JSONObject it = a.getJSONObject(i);
            File f = mediaFile(it);
            if (!f.exists() || f.length() == 0) {
                download(it.getString("url"), f);
            }
            fresh.add(it);
        }

        synchronized (items) {
            items.clear();
            items.addAll(fresh);
            if (current >= items.size()) current = 0;
        }

        saveCacheJson(a.toString());

        ui.post(() -> {
            pairPanel.setVisibility(View.GONE);
            if (!isPlaying()) playNext();
        });
    }

    private boolean isPlaying() {
        return video.getVisibility() == View.VISIBLE && video.isPlaying();
    }

    private void playNext() {
        JSONObject it;

        synchronized (items) {
            if (items.isEmpty()) {
                pairPanel.setVisibility(View.VISIBLE);
                statusText.setText("Sem conteúdo programado para esta TV.");
                return;
            }

            if (current >= items.size()) current = 0;
            it = items.get(current++);
        }

        try {
            File f = mediaFile(it);
            String type = it.optString("file_type");

            if ("video".equals(type)) {
                image.setVisibility(View.GONE);
                video.setVisibility(View.VISIBLE);
                video.setVideoPath(f.getAbsolutePath());

                video.setOnCompletionListener(mp -> {
                    logPlay(it, true);
                    playNext();
                });

                video.setOnErrorListener((mp, what, extra) -> {
                    logPlay(it, false);
                    ui.postDelayed(this::playNext, 1000);
                    return true;
                });

                video.start();
            } else {
                video.stopPlayback();
                video.setVisibility(View.GONE);
                image.setVisibility(View.VISIBLE);
                image.setImageBitmap(BitmapFactory.decodeFile(f.getAbsolutePath()));

                int duration = Math.max(3, it.optInt("duration_seconds", 10));
                ui.postDelayed(() -> {
                    logPlay(it, true);
                    playNext();
                }, duration * 1000L);
            }
        } catch (Exception e) {
            ui.postDelayed(this::playNext, 1000);
        }
    }

    private void logPlay(JSONObject it, boolean complete) {
        new Thread(() -> {
            try {
                JSONObject log = new JSONObject();
                log.put("media_id", it.optInt("media_id"));
                log.put("campaign_id", it.optInt("campaign_id"));
                log.put(
                        "started_at",
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date())
                );
                log.put("completed", complete);

                JSONArray arr = new JSONArray();
                arr.put(log);

                JSONObject req = new JSONObject();
                req.put("install_key", prefs.getString("install_key", ""));
                req.put("logs", arr);

                post(BASE_URL + "/api/playlog.php", req);
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void heartbeat() {
        try {
            JSONObject h = new JSONObject();
            h.put("install_key", prefs.getString("install_key", ""));
            h.put("player_version", ver());
            h.put("storage_free_mb", getFilesDir().getFreeSpace() / 1048576);
            post(BASE_URL + "/api/heartbeat.php", h);
        } catch (Exception ignored) {
        }
    }

    private File mediaFile(JSONObject it) {
        File dir = new File(getFilesDir(), "media");
        if (!dir.exists()) dir.mkdirs();

        String filename = it.optString("file_name", UUID.randomUUID().toString());
        return new File(dir, filename.replaceAll("[^A-Za-z0-9._-]", "_"));
    }

    private void download(String url, File destination) throws Exception {
        URLConnection c = new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);

        File temp = new File(destination.getAbsolutePath() + ".part");
        try (
                InputStream in = c.getInputStream();
                FileOutputStream out = new FileOutputStream(temp)
        ) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
        }

        if (destination.exists()) destination.delete();
        if (!temp.renameTo(destination)) {
            throw new IOException("Falha ao concluir download");
        }
    }

    private void saveCacheJson(String s) {
        try (FileOutputStream out = openFileOutput("playlist.json", MODE_PRIVATE)) {
            out.write(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private void playCachedIfNeeded() {
        synchronized (items) {
            if (!items.isEmpty()) return;
        }

        try (InputStream cached = openFileInput("playlist.json")) {
            String s = new String(readAll(cached), StandardCharsets.UTF_8);
            JSONArray a = new JSONArray(s);

            synchronized (items) {
                for (int i = 0; i < a.length(); i++) {
                    JSONObject item = a.getJSONObject(i);
                    if (mediaFile(item).exists()) items.add(item);
                }
            }

            ui.post(() -> {
                pairPanel.setVisibility(View.GONE);
                if (!isPlaying()) playNext();
            });
        } catch (Exception ignored) {
        }
    }

    private JSONObject get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);

        try (InputStream in = c.getInputStream()) {
            return new JSONObject(new String(readAll(in), StandardCharsets.UTF_8));
        }
    }

    private JSONObject post(String url, JSONObject data) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestProperty("Content-Type", "application/json");

        try (OutputStream out = c.getOutputStream()) {
            out.write(data.toString().getBytes(StandardCharsets.UTF_8));
        }

        try (InputStream in = c.getInputStream()) {
            return new JSONObject(new String(readAll(in), StandardCharsets.UTF_8));
        }
    }

    private byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;

        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }

        return out.toByteArray();
    }

    @Override
    protected void onDestroy() {
        running = false;
        super.onDestroy();
    }
}
