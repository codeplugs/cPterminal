package com.cpterminal.session;

import android.content.Intent;
import android.widget.Toast;
import android.os.Looper;
import android.os.Handler;

import com.cpterminal.MainActivity;
import com.cpterminal.TerminalService;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSession.SessionChangedCallback;
import com.termux.view.TerminalView;
import com.cpterminal.session.DevuanInstaller;
import java.io.File;

public class SessionController {

    private final MainActivity activity;
    private final TerminalService service;
    private final TerminalView terminalView;
    private final SessionFactory factory;
    private final SessionChangedCallback callback;

    private int sessionCounter = 0;
    private int lastEnvIndex   = 0; // 0=Alpine, 1=Android, 2=Devuan
    private String selectedTitle = "Alpine";
    private TerminalSession current;

    public SessionController(MainActivity activity, TerminalService service,
                             TerminalView terminalView, SessionChangedCallback callback) {
		this.lastEnvIndex = activity.prefs.getInt("LAST_ENV_INDEX", 0); 
        this.activity = activity;
        this.service = service;
        this.terminalView = terminalView;
        this.callback = callback;
        this.factory = new SessionFactory(activity, callback);
        
        // --- FIX 1: load last mode, tapi kalau Devuan udah kepasang, paksa Devuan ---
        File devMarker = new File(activity.getFilesDir(), "DevuanInstalled");
        if (devMarker.exists()) {
            lastEnvIndex = 2;
            selectedTitle = "Devuan";
        } else {
            lastEnvIndex = activity.prefs.getInt("LAST_ENV_INDEX", 0);
            selectedTitle = activity.prefs.getString("LAST_TITLE", "Alpine");
        }
    }
	
	

    public void attachExisting(TerminalSession existing) {
        switchSession(existing);
    }

    public void startAlpine() {
        current = factory.createInstalledAlpineSession();
        selectedTitle = "Alpine";
        lastEnvIndex = 0;
        saveState(); // --- FIX 2
        register(current);
    }

    public void startAlpineSetup() {
        current = factory.createAlpineSetupSession();
        selectedTitle = "Setup Alpine";
        lastEnvIndex = 0;
        saveState();
        register(current);
    }

    public void startDevuan() {
        current = factory.createInstalledDevuanSession();
        selectedTitle = "Devuan";
        lastEnvIndex = 2;
        saveState(); // --- FIX 2
        register(current);
    }

    public void startDevuanSetup() {
        current = factory.createDevuanSetupSession();
        selectedTitle = "Setup Devuan";
        lastEnvIndex = 2;
        saveState();
        register(current);
    }

    public void switchSession(TerminalSession session) {
        if (session == null) return;
        selectedTitle = session.mSessionName;

        if (session.mSessionName.contains("Alpine")) {
            lastEnvIndex = 0;
        } else if (session.mSessionName.contains("Devuan")) {
            lastEnvIndex = 2;
        } else if (session.mSessionName.contains("Android")) {
            lastEnvIndex = 1;
        }
        
        saveState(); // --- FIX 2
        
        current = session;
        session.updateCallback(callback);
        
        if (service != null) {
            int idx = service.mTerminalSessions.indexOf(session);
            if (idx != -1) service.setCurrentSessionIndex(idx);
        }
        
        terminalView.attachSession(session);
    }

    public void addNewSession() {
		lastEnvIndex = activity.prefs.getInt("LAST_ENV_INDEX", lastEnvIndex); 
        sessionCounter = getMaxSessionId() + 1;
        TerminalSession s;
        
        File devMarker = new File(activity.getFilesDir(), "DevuanInstalled");
        
        if (lastEnvIndex == 0) {
            s = factory.createInstalledAlpineSession();
        } else if (lastEnvIndex == 2) { 
            if (!devMarker.exists()) {
                activity.prefs.edit().putInt("INSTALL_STAGE", 2).apply();
                startDevuanSetup();
                activity.getDialogHelper().showLoading("Setup Devuan...");
                
               // JALANKAN DI THREAD BACKGROUND, JANGAN DI UI
new Thread(() -> {
    try { Thread.sleep(800); } catch (InterruptedException ignored) {}
    new DevuanInstaller(activity, this).runSetup(devMarker);
}).start();
                return;
            }
            s = factory.createInstalledDevuanSession();
        } else {
            s = factory.createAndroidSession();
        }
        
        s.mSessionId = sessionCounter;
        service.registerSession(s);
        switchSession(s);
        Toast.makeText(activity, "New " + s.mSessionName + " Session", Toast.LENGTH_SHORT).show();
    }

    // ... sisanya sama persis ...
    public void handleSessionExit(TerminalSession session) {
        service.removeSession(session);
        if (service.mTerminalSessions.isEmpty()) {
            activity.stopService(new Intent(activity, TerminalService.class));
            activity.finish();
            return;
        }
        if (session == current) switchSession(service.getLastSession());
    }

    public void sendToAlpine(String command) {
        if (service == null || service.mTerminalSessions.isEmpty()) return;
        if (!selectedTitle.contains("Alpine")) return;
        TerminalSession s = findFirstSessionByName("Setup Alpine");
        if (s == null) s = findFirstSessionByName("Alpine");
        if (s != null && s.isRunning()) s.write(command + "\r");
    }

    public void sendToDevuan(String command) {
        if (service == null || service.mTerminalSessions.isEmpty()) return;
        if (!selectedTitle.contains("Devuan")) return;
        TerminalSession s = findFirstSessionByName("Setup Devuan");
        if (s == null) s = findFirstSessionByName("Devuan");
        if (s != null && s.isRunning()) s.write(command + "\r");
    }

   public void switchToAlpineProper() {
    new Handler(Looper.getMainLooper()).post(() -> {
        activity.getDialogHelper().dismissLoading();
        
        if (current != null && current.mSessionName != null && current.mSessionName.contains("Setup")) {
            current.finishIfRunning();
            service.removeSession(current);
        }
        startAlpine();
    });
}

public void switchToDevuanProper() {
    new Handler(Looper.getMainLooper()).post(() -> {
        activity.getDialogHelper().dismissLoading();
        
        if (current != null && current.mSessionName != null && current.mSessionName.contains("Setup")) {
            current.finishIfRunning();
            service.removeSession(current);
        }
        
        activity.prefs.edit().putInt("LAST_ENV_INDEX", 2).putString("LAST_TITLE","Devuan").apply();
        startDevuan();
    });
}

   private void register(TerminalSession s) {
    if (service == null || terminalView == null || s == null) return;
    service.registerSession(s);
    service.setCurrentSessionIndex(service.mTerminalSessions.size() - 1);
    terminalView.attachSession(s);
    // INI KUNCINYA BIAR SERVICE GAK MATI
    activity.startService(new Intent(activity, TerminalService.class));
}

    private int getMaxSessionId() {
        int max = 0;
        for (TerminalSession s : service.mTerminalSessions)
            if (s.mSessionId > max) max = s.mSessionId;
        return max;
    }

    private TerminalSession findFirstSessionByName(String name) {
        for (TerminalSession s : service.mTerminalSessions) {
            if (s.mSessionName != null && s.mSessionName.equals(name)) return s;
        }
        return null;
    }

    // --- helper baru ---
    private void saveState() {
        activity.prefs.edit()
                .putInt("LAST_ENV_INDEX", lastEnvIndex)
                .putString("LAST_TITLE", selectedTitle)
                .apply();
    }

    public TerminalSession getCurrent()   { return current; }
    public String getSelectedTitle()      { return selectedTitle; }
    public int getLastEnvIndex() {
    return activity.prefs.getInt("LAST_ENV_INDEX", 0); // jangan return variabel memory
}
    public void setLastEnvIndex(int i)    { 
        lastEnvIndex = i; 
        saveState();
    }
}