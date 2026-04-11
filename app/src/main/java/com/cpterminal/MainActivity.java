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
import android.graphics.Color;
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



import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalEmulator;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;
import com.termux.terminal.TerminalSession.SessionChangedCallback;

import com.cpterminal.extrakeys.ExtraKeysView;
import com.cpterminal.extrakeys.ExtraKeysInfo;
import com.cpterminal.extrakeys.ExtraKeyButton;
import com.cpterminal.extrakeys.ExtraKeysConstants;
import com.google.android.material.button.MaterialButton;


public class MainActivity extends AppCompatActivity {
 private boolean keyboardVisible = false; // tambahkan ini
private boolean isCtrlActive = false;
private boolean isAltActive = false;
private ExtraKeysView extraKeysView;
 private ExtraKeysInfo currentExtraKeysInfo; // Tambahkan ini
    private TerminalView terminalView;
    private TerminalSession terminalSession;
	private TerminalService mTerminalService;
	private SessionChangedCallback callback;
	private final ServiceConnection mServiceConnection = new ServiceConnection() {
    @Override
public void onServiceConnected(ComponentName name, IBinder service) {
    TerminalService.TerminalServiceBinder binder = (TerminalService.TerminalServiceBinder) service;
    mTerminalService = binder.getService();
    
    // 🔥 LOGIKA CEK SESSION:
    TerminalSession existingSession = mTerminalService.getLastSession();
    
    if (existingSession != null) {
        // Jika sudah ada session di background, pakai yang itu
        terminalSession = existingSession;
		terminalSession.updateCallback(callback); 
        terminalSession.forceResetState();
		
		
    } else {
        // Jika benar-benar kosong (baru pertama buka), baru buat baru
        terminalSession = createNewSession(); 
        mTerminalService.registerSession(terminalSession);
    }
    
    // Pasang ke view
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
	
	// Buat helper method agar kode onCreate lebih bersih
private TerminalSession createNewSession() {
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
	
	private void showRadioDialog() {
  String[] options = {"Option 1", "Option 2", "Option 3"};
final int[] selectedIndex = {0};

// 🔥 Custom adapter untuk radio text
ArrayAdapter<String> adapter = new ArrayAdapter<String>(
        this,
        android.R.layout.simple_list_item_single_choice,
        options
) {
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        TextView text = view.findViewById(android.R.id.text1);
        text.setTextColor(Color.WHITE); // 🔥 RADIO TEXT PUTIH
        text.setTypeface(Typeface.MONOSPACE); // opsional biar terminal vibe

        view.setBackgroundColor(Color.parseColor("#3605B0")); // background item

        return view;
    }
};

// 🔥 Custom TITLE
TextView title = new TextView(this);
title.setText("Pilih Mode");
title.setTextColor(Color.WHITE); // 🔥 TITLE PUTIH
title.setTextSize(20);
title.setPadding(40, 40, 40, 20);

AlertDialog dialog = new AlertDialog.Builder(this)
        .setCustomTitle(title) // 🔥 pakai custom title
        .setSingleChoiceItems(adapter, selectedIndex[0], (d, which) -> {
            selectedIndex[0] = which;
        })
        .setPositiveButton("OK", (d, which) -> {
            String selected = options[selectedIndex[0]];
            Toast.makeText(this, "Dipilih: " + selected, Toast.LENGTH_SHORT).show();
        })
        .setNegativeButton("Cancel", null)
        .create();

dialog.show();

// 🔥 Background dialog
if (dialog.getWindow() != null) {
    dialog.getWindow().setBackgroundDrawable(
        new ColorDrawable(Color.parseColor("#3605B0"))
    );
}

// 🔥 Tombol jadi putih
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
			
            case "ESC": currentSession.write("\u001b"); break;
			case "BKSP": // Tambahkan case ini jika library mengirimkan singkatan
            currentSession.write("\u007f"); // Kode ASCII untuk menghapus
            break;
            case "TAB": currentSession.write("\t"); break;
            case "ENTER": currentSession.write("\r"); break;
            case "UP": currentSession.write("\u001b[A"); break;
            case "DOWN": currentSession.write("\u001b[B"); break;
            case "RIGHT": currentSession.write("\u001b[C"); break;
            case "LEFT": currentSession.write("\u001b[D"); break;
           
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

     

	   
        // TerminalView
        terminalView = findViewById(R.id.terminal_view);
terminalView.setTextSize(30);
terminalView.setFocusable(true);
        terminalView.setFocusableInTouchMode(true);
        terminalView.requestFocus();


   extraKeysView = findViewById(R.id.extra_keys);
		

		
		String buttonsJson = "[" +
    "[" +
    "  'ESC', " +
    "  {key: 'MY_CONTROL_KEY', display: 'CTRL'}, " + // Paksa sebagai objek
    "  {key: 'MY_ALT_KEY', display: 'ALT'}, " +  // Paksa sebagai objek
    "  {key: 'TAB', display: 'TAB'}, " +
    "  'UP'" +
    "]," +
    "['LEFT', 'DOWN', 'RIGHT']" +
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
    private GestureDetector gestureDetector = new GestureDetector(MainActivity.this,
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
			stopService(new Intent(MainActivity.this, TerminalService.class));
            finish(); 
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
    // ❗ WAJIB ADA (error kamu minta ini)
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
        runOnUiThread(() -> terminalView.invalidate());
    }

    @Override
    public void onTitleChanged(TerminalSession session) {}

    @Override
    public void onSessionFinished(TerminalSession session) {}

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

        // ✅ Attach session setelah view siap
        //terminalView.post(() -> terminalView.attachSession(session));
		
		 
    }
@Override
public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_menu, menu);
    return true;
}
		@Override
public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_switch) {

       showRadioDialog();
       

        return true;
    }
    return super.onOptionsItemSelected(item);
}

  @Override
protected void onDestroy() {
    super.onDestroy();
    // 🔥 PERBAIKAN: Jangan panggil terminalSession.finishIfRunning() di sini!
    // Jika dipanggil di sini, setiap kali kamu keluar app, terminal MATI.
    
    if (mTerminalService != null) {
        unbindService(mServiceConnection);
        mTerminalService = null;
    }
}
}