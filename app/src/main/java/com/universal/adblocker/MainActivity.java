package com.universal.adblocker;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int VPN_REQUEST = 7001;
    private TextView status, blockedCount;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            boolean active = AdBlockVpnService.isRunning();
            status.setText(active ? "PROTECTION ACTIVE" : "PROTECTION OFF");
            status.setTextColor(active ? 0xFF14B8A6 : 0xFFFFC857);
            int count = getSharedPreferences("uab_stats", MODE_PRIVATE).getInt("blocked", 0);
            blockedCount.setText("Blocked DNS requests: " + count);
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(28, 42, 28, 28);
        root.setBackgroundColor(0xFF071629);

        TextView shield = new TextView(this);
        shield.setText("🛡"); shield.setTextSize(64); shield.setGravity(Gravity.CENTER);
        root.addView(shield, new LinearLayout.LayoutParams(-1, -2));

        TextView title = new TextView(this);
        title.setText("UNIVERSAL AD BLOCKER PRO"); title.setTextColor(0xFFFFFFFF);
        title.setTextSize(21); title.setGravity(Gravity.CENTER); title.setPadding(0, 8, 0, 28);
        root.addView(title);

        status = new TextView(this); status.setTextSize(20); status.setGravity(Gravity.CENTER);
        status.setTextColor(0xFFFFC857); root.addView(status, new LinearLayout.LayoutParams(-1, -2));
        blockedCount = new TextView(this); blockedCount.setTextColor(0xFFB7C7DC);
        blockedCount.setTextSize(16); blockedCount.setGravity(Gravity.CENTER); blockedCount.setPadding(0, 14, 0, 24);
        root.addView(blockedCount);

        Button start = new Button(this); start.setText("START PROTECTION");
        start.setOnClickListener(v -> requestVpn()); root.addView(start, new LinearLayout.LayoutParams(-1, -2));
        Button stop = new Button(this); stop.setText("STOP PROTECTION");
        stop.setOnClickListener(v -> stopService(new Intent(this, AdBlockVpnService.class)));
        root.addView(stop, new LinearLayout.LayoutParams(-1, -2));

        TextView info = new TextView(this);
        info.setText("Works as a local DNS filter across apps. It blocks only domains in its filter list. CAPTCHA, first-party ads, YouTube ads and ads served from the same site may still appear. Private DNS / encrypted DNS can bypass DNS filtering.");
        info.setTextColor(0xFFB7C7DC); info.setTextSize(13); info.setPadding(0, 28, 0, 0);
        root.addView(info);
        setContentView(root);
    }

    private void requestVpn() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) startActivityForResult(intent, VPN_REQUEST);
        else startProtection();
    }
    private void startProtection() {
        startForegroundService(new Intent(this, AdBlockVpnService.class));
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == VPN_REQUEST && result == RESULT_OK) startProtection();
    }
    @Override protected void onResume() { super.onResume(); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
}
