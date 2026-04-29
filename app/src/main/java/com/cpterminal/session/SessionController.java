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
import com.cpterminal.session.DevuanInstaller; // Pastikan import ini benar
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
        this.activity = activity;
        this.service = service;
        this.terminalView = terminalView;
        this.callback = callback;
        this.factory = new SessionFactory(activity, callback);
    }

    public void attachExisting(TerminalSession existing) {
        switchSession(existing);
    }

    public void startAlpine() {
        current = factory.createInstalledAlpineSession();
        selectedTitle = "Alpine";
        lastEnvIndex = 0;
        register(current);
    }

    public void startAlpineSetup() {
        current = factory.createAlpineSetupSession();
        selectedTitle = "Setup Alpine";
        lastEnvIndex = 0;
        register(current);
    }

    public void startDevuan() {
        current = factory.createInstalledDevuanSession();
        selectedTitle = "Devuan";
        lastEnvIndex = 2;
        register(current);
    }

    public void startDevuanSetup() {
        current = factory.createDevuanSetupSession();
        selectedTitle = "Setup Devuan";
        lastEnvIndex = 2;
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
        
        current = session;
        session.updateCallback(callback);
        
        if (service != null) {
            int idx = service.mTerminalSessions.indexOf(session);
            if (idx != -1) service.setCurrentSessionIndex(idx);
        }
        
        terminalView.attachSession(session);
    }

    public void addNewSession() {
        sessionCounter = getMaxSessionId() + 1;
        TerminalSession s;
        
        if (lastEnvIndex == 0) {
            s = factory.createInstalledAlpineSession();
        } else if (lastEnvIndex == 2) { 
            File devMarker = new File(activity.getFilesDir(), "DevuanInstalled");
            if (!devMarker.exists()) {
				activity.prefs.edit().putInt("INSTALL_STAGE", 2).apply();
                startDevuanSetup();
                activity.getDialogHelper().showLoading("Setup Devuan...");
                
                // --- PERBAIKAN DI SINI ---
                // Sebelumnya kamu panggil AlpineInstaller, harusnya DevuanInstaller
                new Handler(Looper.getMainLooper()).postDelayed(
                        () -> new DevuanInstaller(activity, this).runSetup(devMarker),
                        1000);
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
        
        // Pengecekan Title yang lebih luwes
        if (!selectedTitle.contains("Devuan")) return;
        
        // Cari session yang relevan
        TerminalSession s = findFirstSessionByName("Setup Devuan");
        if (s == null) s = findFirstSessionByName("Devuan");
        
        if (s != null && s.isRunning()) {
            // Hapus 'fffffff' karena itu bikin command Linux error/bengong
            s.write(command + "\r");
        }
    }

    public void switchToAlpineProper() {
        activity.getDialogHelper().dismissLoading();
        if (current != null) current.finishIfRunning();
        service.mTerminalSessions.clear();
        startAlpine();
    }

    public void switchToDevuanProper() {
        activity.getDialogHelper().dismissLoading();
        if (current != null) current.finishIfRunning();
        //service.mTerminalSessions.clear();
        startDevuan();
    }

    private void register(TerminalSession s) {
        service.registerSession(s);
        service.setCurrentSessionIndex(service.mTerminalSessions.size() - 1);
        terminalView.attachSession(s);
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

    public TerminalSession getCurrent()   { return current; }
    public String getSelectedTitle()      { return selectedTitle; }
    public int getLastEnvIndex()          { return lastEnvIndex; }
    public void setLastEnvIndex(int i)    { lastEnvIndex = i; }
}