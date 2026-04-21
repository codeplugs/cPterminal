package com.cpterminal.utils;

/**
 * Tracks modifier key state (CTRL, ALT) shared across the app.
 */
public class InputState {

    public boolean ctrlActive = false;
    private boolean altActive  = false;

    public boolean isCtrlActive() { return ctrlActive; }
    public boolean isAltActive()  { return altActive; }

    public void toggleCtrl() { ctrlActive = !ctrlActive; }
    public void toggleAlt()  { altActive  = !altActive;  }

    public void resetCtrl()  { ctrlActive = false; }
    public void resetAlt()   { altActive  = false; }

    /** Reads and clears CTRL in one step (one-shot semantics). */
    public boolean consumeCtrl() {
        boolean was = ctrlActive;
        ctrlActive = false;
        return was;
    }

    /** Reads and clears ALT in one step. */
    public boolean consumeAlt() {
        boolean was = altActive;
        altActive = false;
        return was;
    }

    /** Convenience: clears all one-shot modifiers. */
    public void consume() {
        ctrlActive = false;
        altActive  = false;
    }
}