package com.cpterminal.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;

import com.cpterminal.MainActivity;
import com.cpterminal.extrakeys.ExtraKeyButton;
import com.cpterminal.extrakeys.ExtraKeysConstants;
import com.cpterminal.extrakeys.ExtraKeysInfo;
import com.cpterminal.extrakeys.ExtraKeysView;
import com.cpterminal.utils.InputState;
import com.cpterminal.utils.KeyInputHelper;
import com.google.android.material.button.MaterialButton;
import com.termux.view.TerminalView;

import org.json.JSONException;

/**
 * Builds and wires the "extra keys" row (ESC, TAB, CTRL, arrows, …).
 */
public class ExtraKeysHandler {

    public static final String BUTTONS_JSON =
            "[[" +
            "  'ESC','/','-','HOME','UP','END','PGUP']," +
            "[ 'TAB'," +
            "  {key:'MY_CONTROL_KEY', display:'CTRL'}," +
            "  {key:'MY_ALT_KEY',     display:'ALT'}," +
            "  'LEFT','DOWN','RIGHT','PGDN']]";

    private static final int CTRL_ACTIVE_BG = Color.parseColor("#2196F3");

    private final MainActivity activity;
    private final TerminalView terminalView;
    public static ExtraKeysView extraKeysView;
    private final InputState inputState;
    private final KeyInputHelper keyHelper;

    private ExtraKeysInfo info;
    private MaterialButton ctrlButton;    // cached so we can refresh it later

    public ExtraKeysHandler(MainActivity activity,
                            TerminalView terminalView,
                            ExtraKeysView extraKeysView,
                            InputState inputState) {
        this.activity       = activity;
        this.terminalView   = terminalView;
        this.extraKeysView  = extraKeysView;
        this.inputState     = inputState;
        this.keyHelper      = new KeyInputHelper(terminalView, inputState);
    }

    public void init() {
        try {
            info = new ExtraKeysInfo(BUTTONS_JSON, "default",
                    ExtraKeysConstants.CONTROL_CHARS_ALIASES);
            extraKeysView.reload(info, 0);
            extraKeysView.setBackgroundColor(Color.BLACK);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        extraKeysView.setExtraKeysViewClient(new ExtraKeysView.IExtraKeysView() {
            @Override
            public void onExtraKeyButtonClick(View v, ExtraKeyButton b, MaterialButton btn) {
                handleClick(b.getKey(), btn);
            }
            @Override
            public boolean performExtraKeyButtonHapticFeedback(View v, ExtraKeyButton b, MaterialButton btn) {
                return true;
            }
        });
    }

    /** Resets the CTRL button visual state to match {@link InputState}. */
    public void refreshCtrlUi() {
        if (ctrlButton == null) return;
        activity.runOnUiThread(() -> applyCtrlAppearance(ctrlButton));
    }

    // ---------------- private ----------------

    private void handleClick(String key, MaterialButton btn) {
        switch (key) {
            case "MY_CONTROL_KEY":
                ctrlButton = btn;                 // cache for later refresh
                inputState.toggleCtrl();
                applyCtrlAppearance(btn);
                break;
            case "MY_ALT_KEY":
                inputState.toggleAlt();
                break;
            default:
                keyHelper.sendKey(key);
                break;
        }
    }

    private void applyCtrlAppearance(MaterialButton btn) {
        boolean active = inputState.isCtrlActive();
        btn.setBackgroundTintList(ColorStateList.valueOf(
                active ? CTRL_ACTIVE_BG : Color.TRANSPARENT));
        btn.setTextColor(active ? Color.BLUE : Color.WHITE);
    }
}