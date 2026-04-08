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

import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalEmulator;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;
import com.termux.terminal.TerminalSession.SessionChangedCallback;


public class MainActivity extends AppCompatActivity {
 private boolean keyboardVisible = false; // tambahkan ini
    private TerminalView terminalView;
    private TerminalSession terminalSession;

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



// root layout dari activity
final View rootView = findViewById(R.id.rootLayout);

/*rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
    Rect r = new Rect();
    rootView.getWindowVisibleDisplayFrame(r);
    int screenHeight = rootView.getRootView().getHeight();
    int keypadHeight = screenHeight - r.bottom;

    // keypadHeight > 0 berarti keyboard muncul
    keyboardVisible = keypadHeight > 0;
});*/






   

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
		session.writeCodePoint(e.isCtrlPressed(), e.getUnicodeChar());
        return true;
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
       session.writeCodePoint(ctrlDown, codePoint);
        return true;
    }
};

terminalView.setOnKeyListener(client);

TerminalSession.SessionChangedCallback callback =
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

        // Path shell
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
                });

        // ✅ Attach session setelah view siap
        terminalView.post(() -> terminalView.attachSession(session));
		
		 
    }

		
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (terminalSession != null) {
            terminalSession.finishIfRunning();
            terminalSession = null;
        }
    }
}