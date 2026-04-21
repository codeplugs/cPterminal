package com.cpterminal.utils;

import android.view.KeyEvent;

import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;
import com.cpterminal.extrakeys.ExtraKeysView;
import com.cpterminal.extrakeys.ExtraKeysInfo;
/**
 * Translates logical key names (ESC, UP, …) into bytes / key events
 * to send to the current {@link TerminalSession}.
 * Shares a single {@link InputState} so CTRL toggling works correctly.
 */
public class KeyInputHelper {

    private final TerminalView terminalView;
    private final InputState inputState;
    private ExtraKeysView extraKeysView;
    private ExtraKeysInfo currentExtraKeysInfo; 
   public KeyInputHelper(TerminalView terminalView, InputState inputState) {
    this.terminalView = terminalView;
    this.inputState   = inputState;
}

    public void sendKey(String key) {
        TerminalSession s = terminalView.getCurrentSession();
        if (s == null) return;

        switch (key) {
            case "ESC":   s.write("\u001b"); break;
            case "TAB":   s.write("\t");     break;
            case "ENTER": s.write("\r");     break;
            case "UP":    dispatch(KeyEvent.KEYCODE_DPAD_UP);    break;
            case "DOWN":  dispatch(KeyEvent.KEYCODE_DPAD_DOWN);  break;
            case "LEFT":  dispatch(KeyEvent.KEYCODE_DPAD_LEFT);  break;
            case "RIGHT": dispatch(KeyEvent.KEYCODE_DPAD_RIGHT); break;
            case "HOME":  dispatch(KeyEvent.KEYCODE_MOVE_HOME);  break;
            case "END":   dispatch(KeyEvent.KEYCODE_MOVE_END);   break;
            case "PGUP":  s.write("\u001b[A"); break;
            case "PGDN":  s.write("\u001b[B"); break;
            case "/":     s.write("/");        break;
            case "-":     s.write("-");        break;
            default:
                
                break;
        }
        terminalView.updateSize();
        terminalView.scrollToBottom();
    }

    private void sendPrintable(TerminalSession s, String key) {
        if (inputState.isCtrlActive()) {
            char c = key.toUpperCase().charAt(0);
            s.write(new String(new char[]{(char) (c - 64)}));
            inputState.resetCtrl();
        } else {
            s.write(key);
        }
    }

    private void dispatch(int keyCode) {
        terminalView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
    }
}