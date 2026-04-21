package com.cpterminal;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.cpterminal.utils.InputState;
import com.cpterminal.session.AlpineInstaller;
import com.cpterminal.session.SessionController;
import com.cpterminal.session.SessionFactory;
import com.cpterminal.terminal.SessionCallbackImpl;
import com.cpterminal.terminal.TerminalClientImpl;
import com.cpterminal.ui.DialogHelper;
import com.cpterminal.ui.ExtraKeysHandler;
import com.cpterminal.ui.SessionListDialog;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;

import java.io.File;

/**
 * Main activity — orchestrates UI, service binding and delegates logic to helpers.
 */
public class MainActivity extends AppCompatActivity {

    private static final float TERMINAL_TEXT_SP = 13;
    private static final String FONT_PATH = "fonts/JetBrainsMono_Regular.ttf";

    private TerminalView terminalView;
    private TextView sessionBadgeTxt;
    private InputState inputState;
    private ExtraKeysHandler extraKeysHandler;
    private TerminalService terminalService;
    private SessionController sessionController;
    private DialogHelper dialogHelper;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            terminalService = ((TerminalService.TerminalServiceBinder) service).getService();
            sessionController = new SessionController(
                    MainActivity.this, terminalService, terminalView,
                    new SessionCallbackImpl(MainActivity.this, terminalView));

            bootstrapSession();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            terminalService = null;
        }
    };

    // ---------------- lifecycle ----------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ensureFilesDir();
		inputState = new InputState();   
        setupTerminalView();
        setupExtraKeys();
        dialogHelper = new DialogHelper(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, TerminalService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
        else startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (terminalService != null) {
            unbindService(serviceConnection);
            terminalService = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler(Looper.getMainLooper()).postDelayed(this::updateSessionBadge, 200);
    }

    // ---------------- setup ----------------
    private void ensureFilesDir() {
        File dir = getFilesDir();
        if (!dir.exists()) dir.mkdirs();
    }

    private void setupTerminalView() {
    terminalView = findViewById(R.id.terminal_view);
    float sizeInSp = TERMINAL_TEXT_SP;
    float selectedSize = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_SP, 
    sizeInSp, 
    getResources().getDisplayMetrics()
    );
    terminalView.setTextSize((int) selectedSize);
    terminalView.setTypeface(Typeface.createFromAsset(getAssets(), FONT_PATH));
    terminalView.setFocusable(true);
    terminalView.setFocusableInTouchMode(true);
    terminalView.requestFocus();
    terminalView.setOnKeyListener(
            (TerminalViewClient) new TerminalClientImpl(this, terminalView, inputState));
}

  private void setupExtraKeys() {
    extraKeysHandler = new ExtraKeysHandler(
            this, terminalView, findViewById(R.id.extra_keys), inputState);
    extraKeysHandler.init();
}



    private void bootstrapSession() {
        TerminalSession existing = terminalService.getActiveSession();
        File marker = new File(getFilesDir(), "AlpineInstalled");

        if (existing != null) {
            sessionController.attachExisting(existing);
        } else if (marker.exists()) {
            sessionController.startAlpine();
        } else {
            sessionController.startAlpineSetup();
            dialogHelper.showLoading("Setup Alpine Linux...\nPlease wait.");
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> new AlpineInstaller(this, sessionController).runSetup(marker),
                    1000);
        }
    }

    // ---------------- menu ----------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem item = menu.findItem(R.id.action_show_sessions);
        View actionView = item.getActionView();
        sessionBadgeTxt = actionView.findViewById(R.id.session_count_txt);
        actionView.setOnClickListener(v -> {
            new SessionListDialog(this, terminalService, sessionController).show();
            updateSessionBadge();
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_switch) { dialogHelper.showRadioDialog(); return true; }
        if (id == R.id.action_info)   { dialogHelper.showAbout();       return true; }
        if (id == R.id.action_add)    {  
		new Handler(Looper.getMainLooper()).postDelayed(() -> {
        sessionController.addNewSession();
        updateSessionBadge(); // Masukkan ke dalam sini sekalian agar update setelah session baru muncul
    }, 200);
    return true; }
        return super.onOptionsItemSelected(item);
    }

    // ---------------- helpers ----------------
    public void updateSessionBadge() {
        if (sessionBadgeTxt == null || terminalService == null) return;
        runOnUiThread(() -> sessionBadgeTxt.setText(
                String.valueOf(terminalService.mTerminalSessions.size())));
    }

    public InputState        getInputState()        { return inputState; }
    public ExtraKeysHandler  getExtraKeysHandler()  { return extraKeysHandler; }
    public DialogHelper      getDialogHelper()      { return dialogHelper; }
    public SessionController getController()        { return sessionController; }
    public TerminalView      getTerminalView()      { return terminalView; }
	
	
}