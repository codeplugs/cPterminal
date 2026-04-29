package com.cpterminal.terminal;

import com.cpterminal.MainActivity;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSession.SessionChangedCallback;
import com.termux.view.TerminalView;

/**
 * Forwards session events to the activity.
 */
public class SessionCallbackImpl implements SessionChangedCallback {

    private static final String INSTALL_DONE_MARKER = "CP_INSTALL_SUCCESS";

    private final MainActivity activity;
    private final TerminalView terminalView;

    public SessionCallbackImpl(MainActivity activity, TerminalView terminalView) {
        this.activity = activity;
        this.terminalView = terminalView;
    }

  @Override
    public void onTextChanged(TerminalSession session) {
        String text = session.getEmulator().getScreen().getTranscriptText();
        
        // Cek apakah ada sinyal sukses dari script installer
        if (text.contains(INSTALL_DONE_MARKER)) {
            String title = activity.getController().getSelectedTitle();
            
            if ("Setup Alpine".equals(title)) {
                activity.runOnUiThread(() -> activity.getController().switchToAlpineProper());
            } 
            // --- TAMBAHAN UNTUK DEVUAN ---
            else if ("Setup Devuan".equals(title)) {
                activity.runOnUiThread(() -> activity.getController().switchToDevuanProper());
            }
        }
        
        activity.runOnUiThread(terminalView::invalidate);
    }
	
    @Override
    public void onSessionFinished(TerminalSession session) {
        activity.runOnUiThread(() -> {
            activity.getController().handleSessionExit(session);
            activity.updateSessionBadge();
        });
    }

    @Override public void onTitleChanged(TerminalSession session) {}
    @Override public void onClipboardText(TerminalSession session, String text) {}
    @Override public void onBell(TerminalSession session) {}
    @Override public void onColorsChanged(TerminalSession session) {}
}