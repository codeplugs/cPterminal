package com.cpterminal;

import android.app.Activity;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.view.GestureDetector;
import android.graphics.Rect;
import android.view.ViewTreeObserver;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.view.Menu;
import android.view.MenuItem;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.content.res.Resources;
import android.view.Gravity;
import android.widget.ProgressBar; // Jika ingin pakai animasi muter
import android.widget.LinearLayout; // Untuk menyusun ProgressBar & Teks
import android.view.LayoutInflater;
import android.widget.ListView;
import android.content.ClipboardManager;
import android.content.ClipData;

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalEmulator;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;
import com.termux.terminal.TerminalSession.SessionChangedCallback;

import com.cpterminal.BuildConfig;
import com.cpterminal.extrakeys.ExtraKeysView;
import com.cpterminal.extrakeys.ExtraKeysInfo;
import com.cpterminal.extrakeys.ExtraKeyButton;
import com.cpterminal.extrakeys.ExtraKeysConstants;
import com.google.android.material.button.MaterialButton;


public class MainActivityCopy1 extends AppCompatActivity {
	// Simpan tipe session: 0 untuk Alpine, 1 untuk Android
private List<Integer> sessionTypes = new ArrayList<>();

 private boolean keyboardVisible = false; // tambahkan ini
private boolean isCtrlActive = false;
private boolean isAltActive = false;
private String selectedTitle = "Alpine";
private AlertDialog loadingDialog;
private int lastSelectedEnvironmentIndex = 0;
private int sessionCounter = 0;
private int lastSessionNumber = 0;
private ListView globalListView;
private ArrayAdapter<TerminalSession> globalSessionAdapter;
private AlertDialog sessionListDialog;

private TextView sessionBadgeTxt;
private ExtraKeysView extraKeysView;
 private ExtraKeysInfo currentExtraKeysInfo; // Tambahkan ini
    private TerminalView terminalView;
    private TerminalSession terminalSession;
	private TerminalService mTerminalService;
	private SessionChangedCallback callback;
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

/*@Override
public void onServiceConnected(ComponentName name, IBinder service) {
    TerminalService.TerminalServiceBinder binder = (TerminalService.TerminalServiceBinder) service;
    mTerminalService = binder.getService();
    
    // 1. Ambil sesi yang ada di Service
    TerminalSession existingSession = mTerminalService.getActiveSession();
    
    // 2. Cek apakah file marker sudah ada (untuk menentukan sesi mana yang dibuat)
    File markerFile = new File(getFilesDir(), "AlpineInstalled");

    if (existingSession != null) {
        // --- SKENARIO RE-ATTACH (Aplikasi hidup kembali) ---
        terminalSession = existingSession;
        terminalSession.updateCallback(callback); 
        terminalSession.forceResetState();
        
        // Ambil index yang tersimpan di Service
        lastSelectedEnvironmentIndex = mTerminalService.getCurrentSessionIndex();
        
        // Sinkronkan selectedTitle berdasarkan index yang tersimpan
        // Contoh: Index 0 adalah Alpine
        if (lastSelectedEnvironmentIndex == 0) {
            selectedTitle = "Alpine";
        }
        
    } else {
        // --- SKENARIO COLD BOOT (Baru pertama kali buka/setelah kill) ---
        
        if (markerFile.exists()) {
            // Jika sudah terinstall, langsung buat sesi Proot Alpine
            terminalSession = AlpineSessionInstalled();
            selectedTitle = "Alpine";
        } else {
            // Jika belum terinstall, buat sesi sh biasa untuk setup
            terminalSession = AlpineSession(); 
            selectedTitle = "Setup Alpine";
        }
        
        mTerminalService.registerSession(terminalSession);
        mTerminalService.setCurrentSessionIndex(0);
        lastSelectedEnvironmentIndex = 0;
    }
    
    // 3. Update UI sesuai dengan selectedTitle yang sudah ditentukan
    // Misal: setActionBarTitle(selectedTitle) atau updateSpinner(selectedTitle);
    
    terminalView.attachSession(terminalSession);
}*/





   @Override
public void onServiceConnected(ComponentName name, IBinder service) {
    TerminalService.TerminalServiceBinder binder = (TerminalService.TerminalServiceBinder) service;
    mTerminalService = binder.getService();
    
    
    TerminalSession existingSession = mTerminalService.getActiveSession();
    File markerFile = new File(getFilesDir(), "AlpineInstalled");

    if (existingSession != null) {
    	
        terminalSession = existingSession;
        terminalSession.updateCallback(callback); 
        //lastSelectedEnvironmentIndex = mTerminalService.getCurrentSessionIndex();
          switchSession(terminalSession);
            if (markerFile.exists()) {
            //selectedTitle = "Alpine";
        } else {
            //selectedTitle = "Setup Alpine";
        }
    } else {
        if (markerFile.exists()) {
            terminalSession = AlpineSessionInstalled();
            lastSelectedEnvironmentIndex = 0;
           selectedTitle = "Alpine"; 
           new Handler(Looper.getMainLooper()).postDelayed(() -> {
        //sendToAlpine("cat /etc/motd");
    }, 1000); 


        } else {
            terminalSession = AlpineSession(); 
            selectedTitle = "Setup Alpine";
            lastSelectedEnvironmentIndex = 0;
            showLoadingDialog("Setup Alpine Linux...\nPlease wait.");
            //selectedTitle = "Setup Alpine";
            // Kirim perintah install dengan pemicu di akhir
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                String rootfsPath = "/data/data/com.cpterminal/files/rootfs";
String markerPath = markerFile.getAbsolutePath();

sendToAlpine(
    "echo '[*] Downloading script...'\n" +
    "curl -L -# --fail -o setup.sh https://gist.githubusercontent.com/aznoisib/10edbbe1dd7fd66d55958d01d536f22f/raw/a253a30821593f107962789872a02337eccfa14b/setupproot.sh\n" +
    "chmod 755 setup.sh\n" +
    "echo '[*] Executing...'\n" +
    "sh setup.sh\n" +
    "mkdir tmp\n"
);

sendToAlpine(
    "while [ ! -d /data/data/com.cpterminal/files/rootfs/bin ]; do\n" +
    "  echo '[*] Waiting rootfs...'\n" +
    "  sleep 1\n" +
    "done\n" +
    "echo ' RootFS ready'\n"  +
      "touch " + markerPath + " && " +
                    "echo 'CP_INSTALL_SUCCESS'"
);

                
            }, 1000);
        }
        mTerminalService.registerSession(terminalSession);
        
    }
    terminalView.attachSession(terminalSession);
    
}


    @Override
    public void onServiceDisconnected(ComponentName name) {
        mTerminalService = null;
    }
};

@Override
protected void onStop() {
    super.onStop();
    // Lepaskan koneksi service saat activity tidak terlihat
    if (mTerminalService != null) {
        unbindService(mServiceConnection);
        mTerminalService = null;
    }
}

@Override
protected void onResume() {
    super.onResume();
    // Beri sedikit delay (100-200ms) agar UI siap menerima update teks
    new Handler(Looper.getMainLooper()).postDelayed(() -> {
        updateSessionBadge();
    }, 200);
}




@Override
protected void onStart() {
    super.onStart();
    Intent intent = new Intent(this, TerminalService.class);
    // Jalankan agar tetap hidup meski MainActivity hancur
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent);
    } else {
        startService(intent);
    }
    // Bind untuk interaksi antar code
    bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
}
	
	@Override
protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    
    // Saat notifikasi diklik dan app sudah terbuka, 
    // pastikan kita tetap fokus ke terminal yang sama
    if (terminalView != null && terminalSession != null) {
        terminalView.attachSession(terminalSession);
    }
}
	
private TerminalSession createNewSession() {
    String prefix = getFilesDir().getAbsolutePath();
    String shellPath = prefix + "/bin/bash";
    
    TerminalSession session = new TerminalSession(
         "/system/bin/sh",   // command
        prefix,               // args
         new String[0],               // env
         new String[0],               // cwd
        callback   
    );
    session.mSessionName = "Android";
    return session;
}

private void switchToAlpineProper() {
	dismissLoadingDialog();
    if (mTerminalService == null) return;

    // 1. Matikan sesi installer
    if (terminalSession != null) {
        terminalSession.finishIfRunning();
    }

    // 2. Bersihkan daftar sesi
    mTerminalService.mTerminalSessions.clear();

    // 3. Buat sesi Alpine yang sudah terinstall
    terminalSession = AlpineSessionInstalled();
    
    // 4. Daftarkan dan tempel ke UI
    mTerminalService.registerSession(terminalSession);
    mTerminalService.setCurrentSessionIndex(0);
    terminalView.attachSession(terminalSession);
    
    selectedTitle = "Alpine";
    
    
    
    new Handler(Looper.getMainLooper()).postDelayed(() -> {
        sendToAlpine("apk update");
        sendToAlpine("apk upgrade");
        
    }, 1000);

    
    Toast.makeText(this, "Alpine Linux Siap!", Toast.LENGTH_SHORT).show();
}



private TerminalSession AlpineSessionInstalled() {
    String home = getFilesDir().getAbsolutePath();
    String prootPath = home + "/usr/bin/proot.bin"; // Path absolut ke binary
    String linker = "/system/bin/linker64";
    String loader = getApplicationInfo().nativeLibraryDir + "/libproot-loader.so";
    
    // Parameter 1: shellPath adalah linker-nya
    String shellPath = linker; 

    // Parameter 2: cwd (Set ke home saja, jangan ke rootfs/root dulu)
    String cwd = home;

    // Parameter 3: args 
    // Linker64 akan membaca args[0] sebagai program yang harus dijalankan.
    // Pastikan prootPath di sini adalah ABSOLUTE PATH.
    String[] args = {
    linker,           // argv[0] untuk linker
    prootPath,        // argv[1] (Program yang dicari linker)
    "-r",  "/data/data/com.cpterminal/files/rootfs",
    "-0",
    "-b", "/dev",
    "-b", "/proc",
    "-b", "/sys",
    "-b", "/sdcard",
     "-b", "/storage",
    "-w", "/root",
     "/bin/sh",
    "-c", "cat /etc/motd; exec /bin/sh -l"
    //"/bin/sh",
    //"-l"
};


    // Parameter 4: env
    String[] env = {
    	"PROOT_TMP_DIR=/data/data/com.cpterminal/files/tmp",
    	"LD_LIBRARY_PATH=/data/data/com.cpterminal/files/usr/lib",
        "PROOT_TMP_DIR=" + home + "/tmp",
        "PROOT_LOADER=" + loader,
        "HOME=/root",
        "TERM=xterm-256color",
        "PATH=/usr/bin:/bin:/usr/sbin:/sbin"
    };

    TerminalSession session = new TerminalSession(
        shellPath, 
        cwd, 
        args, 
        env, 
        callback
    );
    
    session.mSessionName = "Alpine";
    
    return session;
}




private TerminalSession AlpineSession() {
    String prefix = getFilesDir().getAbsolutePath();
    String shellPath = prefix + "/bin/bash";
    
    return new TerminalSession(
         "/system/bin/sh",   // command
        prefix,               // args
         new String[0],               // env
         new String[0],               // cwd
        callback   
    );
}

public void updateSessionBadge() {
    if (sessionBadgeTxt != null && mTerminalService != null) {
        runOnUiThread(() -> {
            int count = mTerminalService.mTerminalSessions.size();
            sessionBadgeTxt.setText(String.valueOf(count));
        });
    }
}


public void sendToAlpine(String command) {
    // Pastikan Service tidak null dan ada session yang tersedia
    if (mTerminalService != null && !mTerminalService.mTerminalSessions.isEmpty()) {
        
        // Cek apakah Title yang dipilih saat ini adalah "Alpine"
        if (selectedTitle.equals("Alpine") || selectedTitle.equals("Setup Alpine")) {
            // Karena Alpine adalah session pertama, kita ambil index 0
            TerminalSession session = mTerminalService.mTerminalSessions.get(0);
            
            if (session != null && session.isRunning()) {
                session.write(command + "\r");
            }
        } else {
            // Logika opsional: Jika sedang di Android, beri tahu user perintah tidak dikirim
            //atau kamu bisa tetap memaksa kirim ke index 0 secara background.
            //Log.d("cPterminal", "Gagal kirim: Selected Title bukan Alpine");
        }
    }
}


private void switchSession(TerminalSession session) {
    if (session == null) return;
    String type = session.mSessionName; 

    if ("Alpine".equals(type)) {
        lastSelectedEnvironmentIndex = 0;
        selectedTitle = "Alpine";
    } else {
        lastSelectedEnvironmentIndex = 1;
        selectedTitle = "Android";
    }
    this.terminalSession = session;
    
    // PENTING: Update callback agar session lama lapor ke activity yang sekarang
    terminalSession.updateCallback(this.callback); 
    //  SIMPAN INDEX KE SERVICE
    if (mTerminalService != null) {
        int index = mTerminalService.mTerminalSessions.indexOf(session);
        if (index != -1) {
        	
            mTerminalService.setCurrentSessionIndex(index);
            // Update juga variabel radio button agar tetap sinkron
            //lastSelectedEnvironmentIndex = index; 
        }
    }
    // Pasang ke view
    terminalView.attachSession(terminalSession);
    
    
}


private int getMaxSessionId() {
    int maxId = 0;
    for (TerminalSession s : mTerminalService.mTerminalSessions) {
        if (s.mSessionId > maxId) maxId = s.mSessionId;
    }
    return maxId;
}

private void addNewSession() {
    if (mTerminalService == null) return;
    sessionCounter = getMaxSessionId(); // sync dulu
    sessionCounter++;
    TerminalSession newSession;
    
    // Cek index yang sedang aktif (0: Alpine, 1: Android)
    if (lastSelectedEnvironmentIndex == 0) {
        // Buat sesi Alpine baru
        newSession = AlpineSessionInstalled();
        
        Toast.makeText(this, "New Alpine Session", Toast.LENGTH_SHORT).show();
    } else {
        // Buat sesi Android baru
        newSession = createNewSession();
        
        Toast.makeText(this, "New Android Session", Toast.LENGTH_SHORT).show();
    }
    
    newSession.mSessionId = sessionCounter;
    
    // Daftarkan ke service
    mTerminalService.registerSession(newSession);
    
    // Langsung pindah ke sesi yang baru dibuat
    switchSession(newSession);
    updateSessionBadge();
    if (globalSessionAdapter != null) {
        globalSessionAdapter.notifyDataSetChanged();
    }
    
    
}

private void handleSessionExit(TerminalSession session) {
    if (mTerminalService != null) {
    	session.mSessionId = sessionCounter  - 1;
    	TerminalSession currentSession = terminalView.getCurrentSession();

        // 1. Hapus session dari List di Service
        mTerminalService.removeSession(session);
        
        // 2. Cek apakah masih ada session lain yang tersisa
        if (mTerminalService.mTerminalSessions.isEmpty()) {
            // Jika benar-benar kosong, baru tutup aplikasi
            stopService(new Intent(this, TerminalService.class));
            finish();
        } else {
            // Jika masih ada session lain (misal Alpine masih ada)
            // Pindahkan tampilan secara otomatis ke session yang tersisa
            if (session == currentSession) {
            TerminalSession nextSession = mTerminalService.getLastSession();
            
            switchSession(nextSession);
            Toast.makeText(this, "Session ditutup. Berpindah ke session aktif lainnya.", Toast.LENGTH_SHORT).show();
       } else {
       	lastSessionNumber = session.mSessionId;
            // 4. Kalau yang dihapus BUKAN session aktif
            // Tidak perlu pindah session
            Toast.makeText(this, "Session lain ditutup.", Toast.LENGTH_SHORT).show();
        }
        }
    }
}
	
	private void showLoadingDialog(String message) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    
    // Custom View sederhana
    TextView textView = new TextView(this);
    textView.setText(message);
    textView.setPadding(50, 50, 50, 50);
    textView.setTextSize(18);
    textView.setTextColor(Color.WHITE);
    textView.setTypeface(Typeface.MONOSPACE);

    builder.setView(textView);
    builder.setCancelable(false); // Kunci agar tidak bisa di-back

    loadingDialog = builder.create();
    
    // Beri warna background agar senada dengan terminal
    if (loadingDialog.getWindow() != null) {
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#3605B0")));
    }
    
    loadingDialog.show();
}

private void dismissLoadingDialog() {
    if (loadingDialog != null && loadingDialog.isShowing()) {
        loadingDialog.dismiss();
    }
}

	
	private void showSessionListDialog() {
    if (mTerminalService == null) return;
    List<TerminalSession> sessions = mTerminalService.mTerminalSessions;

    // 1. Setup Adapter
    globalSessionAdapter = new ArrayAdapter<TerminalSession>(this, R.layout.item_session, sessions) {
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                Context themeWrapper = new androidx.appcompat.view.ContextThemeWrapper(getContext(), 
                    com.google.android.material.R.style.Theme_MaterialComponents_DayNight);
                convertView = LayoutInflater.from(themeWrapper).inflate(R.layout.item_session, parent, false);
            }

            TerminalSession session = getItem(position);
            TextView txtName = convertView.findViewById(R.id.txtSessionName);
            MaterialButton btnClose = convertView.findViewById(R.id.btnCloseSession);
            btnClose.setRippleColor(ColorStateList.valueOf(Color.TRANSPARENT));
            btnClose.setElevation(0);
            btnClose.setStateListAnimator(null);
            // LOGIKA TANDAI AKTIF (HIGHLIGHT)
            if (session == terminalSession) {
            	btnClose.setVisibility(View.GONE); 
                convertView.setBackgroundColor(Color.parseColor("#4000FFFF")); // Cyan Transparan
                txtName.setTextColor(Color.parseColor("#00FFFF")); // Teks Cyan
                txtName.setTypeface(null, Typeface.BOLD);
            } else {
            	btnClose.setVisibility(View.VISIBLE);
                convertView.setBackgroundColor(Color.TRANSPARENT);
                txtName.setTextColor(Color.WHITE);
                txtName.setTypeface(null, Typeface.NORMAL);
            }

            // Penomoran ID
            txtName.setText((session.mSessionId + 1) + ". " + (session.mSessionName != null ? session.mSessionName : "Session"));

            // KLIK BARIS: Pindah Sesi, Dialog tetap buka
            convertView.setOnClickListener(v -> {
                switchSession(session);
                notifyDataSetChanged(); // Refresh highlight biru
            });

            // KLIK TOMBOL HAPUS (DI KANAN): Dialog tetap buka
            btnClose.setOnClickListener(v -> {
                TerminalSession toDelete = session;
                boolean isDeletingActive = (toDelete == terminalSession);

                // Hapus sesi melalui service
                handleSessionExit(toDelete);
                updateSessionBadge();

                if (sessions.isEmpty()) {
                    sessionListDialog.dismiss();
                } else {
                    // Jika yang dihapus sedang aktif, pindahkan ke sesi terdekat
                    if (isDeletingActive) {
                        int nextIndex = (position >= sessions.size()) ? sessions.size() - 1 : position;
                        switchSession(sessions.get(nextIndex));
                    }
                    
                    notifyDataSetChanged();
                    
               
                }
            });

            return convertView;
        }
    };

    // 2. Setup Dialog Builder
    TextView title = new TextView(this);
    title.setText("Active Sessions");
    title.setTextColor(Color.WHITE);
    title.setPadding(40, 40, 40, 20);
    title.setTextSize(18);

    sessionListDialog = new AlertDialog.Builder(this)
            .setCustomTitle(title)
            .setAdapter(globalSessionAdapter, null)
       .create();

    sessionListDialog.show();

    // 3. ATUR UKURAN BOX (Agar tidak menghalangi terminal)
    globalListView = sessionListDialog.getListView();
    if (sessionListDialog.getWindow() != null) {
        // Warna background dialog ungu gelap
        sessionListDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#3605B0")));
        
        // Ukuran Kotak: Lebar 85% layar, Tinggi 60% layar
        int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.85);
        int height = (int)(getResources().getDisplayMetrics().heightPixels * 0.60);
        sessionListDialog.getWindow().setLayout(width, height);
        
        // Atur posisi agar di tengah
        sessionListDialog.getWindow().setGravity(Gravity.CENTER);
        
        // Efek transparan pada background luar dialog (dim)
        sessionListDialog.getWindow().setDimAmount(0.5f);
    }
}

private void showAboutDialog() {
        String versionName = BuildConfig.VERSION_NAME;
        int versionCode = BuildConfig.VERSION_CODE;
String libPath = getApplicationInfo().nativeLibraryDir;

        String message =
                "cP Terminal\n\n" +
                "Version: " + versionName + " (" + versionCode + ")\n\n" +
                "Lib path: " + libPath + "\n\n" +
                "Developer: Codeplug\n" +
                "© 2026 All Rights Reserved\n\n" +
                "Terminal Android .\n";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Info App");
        //builder.setMessage(message);
TextView textView = new TextView(this);
textView.setText(message);
textView.setTextColor(0xFFFFFFFF); //
textView.setPadding(50, 30, 50, 30);
builder.setView(textView);
        builder.setPositiveButton("Copy", (dialog, which) -> {

    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    ClipData clip = ClipData.newPlainText("About Info", message);
    clipboard.setPrimaryClip(clip);

    Toast.makeText(this, "Copy to clipboard", Toast.LENGTH_SHORT).show();
});
AlertDialog dialog = builder.create();
dialog.setOnShowListener(d -> {
	TextView title = dialog.findViewById(
        getResources().getIdentifier("alertTitle", "id", "android")
);
    if (title != null) {
        title.setTextColor(0xFFFFFFFF); // putih
    }
    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    dialog.getWindow().setBackgroundDrawable(
            new android.graphics.drawable.ColorDrawable(0xFF3605B0)
    );
});
        dialog.show();
    }
	
	private void showRadioDialog() {
  String[] options = {"Alpine", "Android"};
final int[] selectedIndex = {0};

final int[] tempChoice = {lastSelectedEnvironmentIndex};

//  Custom adapter untuk radio text
ArrayAdapter<String> adapter = new ArrayAdapter<String>(
        this,
        android.R.layout.simple_list_item_single_choice,
        options
) {
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        TextView text = view.findViewById(android.R.id.text1);
        text.setTextColor(Color.WHITE); //  RADIO TEXT PUTIH
        text.setTypeface(Typeface.MONOSPACE); // opsional biar terminal vibe

        view.setBackgroundColor(Color.parseColor("#3605B0")); // background item

        return view;
    }
};

//  Custom TITLE
TextView title = new TextView(this);
title.setText("Pilih Mode");
title.setTextColor(Color.WHITE); //  TITLE PUTIH
title.setTextSize(20);
title.setPadding(40, 40, 40, 20);

AlertDialog dialog = new AlertDialog.Builder(this)
        .setCustomTitle(title) //  pakai custom title
		.setSingleChoiceItems(adapter, lastSelectedEnvironmentIndex, (d, which) -> {
            // 3. Update variabel class setiap kali user klik (sebelum tekan OK)
            lastSelectedEnvironmentIndex = which;
        })
       /* .setPositiveButton("OK", (d, which) -> {
			int index = lastSelectedEnvironmentIndex; 
            String modeName = (index == 0) ? "Alpine" : "Android";
            selectedTitle = (lastSelectedEnvironmentIndex == 0) ? "Alpine" : "Android";
            TerminalSession targetSession = null;
            if (mTerminalService.mTerminalSessions.size() > index) {
                targetSession = mTerminalService.mTerminalSessions.get(index);
            }

            if (targetSession == null) {
                if (index == 0) {
                    targetSession = AlpineSession();
                } else {
                    targetSession = createNewSession();
                }
                mTerminalService.registerSession(targetSession);
                Toast.makeText(this, " Create Session " + modeName, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Switch to " + modeName, Toast.LENGTH_SHORT).show();
            }

            switchSession(targetSession);
			
///start
int index = selectedIndex[0]; // 0 untuk Alpine, 1 untuk Android
    String modeName = (index == 0) ? "Alpine" : "Android";
    TerminalSession targetSession = null;

    //  1. CEK: Apakah di list Service sudah ada session pada posisi index tersebut
    if (mTerminalService.mTerminalSessions.size() > index) {
        targetSession = mTerminalService.mTerminalSessions.get(index);
    }

    //  2. LOGIKA: Buat Baru vs Switch
    if (targetSession == null) {
        // --- PROSES CREATE ---
        if (index == 0) {
            targetSession = AlpineSession();
        } else {
            targetSession = createNewSession();
        }
        mTerminalService.registerSession(targetSession);
        
        // Tampilkan Toast saat membuat session pertama kali
        Toast.makeText(this, " Membuat Session " + modeName, Toast.LENGTH_SHORT).show();
    } else {
        // --- PROSES SWITCH ---
        // Tampilkan Toast saat berpindah ke session yang sudah ada di memori
        Toast.makeText(this, " Switch ke " + modeName, Toast.LENGTH_SHORT).show();
    }

    //  3. EKSEKUSI
    switchSession(targetSession);
   ///end
			
			
			
			
			
            //String selected = options[selectedIndex[0]];
            //Toast.makeText(this, "Dipilih: " + selected, Toast.LENGTH_SHORT).show();
        })
        .setNegativeButton("Cancel", null)*/
      
        .create();

dialog.show();

//  Background dialog
if (dialog.getWindow() != null) {
    dialog.getWindow().setBackgroundDrawable(
        new ColorDrawable(Color.parseColor("#3605B0"))
    );
}

//  Tombol jadi putih
dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
}
	
	private void sendControlKey(TerminalSession session, int codePoint) {
    if (session == null) return;

    int controlCode = -1;
    
    // Jika input adalah huruf a-z atau A-Z
    if (codePoint >= 'a' && codePoint <= 'z') {
        controlCode = codePoint - 'a' + 1;
    } else if (codePoint >= 'A' && codePoint <= 'Z') {
        controlCode = codePoint - 'A' + 1;
    } 
    // Handle karakter spesial tambahan seperti di kodemu
    else if (codePoint == '@') {
        session.write("\u0000");
        return;
    } else if (codePoint == '[') {
        session.write("\u001b");
        return;
    } else if (codePoint == ' ') {
        controlCode = 0; // Ctrl + Space
    }

    if (controlCode != -1) {
        session.write(new String(new char[]{(char) controlCode}));
    }

    // Reset status CTRL setelah digunakan
    isCtrlActive = false;
    runOnUiThread(() -> extraKeysView.reload(currentExtraKeysInfo, 0));
}
	
	
	
	
	
	// PERBAIKAN: Fungsi pengirim tombol
    private void sendKeyToTerminal(String key) {
        // Ambil session saat ini dari terminalView
        TerminalSession currentSession = terminalView.getCurrentSession();
        if (currentSession == null) return;

        switch (key) {
    case "ESC": 
        currentSession.write("\u001b"); 
        break;
    case "TAB": 
        currentSession.write("\t"); 
        break;
    case "ENTER": 
        currentSession.write("\r"); 
        break;
    
    // Gunakan dispatchKeyEvent untuk navigasi agar dash/sh tidak error ^[[A
    case "UP": 
        terminalView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP)); 
        break;
    case "DOWN": 
        terminalView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN)); 
        break;
    case "LEFT": 
        terminalView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT)); 
        break;
    case "RIGHT": 
        terminalView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT)); 
        break;

    // Tambahan sesuai gambar
    case "HOME": 
        terminalView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_HOME)); 
        break;
    case "END": 
        terminalView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_END)); 
        break;
    case "PGUP": 
        currentSession.write("\u001b[A"); 
        break;
    case "PGDN": 
        currentSession.write("\u001b[B"); 
        break;
    
    case "/": 
        currentSession.write("/"); 
        break;
    case "-": 
        currentSession.write("-"); 
        break;

   
			default:
            if (key.length() == 1) {
                // Jika tombol 'C' di Extra Keys diklik saat tombol CTRL software aktif
                if (isCtrlActive) {
                    char c = key.toUpperCase().charAt(0);
                    currentSession.write(new String(new char[]{(char) (c - 64)}));
                    isCtrlActive = false; // Reset
                    extraKeysView.reload(currentExtraKeysInfo, 0);
                } else {
                    currentSession.write(key);
                }
            }
            break;
        }
		 terminalView.updateSize();
        terminalView.scrollToBottom();
    }
	
	
	

	
	private void scrollToBottom() {
    if (terminalView != null) {
        terminalView.post(() -> {
            // Memanggil method yang sudah kita buat public di TerminalView
            terminalView.updateSize(); 
            terminalView.setTopRow(0);
            terminalView.onScreenUpdated();
            terminalView.invalidate();
        });
    }
}
	

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

     File filesDir = this.getFilesDir();
    if (!filesDir.exists()) {
        filesDir.mkdirs();
    }

	   
        // TerminalView
        terminalView = findViewById(R.id.terminal_view);


float sizeInSp = 13;
float selectedSize = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_SP, 
    sizeInSp, 
    getResources().getDisplayMetrics()
);
terminalView.setTextSize((int) selectedSize);


Typeface tf = Typeface.createFromAsset(
    getAssets(),
    "fonts/JetBrainsMono_Regular.ttf"
);
terminalView.setTypeface(tf);
terminalView.setFocusable(true);
        terminalView.setFocusableInTouchMode(true);
        terminalView.requestFocus();


   extraKeysView = findViewById(R.id.extra_keys);
		

		
		/* String buttonsJson = "[" +
    "[" +
    "  'ESC', " +
    "  {key: 'MY_CONTROL_KEY', display: 'CTRL'}, " + // Paksa sebagai objek
    "  {key: 'MY_ALT_KEY', display: 'ALT'}, " +  // Paksa sebagai objek
    "  {key: 'TAB', display: 'TAB'}, " +
    "  'UP'" +
    "]," +
    "['LEFT', 'DOWN', 'RIGHT']" +
    "]"; */
    
    String buttonsJson = "[" +
    "[" +
    "  'ESC', " +
    "  '/', " +
    "  '-', " +
    "  'HOME', " +
    "  'UP', " +
    "  'END', " +
    "  'PGUP'" + // Tambah PGUP di sini
    "]," +
    "[" +
    "  'TAB', " +
    "  {key: 'MY_CONTROL_KEY', display: 'CTRL'}, " + 
    "  {key: 'MY_ALT_KEY', display: 'ALT'}, " +  
    "  'LEFT', " +
    "  'DOWN', " +
    "  'RIGHT', " +
    "  'PGDN'" + // Tambah PGDN di sini
    "]" +
    "]";
    
	
	
try {
    // 2. Gunakan 3 parameter sesuai constructor di file ExtraKeysInfo.java kamu:
    // Parameter 1: String JSON
    // Parameter 2: Nama style (misal "default")
    // Parameter 3: Alias map (kita kirim default aliases dari Constants)
    
    currentExtraKeysInfo = new ExtraKeysInfo(
        buttonsJson, 
        "default", 
        ExtraKeysConstants.CONTROL_CHARS_ALIASES
    );

    extraKeysView.reload(currentExtraKeysInfo, 0);
// PAKSA MUNCUL:
//extraKeysView.setVisibility(View.VISIBLE);
//extraKeysView.setAlpha(1.0f); // Pastikan tidak transparan
//extraKeysView.bringToFront(); // Paksa ke lapisan paling atas
extraKeysView.setBackgroundColor(android.graphics.Color.BLACK); // Beri wa


} catch (org.json.JSONException e) {
    e.printStackTrace();
}

        extraKeysView.setExtraKeysViewClient(new ExtraKeysView.IExtraKeysView() {
            @Override
            public void onExtraKeyButtonClick(View view, ExtraKeyButton buttonInfo, MaterialButton button) {
				String key = buttonInfo.getKey();
  TerminalSession session = terminalView.getCurrentSession();

        if (session != null) {
			
    if ("MY_CONTROL_KEY".equals(key)) {
        isCtrlActive = !isCtrlActive; // Toggle status
		
		if (isCtrlActive) {
			
                    //char c = key.toLowerCase().charAt(0);
                   //session.write(new String(new char[]{3})); // CTRL + huruf
			
            // Warna saat aktif (misal: Biru)
            button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2196F3")));
            button.setTextColor(Color.BLUE);
			//sendControlKey(session, key);
			
        } else {
            // Warna saat mati (kembalikan ke transparan atau abu-abu)
            button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            button.setTextColor(Color.WHITE); // Atau warna defaultmu
        }
    } else if ("MY_ALT_KEY".equals(key)) {
        //isAltActive = !isAltActive;
        //button.setSelected(isAltActive);
    } else {
        sendKeyToTerminal(key);
    }
    }
            }

            @Override
            public boolean performExtraKeyButtonHapticFeedback(View view, ExtraKeyButton buttonInfo, MaterialButton button) {
                return true;
            }
        });





terminalView.setOnTouchListener(new View.OnTouchListener() {
    private GestureDetector gestureDetector = new GestureDetector(MainActivityCopy1.this,
        new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                // Hanya saat tap (single tap), keyboard muncul
                terminalView.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(terminalView, InputMethodManager.SHOW_IMPLICIT);
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                // Scroll jangan muncul keyboard
                return false;
            }
        });

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return false; // biar TerminalView tetap menangani touch
    }
});



 
	

   

TerminalViewClient client = new TerminalViewClient() {
	


    @Override
    public void onSingleTapUp(MotionEvent e) {
        terminalView.requestFocus();
    }

    @Override
    public boolean onLongPress(MotionEvent e) {
        return false;
    }
@Override
public void copyModeChanged(boolean copyMode) {
    // kosong juga gapapa
}
    @Override
    public float onScale(float scale) {
        return scale;
    }

  
	
	
	@Override
public boolean onKeyDown(int keyCode, KeyEvent e, TerminalSession session) {
    // 1. Jalankan fungsi scroll otomatis agar saat menghapus pun layar tetap di bawah
    terminalView.post(() -> {
        terminalView.updateSize();
        terminalView.scrollToBottom();
    });

    // 2. Kirim tombol khusus (seperti Backspace, Enter, Tab) ke session
    // Method ini adalah cara standar Termux mengirim input hardware ke emulator
    /*if (session != null && session.isRunning()) {
        // Jika terminalView punya method handleKeyDown, panggil itu
        // Jika tidak, biarkan sistem menangani lewat return false
    }*/

// 2. Logika khusus untuk tombol ENTER saat proses selesai
    if (keyCode == KeyEvent.KEYCODE_ENTER) {
        // Jika session sudah tidak jalan (tampil pesan Process completed)
        if (session != null && !session.isRunning()) {
            // Skenario A: Finish activity jika session selesai
			//stopService(new Intent(MainActivity.this, TerminalService.class));
            //finish(); 
			handleSessionExit(session);
            // Skenario B: Atau jika mau restart session, panggil method restart kamu di sini
            return true;
        }
    }


    // 3. PENTING: Kembalikan 'false' agar TerminalView internal 
    // tetap menerima event ini dan memproses Backspace/Enter secara normal.
    return false; 
}

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent e) {
        return false;
    }

    @Override
    public boolean readControlKey() {
        return false;
    }

    @Override
    public boolean readAltKey() {
        return false;
    }
@Override
public boolean shouldBackButtonBeMappedToEscape() {
    return false;
}
    //  WAJIB ADA (error kamu minta ini)
    @Override
    public boolean onCodePoint(int codePoint, boolean ctrlDown, TerminalSession session) {
       //session.writeCodePoint(ctrlDown, codePoint);
	   if (session == null) return false;
	   
	  // Gabungkan CTRL hardware dan CTRL software kita
    boolean finalCtrl = ctrlDown || isCtrlActive;
    boolean finalAlt = isAltActive; // Jika terminal mendukung Alt software


    if (isCtrlActive) {
		//session.write(new String(new char[]{3}));
        sendControlKey(session, codePoint); // Panggil fungsi master
        //return true; 
    }else{
		session.writeCodePoint(ctrlDown, codePoint);
	}

 
	   
	   
	   // SETIAP KALI MENGETIK:
        // Gunakan post agar dijalankan setelah layouting selesai
        terminalView.post(() -> {
            terminalView.updateSize();    // Hitung ulang baris yang muat di atas keyboard
            terminalView.scrollToBottom(); // Paksa scroll ke bawah (mTopRow = 0)
        });
        return true;
    }
};

terminalView.setOnKeyListener(client);

   callback =
        new TerminalSession.SessionChangedCallback() {
@Override
public void onColorsChanged(TerminalSession session) {
    // kosong juga gapapa
}
    @Override
    public void onTextChanged(TerminalSession session) {
    	String screenText = session.getEmulator().getScreen().getTranscriptText();
        
        if (screenText.contains("CP_INSTALL_SUCCESS")) {
            // Jalankan transisi otomatis
             
             
              if (selectedTitle.equals("Setup Alpine")) { 
                 runOnUiThread(() -> switchToAlpineProper());
            }
            /*runOnUiThread(() -> {
                // Beri jeda sedikit agar file system Android sinkron
                new Handler().postDelayed(() -> switchToAlpineProper(), 1000);
            });*/
            
        }
        runOnUiThread(() -> terminalView.invalidate());
    }

    @Override
    public void onTitleChanged(TerminalSession session) {}

    @Override
    public void onSessionFinished(TerminalSession session) {
  runOnUiThread(() -> {
  	if (mTerminalService != null) {
            mTerminalService.mTerminalSessions.remove(session);
        }
      updateSessionBadge();
         });

}

    @Override
    public void onClipboardText(TerminalSession session, String text) {}

    @Override
    public void onBell(TerminalSession session) {}
};

     /*   // Path shell
        String prefix = getFilesDir().getAbsolutePath();
        String shellPath = prefix + "/bin/bash";


TerminalSession session = new TerminalSession(
        "/system/bin/sh",   // command
        prefix,               // args
         new String[0],               // env
         new String[0],               // cwd
        callback            // callback
);



        // Buat TerminalSession
        terminalSession = new TerminalSession(shellPath, prefix, new String[0], new String[0],
                new TerminalSession.SessionChangedCallback() {
                    @Override public void onTextChanged(TerminalSession changedSession) {}
                    @Override public void onTitleChanged(TerminalSession changedSession) {}
                    @Override public void onSessionFinished(TerminalSession finishedSession) {}
                    @Override public void onClipboardText(TerminalSession session, String text) {}
                    @Override public void onBell(TerminalSession session) {}
                    @Override public void onColorsChanged(TerminalSession session) {}
                });*/

        //  Attach session setelah view siap
        //terminalView.post(() -> terminalView.attachSession(session));
		
		 
    }
@Override
public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_menu, menu);
    
    MenuItem menuItem = menu.findItem(R.id.action_show_sessions);
    View actionView = menuItem.getActionView();
    sessionBadgeTxt = actionView.findViewById(R.id.session_count_txt);

    // Set klik listener pada actionView (karena kita pakai actionLayout)
    actionView.setOnClickListener(v -> {
        showSessionListDialog();
        
    updateSessionBadge(); 
    });
    
    return true;
}
		@Override
public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_switch) {

       showRadioDialog();
       

        return true;
    }else if (item.getItemId() == R.id.action_info) {

       showAboutDialog();
       

        return true;
    }else if (item.getItemId() == R.id.action_add) {
        
 addNewSession();
       

        return true;
    }
    return super.onOptionsItemSelected(item);
}

  @Override
protected void onDestroy() {
    super.onDestroy();
    //  PERBAIKAN: Jangan panggil terminalSession.finishIfRunning() di sini!
    // Jika dipanggil di sini, setiap kali kamu keluar app, terminal MATI.
    
    if (mTerminalService != null) {
        unbindService(mServiceConnection);
        mTerminalService = null;
    }
}
}