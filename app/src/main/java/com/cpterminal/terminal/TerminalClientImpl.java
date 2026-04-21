package com.cpterminal.terminal;

import android.content.Context;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;

import com.cpterminal.MainActivity;
import com.cpterminal.utils.InputState;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;
import com.cpterminal.extrakeys.ExtraKeysView;
import com.cpterminal.extrakeys.ExtraKeysInfo;
import com.cpterminal.extrakeys.ExtraKeysConstants;
import com.cpterminal.ui.ExtraKeysHandler;

import org.json.JSONException;

/**
 * Handles input events and forwards them to the terminal session.
 */
public class TerminalClientImpl implements TerminalViewClient {

    private final MainActivity activity;
    private final TerminalView terminalView;
    private final InputState   inputState;
    private ExtraKeysView extraKeysView;
    private ExtraKeysInfo currentExtraKeysInfo; 
    public TerminalClientImpl(MainActivity activity,
                              TerminalView terminalView,
                              InputState inputState) {
        this.activity     = activity;
        this.terminalView = terminalView;
        this.inputState   = inputState;
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
     inputState.resetCtrl();
	 String btnjs = activity.getExtraKeysHandler().BUTTONS_JSON;
	  
	  
	   try {
            ExtraKeysInfo info = new ExtraKeysInfo(btnjs, "default",
                    ExtraKeysConstants.CONTROL_CHARS_ALIASES);
    activity.runOnUiThread(() -> ExtraKeysHandler.extraKeysView.reload(info, 0));
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
	  
	 
}
    @Override
    public boolean onCodePoint(int codePoint, boolean ctrlDown, TerminalSession session) {
        if (session == null) return false;

        boolean effectiveCtrl = ctrlDown || inputState.isCtrlActive();
        
		
		if (inputState.isCtrlActive()) {
		
        sendControlKey(session, codePoint); // Panggil fungsi master
        inputState.consume();                           // one-shot modifier
            activity.runOnUiThread(() ->
                    activity.getExtraKeysHandler().refreshCtrlUi());
    }else{
		session.writeCodePoint(ctrlDown, codePoint);
	}

        terminalView.post(() -> {
            terminalView.updateSize();
            terminalView.scrollToBottom();
        });
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e, TerminalSession session) {
        terminalView.post(() -> {
            terminalView.updateSize();
            terminalView.scrollToBottom();
        });
        if (keyCode == KeyEvent.KEYCODE_ENTER && session != null && !session.isRunning()) {
            activity.getController().handleSessionExit(session);
            return true;
        }
        return false;
    }

    @Override
    public void onSingleTapUp(MotionEvent e) {
        terminalView.requestFocus();
        InputMethodManager imm = (InputMethodManager)
                activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(terminalView, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override public boolean onLongPress(MotionEvent e)         { return false; }
    @Override public float   onScale(float scale)               { return scale; }
    @Override public boolean onKeyUp(int keyCode, KeyEvent e)   { return false; }
    @Override public boolean readControlKey()                   { return inputState.isCtrlActive(); }
    @Override public boolean readAltKey()                       { return inputState.isAltActive();  }
    @Override public boolean shouldBackButtonBeMappedToEscape() { return false; }
    @Override public void    copyModeChanged(boolean copyMode)  {}
}