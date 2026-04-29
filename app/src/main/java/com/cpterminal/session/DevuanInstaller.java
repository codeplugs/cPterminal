package com.cpterminal.session;

import android.content.Context;
import android.util.Log;

import com.cpterminal.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Copies the bundled setup script into the app's files dir and executes it.
 * No network required — safer and offline-capable.
 */
public class DevuanInstaller {

    private static final String TAG              = "DevuanInstaller";
    private static final String ASSET_SCRIPT     = "scripts/setupdevuan.sh";
    private static final String LOCAL_SCRIPT     = "setupdevuan.sh";
    private static final String ROOTFS_CHECK_DIR =
            "/data/data/com.cpterminal/files/rootfsdevuan/bin";

    private final MainActivity activity;
    private final SessionController controller;

    public DevuanInstaller(MainActivity activity, SessionController controller) {
        this.activity = activity;
        this.controller = controller;
    }

    /**
     * Runs the Alpine setup script from assets. Creates {@code markerFile}
     * when finished so the next launch skips setup.
     */
   public void runSetup(File markerFile) {
    File scriptFile = new File(activity.getFilesDir(), LOCAL_SCRIPT);
    if (!copyAssetIfNeeded(activity, ASSET_SCRIPT, scriptFile)) {
        Log.e(TAG, "Failed to copy setup script from assets");
        controller.sendToDevuan("echo 'Gagal copy script'\n");
        return;
    }

    // HAPUS LD_PRELOAD dulu — ini yang bikin FC di fresh install
    controller.sendToDevuan(
        "export LD_LIBRARY_PATH=/data/data/com.cpterminal/files/usr/lib\n" +
        "echo '[*] Running bundled setup...'\n" +
        "chmod 755 " + scriptFile.getAbsolutePath() + "\n" +
        "sh " + scriptFile.getAbsolutePath() + "\n" +
        "echo '--- script selesai ---'\n"
    );

    // Tunggu rootfs tapi kasih timeout 5 menit, jangan infinite
    controller.sendToDevuan(
        "count=0\n" +
        "while [ ! -d " + ROOTFS_CHECK_DIR + " ] && [ $count -lt 300 ]; do\n" +
        "  echo '[*] Waiting rootfs... '$count's'\n" +
        "  sleep 1\n" +
        "  count=$((count+1))\n" +
        "done\n" +
        "if [ -d " + ROOTFS_CHECK_DIR + " ]; then\n" +
        "  touch " + markerFile.getAbsolutePath() + "\n" +
        "  echo 'CP_INSTALL_SUCCESS'\n" +
        "else\n" +
        "  echo '=== DOWNLOAD GAGAL ==='\n" +
        "  echo 'Wifi mati atau timeout. Nyalain internet lalu buka app lagi.'\n" +
        "fi\n"
    );
    
    // Kalo sukses, panggil switch (biar gak nunggu watcher)
    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
        if (markerFile.exists()) {
            controller.switchToDevuanProper();
        }
    }, 2000);
}

    /** Copies an asset file to the target location if it doesn't already exist. */
    private static boolean copyAssetIfNeeded(Context ctx, String assetPath, File target) {
        if (target.exists()) return true;
        try (InputStream in = ctx.getAssets().open(assetPath);
             OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "copyAsset failed", e);
            return false;
        }
    }
}