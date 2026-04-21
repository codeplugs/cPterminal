package com.cpterminal.ui;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.content.Context;

import com.cpterminal.MainActivity;
import com.cpterminal.R;
import com.cpterminal.TerminalService;
import com.cpterminal.session.SessionController;
import com.google.android.material.button.MaterialButton;
import com.termux.terminal.TerminalSession;

import java.util.List;

/**
 * Displays the list of active terminal sessions with switch / close actions.
 */
public class SessionListDialog {

    private final MainActivity activity;
    private final TerminalService service;
    private final SessionController controller;
    private AlertDialog dialog;
    private ArrayAdapter<TerminalSession> adapter;

    public SessionListDialog(MainActivity activity, TerminalService service,
                             SessionController controller) {
        this.activity = activity;
        this.service = service;
        this.controller = controller;
    }

    public void show() {
        List<TerminalSession> sessions = service.mTerminalSessions;
        adapter = buildAdapter(sessions);

        TextView title = new TextView(activity);
        title.setText("Active Sessions");
        title.setTextColor(Color.WHITE);
        title.setPadding(40, 40, 40, 20);
        title.setTextSize(18);

        dialog = new AlertDialog.Builder(activity)
                .setCustomTitle(title)
                .setAdapter(adapter, null)
                .create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.parseColor("#3605B0")));
            int w = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.85);
            int h = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.60);
            dialog.getWindow().setLayout(w, h);
            dialog.getWindow().setGravity(Gravity.CENTER);
            dialog.getWindow().setDimAmount(0.5f);
        }
    }

  private ArrayAdapter<TerminalSession> buildAdapter(List<TerminalSession> sessions) {
    return new ArrayAdapter<TerminalSession>(activity, R.layout.item_session, sessions) {
        @Override
        public View getView(int pos, View cv, ViewGroup parent) {
            if (cv == null) {
                // Wrap context with a Material theme so MaterialButton can inflate correctly
                Context themed = new androidx.appcompat.view.ContextThemeWrapper(
                        getContext(),
                        com.google.android.material.R.style.Theme_MaterialComponents_DayNight);
                cv = LayoutInflater.from(themed)
                        .inflate(R.layout.item_session, parent, false);
            }

            TerminalSession s = getItem(pos);
            TextView name        = cv.findViewById(R.id.txtSessionName);
            MaterialButton close = cv.findViewById(R.id.btnCloseSession);
            close.setRippleColor(ColorStateList.valueOf(Color.TRANSPARENT));

            boolean isCurrent = (s == controller.getCurrent());
            close.setVisibility(isCurrent ? View.GONE : View.VISIBLE);
            cv.setBackgroundColor(isCurrent
                    ? Color.parseColor("#4000FFFF") : Color.TRANSPARENT);
            name.setTextColor(isCurrent ? Color.parseColor("#00FFFF") : Color.WHITE);
            name.setTypeface(null, isCurrent ? Typeface.BOLD : Typeface.NORMAL);
            name.setText((s.mSessionId + 1) + ". " +
                    (s.mSessionName != null ? s.mSessionName : "Session"));

            cv.setOnClickListener(v -> {
                controller.switchSession(s);
                notifyDataSetChanged();
            });
            close.setOnClickListener(v -> {
                controller.handleSessionExit(s);
                activity.updateSessionBadge();
                if (sessions.isEmpty()) dialog.dismiss();
                else notifyDataSetChanged();
            });
            return cv;
        }
    };
}
}