package com.universal.adblocker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class AdBlockVpnService extends VpnService {
    private static final String CHANNEL = "uab_pro";
    private static final String DNS_IP = "10.111.0.1";
    private static final String UPSTREAM_DNS = "1.1.1.1";
    private volatile boolean running = false;
    private static volatile boolean serviceRunning = false;
    private int blockedCount = 0;
    public static boolean isRunning() { return serviceRunning; }
    private ParcelFileDescriptor tun;
    private Thread worker;

    private final Set<String> blocked = new HashSet<>(Arrays.asList(
        "doubleclick.net","googlesyndication.com","googleadservices.com",
        "adservice.google.com","adnxs.com","adform.net","adsrvr.org",
        "taboola.com","outbrain.com","criteo.com","rubiconproject.com",
        "pubmatic.com","openx.net","casalemedia.com","zedo.com","adroll.com",
        "popads.net","popcash.net","propellerads.com","exoclick.com",
        "trafficjunky.net","mgid.com","revcontent.com","bidvertiser.com",
        "smartadserver.com", "googletagmanager.com", "googletagservices.com",
        "pagead2.googlesyndication.com", "securepubads.g.doubleclick.net",
        "adservice.google.co.in", "ads.google.com", "ads.youtube.com",
        "amazon-adsystem.com", "advertising.com", "serving-sys.com",
        "publift.com", "33across.com", "sharethrough.com", "triplelift.com",
        "yieldmo.com", "lijit.com", "media.net", "revcontent.com",
        "scorecardresearch.com", "quantserve.com", "hotjar.com",
        "clarity.ms", "mixpanel.com", "app-measurement.com"
    ));

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        createNotification();
        startForeground(1, notification());
        if (!running) startTunnel();
        return START_STICKY;
    }

    private void createNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "Ad blocker protection", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    private Notification notification() {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return b.setContentTitle("Universal Ad Blocker PRO")
            .setContentText("DNS protection is active")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true).build();
    }

    private void startTunnel() {
        try {
            Builder builder = new Builder()
                .setSession("Universal Ad Blocker PRO")
                .setMtu(1500)
                .addAddress(DNS_IP, 32)
                .addDnsServer(DNS_IP)
                .addRoute(DNS_IP, 32)
                .allowFamily(android.system.OsConstants.AF_INET);
            tun = builder.establish();
            if (tun == null) { stopSelf(); return; }
            running = true;
            serviceRunning = true;
            worker = new Thread(this::readPackets, "UAB-DNS-Worker");
            worker.start();
        } catch (Exception e) {
            running = false;
            serviceRunning = false;
            stopSelf();
        }
    }

    private void readPackets() {
        byte[] buf = new byte[32767];
        try (FileInputStream in = new FileInputStream(tun.getFileDescriptor());
             FileOutputStream out = new FileOutputStream(tun.getFileDescriptor())) {
            while (running) {
                int n = in.read(buf);
                if (n <= 0) continue;
                byte[] packet = Arrays.copyOf(buf, n);
                byte[] response = handleIpv4UdpDns(packet);
                if (response != null) out.write(response);
            }
        } catch (Exception ignored) { }
    }

    private byte[] handleIpv4UdpDns(byte[] p) {
        try {
            if (p.length < 28 || (p[0] >> 4) != 4 || (p[9] & 0xff) != 17) return null;
            int ihl = (p[0] & 0x0f) * 4;
            if (p.length < ihl + 8) return null;
            int srcPort = u16(p, ihl), dstPort = u16(p, ihl + 2);
            if (dstPort != 53) return null;
            int udpLen = u16(p, ihl + 4);
            if (udpLen < 8 || ihl + udpLen > p.length) return null;
            byte[] query = Arrays.copyOfRange(p, ihl + 8, ihl + udpLen);
            String domain = readQName(query).toLowerCase(Locale.ROOT);
            byte[] dnsResponse;
            if (isBlocked(domain)) {
                blockedCount++;
                getSharedPreferences("uab_stats", MODE_PRIVATE).edit().putInt("blocked", blockedCount).apply();
                dnsResponse = blockedResponse(query);
            } else {
                dnsResponse = forward(query);
                if (dnsResponse == null) return null;
            }
            return makeIpv4UdpResponse(p, ihl, srcPort, dstPort, dnsResponse);
        } catch (Exception e) { return null; }
    }

    private String readQName(byte[] dns) {
        if (dns.length < 17) return "";
        int pos = 12;
        StringBuilder name = new StringBuilder();
        while (pos < dns.length) {
            int len = dns[pos++] & 0xff;
            if (len == 0) break;
            if ((len & 0xc0) != 0 || pos + len > dns.length) return "";
            if (name.length() > 0) name.append('.');
            name.append(new String(dns, pos, len, java.nio.charset.StandardCharsets.US_ASCII));
            pos += len;
        }
        return name.toString();
    }

    private boolean isBlocked(String host) {
        if (host.isEmpty()) return false;
        for (String d : blocked) if (host.equals(d) || host.endsWith("." + d)) return true;
        return false;
    }

    private byte[] blockedResponse(byte[] q) {
        byte[] r = Arrays.copyOf(q, q.length);
        if (r.length < 12) return r;
        r[2] = (byte) ((r[2] | 0x80) & 0xfb); // QR=1, preserve most flags
        r[3] = (byte) ((r[3] & 0xf0) | 0x03); // NXDOMAIN
        r[6] = r[7] = r[8] = r[9] = r[10] = r[11] = 0;
        return r;
    }

    private byte[] forward(byte[] q) {
        DatagramSocket s = null;
        try {
            s = new DatagramSocket();
            protect(s);
            s.setSoTimeout(2500);
            byte[] ip = InetAddress.getByName(UPSTREAM_DNS).getAddress();
            s.send(new DatagramPacket(q, q.length, InetAddress.getByAddress(ip), 53));
            byte[] b = new byte[4096];
            DatagramPacket response = new DatagramPacket(b, b.length);
            s.receive(response);
            return Arrays.copyOf(response.getData(), response.getLength());
        } catch (Exception e) { return null; }
        finally { if (s != null) s.close(); }
    }

    private byte[] makeIpv4UdpResponse(byte[] original, int ihl, int srcPort, int dstPort, byte[] dns) {
        int total = 20 + 8 + dns.length;
        byte[] r = new byte[total];
        r[0] = 0x45; r[1] = 0;
        put16(r, 2, total); put16(r, 4, u16(original, 4));
        r[8] = 64; r[9] = 17;
        System.arraycopy(original, 16, r, 12, 4); // old destination becomes response source
        System.arraycopy(original, 12, r, 16, 4); // old source becomes response destination
        put16(r, 10, checksum(r, 0, 20));
        put16(r, 20, dstPort); put16(r, 22, srcPort);
        put16(r, 24, 8 + dns.length); put16(r, 26, 0);
        System.arraycopy(dns, 0, r, 28, dns.length);
        return r;
    }

    private int checksum(byte[] b, int off, int len) {
        long sum = 0;
        for (int i = off; i < off + len; i += 2) {
            int v = (b[i] & 0xff) << 8;
            if (i + 1 < off + len) v |= b[i + 1] & 0xff;
            sum += v;
        }
        while ((sum >> 16) != 0) sum = (sum & 0xffff) + (sum >> 16);
        return (int) (~sum) & 0xffff;
    }
    private int u16(byte[] b, int o) { return ((b[o] & 255) << 8) | (b[o+1] & 255); }
    private void put16(byte[] b, int o, int v) { b[o]=(byte)(v>>8); b[o+1]=(byte)v; }

    @Override public void onDestroy() {
        running = false;
        serviceRunning = false;
        try { if (tun != null) tun.close(); } catch (Exception ignored) {}
        super.onDestroy();
    }
}
