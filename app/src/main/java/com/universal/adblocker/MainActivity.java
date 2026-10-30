package com.universal.adblocker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;
    private static final int VPN_REQUEST = 7001;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36, 56, 36, 36);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setBackgroundColor(0xFF081426);

        TextView title = new TextView(this);
        title.setText("🛡  UNIVERSAL AD BLOCKER PRO");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(21);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 28);
        layout.addView(title);

        status = new TextView(this);
        status.setText("Protection is OFF");
        status.setTextColor(0xFFFFC857);
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 18, 0, 24);
        layout.addView(status);

        Button start = new Button(this);
        start.setText("START PROTECTION");
        start.setOnClickListener(v -> requestVpn());
        layout.addView(start, new LinearLayout.LayoutParams(-1, -2));

        Button stop = new Button(this);
        stop.setText("STOP PROTECTION");
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, AdBlockVpnService.class));
            status.setText("Protection stopped");
        });
        layout.addView(stop, new LinearLayout.LayoutParams(-1, -2));

        TextView note = new TextView(this);
        note.setText("\nUses Android's local VPN permission to filter DNS requests across apps. It does not send browsing traffic to a remote VPN server.");
        note.setTextColor(0xFFB7C7DC);
        note.setTextSize(13);
        layout.addView(note);

        setContentView(layout);
    }

    private void requestVpn() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) startActivityForResult(intent, VPN_REQUEST);
        else onActivityResult(VPN_REQUEST, RESULT_OK, null);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == VPN_REQUEST && result == RESULT_OK) {
            startForegroundService(new Intent(this, AdBlockVpnService.class));
            status.setText("Protection starting…");
            status.setTextColor(0xFF50D890);
        } else if (request == VPN_REQUEST) {
            status.setText("VPN permission not granted");
        }
    }
}
