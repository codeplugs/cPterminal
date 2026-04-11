package com.cpterminal; // Sesuaikan dengan package name project kamu

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;

import com.termux.terminal.TerminalSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Service untuk menjaga terminal tetap berjalan di background
 * Mengikuti pola manajemen session pada aplikasi Termux.
 */
public final class TerminalService extends Service {

    private static final String CHANNEL_ID = "cpterminal_notification_channel";
    private static final int NOTIFICATION_ID = 1337;
	private int mCurrentSessionIndex = 0;
    private static final String ACTION_EXIT = "com.cpterminal.ACTION_EXIT";

    /** List penampung semua session aktif */
    public final List<TerminalSession> mTerminalSessions = new ArrayList<>();
    
    private final IBinder mBinder = new TerminalServiceBinder();
    private PowerManager.WakeLock mWakeLock;

    /**
     * Class Binder untuk interaksi dengan MainActivity
     */
    public final class TerminalServiceBinder extends Binder {
        public TerminalService getService() {
            return TerminalService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setupNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_EXIT.equals(intent.getAction())) {
            exitService();
            return START_NOT_STICKY;
        }

        updateNotification();
        return START_STICKY;
    }
	
	
	
	public void setCurrentSessionIndex(int index) {
        this.mCurrentSessionIndex = index;
    }

    public int getCurrentSessionIndex() {
        return mCurrentSessionIndex;
    }
    
    // Update getLastSession agar lebih cerdas
    public TerminalSession getActiveSession() {
        if (mTerminalSessions.isEmpty()) return null;
        // Jika index yang disimpan di luar jangkauan, reset ke 0
        if (mCurrentSessionIndex >= mTerminalSessions.size()) mCurrentSessionIndex = 0;
        return mTerminalSessions.get(mCurrentSessionIndex);
    }
	

    /**
     * Mendaftarkan session baru ke dalam service dan update notifikasi
     */
    public void registerSession(TerminalSession session) {
        if (!mTerminalSessions.contains(session)) {
            mTerminalSessions.add(session);
            updateNotification();
        }
    }

    /**
     * Menghapus session dan update notifikasi
     */
    public void removeSession(TerminalSession session) {
        mTerminalSessions.remove(session);
        if (mTerminalSessions.isEmpty()) {
            stopForeground(true);
            stopSelf();
        } else {
            updateNotification();
        }
    }

    private void updateNotification() {
        // Intent untuk membuka MainActivity saat notifikasi diklik
        // Ganti MainActivity.class jika nama Activity utamamu berbeda
        Intent contentIntent = new Intent(this, MainActivity.class);
		contentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pContentIntent = PendingIntent.getActivity(this, 0, contentIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Intent untuk tombol EXIT di notifikasi
        Intent exitIntent = new Intent(this, TerminalService.class).setAction(ACTION_EXIT);
        PendingIntent pExitIntent = PendingIntent.getService(this, 0, exitIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String sessionCountText = mTerminalSessions.size() + " sessions active";

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("cpterminal")
                .setContentText(sessionCountText)
                .setSmallIcon(R.drawable.ic_stat_code) // Pastikan icon ini ada atau ganti ke icon milikmu
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pContentIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "EXIT", pExitIntent)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Terminal Background Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Menjaga terminal tetap hidup saat aplikasi di background");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Meminta WakeLock agar CPU tetap menyala meski layar mati (Fitur ACQUIRE WAKELOCK)
     */
    public void acquireWakeLock() {
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "cpterminal:wakelock");
            mWakeLock.acquire();
            updateNotification(); // Kamu bisa modif teks notifikasi jika wakelock aktif
        }
    }

    public void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    private void exitService() {
        for (TerminalSession session : mTerminalSessions) {
            session.finishIfRunning();
        }
        mTerminalSessions.clear();
        releaseWakeLock();
        stopForeground(true);
        stopSelf();
        // Memastikan proses benar-benar mati seperti Termux
        android.os.Process.killProcess(android.os.Process.myPid());
    }
	
	// Tambahkan ini di TerminalService.java
public TerminalSession getLastSession() {
    if (mTerminalSessions.isEmpty()) return null;
    return mTerminalSessions.get(mTerminalSessions.size() - 1);
}

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseWakeLock();
    }
}