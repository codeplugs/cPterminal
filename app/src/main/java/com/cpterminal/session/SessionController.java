package com.cpterminal.session;

import android.content.Intent;
import android.widget.Toast;

import com.cpterminal.MainActivity;
import com.cpterminal.TerminalService;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSession.SessionChangedCallback;
import com.termux.view.TerminalView;

/**
 * Handles creating, switching and removing terminal sessions.
 */
public class SessionController {

    private final MainActivity activity;
    private final TerminalService service;
    private final TerminalView terminalView;
    private final SessionFactory factory;
    private final SessionChangedCallback callback;

    private int sessionCounter = 0;
    private int lastEnvIndex   = 0;
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

    // -- Initial boot --
    public void attachExisting(TerminalSession existing) {
        switchSession(existing);
    }

    public void startAlpine() {
        current = factory.createInstalledAlpineSession();
        selectedTitle = "Alpine";
        register(current);
    }

    public void startAlpineSetup() {
        current = factory.createAlpineSetupSession();
        selectedTitle = "Setup Alpine";
        register(current);
    }

    // -- Session operations --
    public void switchSession(TerminalSession session) {
        if (session == null) return;
        selectedTitle = "Alpine".equals(session.mSessionName) ? "Alpine" : "Android";
        lastEnvIndex  = "Alpine".equals(session.mSessionName) ? 0 : 1;
        current = session;
        session.updateCallback(callback);
        int idx = service.mTerminalSessions.indexOf(session);
        if (idx != -1) service.setCurrentSessionIndex(idx);
        terminalView.attachSession(session);
    }

    public void addNewSession() {
        sessionCounter = getMaxSessionId() + 1;
        TerminalSession s = (lastEnvIndex == 0)
                ? factory.createInstalledAlpineSession()
                : factory.createAndroidSession();
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

    // -- Helpers --
    public void sendToAlpine(String command) {
        if (service == null || service.mTerminalSessions.isEmpty()) return;
        if (!selectedTitle.startsWith("Alpine") && !selectedTitle.equals("Setup Alpine")) return;
        TerminalSession s = service.mTerminalSessions.get(0);
        if (s != null && s.isRunning()) s.write(command + "\r");
    }

    public void switchToAlpineProper() {
        activity.getDialogHelper().dismissLoading();
        if (current != null) current.finishIfRunning();
        service.mTerminalSessions.clear();
        startAlpine();
        terminalView.attachSession(current);
        Toast.makeText(activity, "Alpine Linux Ready!", Toast.LENGTH_SHORT).show();
    }

    // -- Private --
    private void register(TerminalSession s) {
        service.registerSession(s);
        service.setCurrentSessionIndex(0);
        terminalView.attachSession(s);
    }

    private int getMaxSessionId() {
        int max = 0;
        for (TerminalSession s : service.mTerminalSessions)
            if (s.mSessionId > max) max = s.mSessionId;
        return max;
    }

    // -- Getters --
    public TerminalSession getCurrent()   { return current; }
    public String getSelectedTitle()      { return selectedTitle; }
    public int getLastEnvIndex()          { return lastEnvIndex; }
    public void setLastEnvIndex(int i)    { lastEnvIndex = i; }
}