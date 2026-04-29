package com.cpterminal.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.cpterminal.BuildConfig;
import com.cpterminal.MainActivity;

/**
 * Centralises construction of the custom AlertDialogs used by the app.
 */
public class DialogHelper {

    private static final int DIALOG_BG = Color.parseColor("#3605B0");

    private final MainActivity activity;
    private AlertDialog loadingDialog;

    public DialogHelper(MainActivity activity) {
        this.activity = activity;
    }

    // -- Loading --
    public void showLoading(String message) {
        TextView tv = new TextView(activity);
        tv.setText(message);
        tv.setPadding(50, 50, 50, 50);
        tv.setTextSize(18);
        tv.setTextColor(Color.WHITE);
        tv.setTypeface(Typeface.MONOSPACE);

        loadingDialog = new AlertDialog.Builder(activity)
                .setView(tv).setCancelable(false).create();
        if (loadingDialog.getWindow() != null)
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(DIALOG_BG));
        loadingDialog.show();
    }

    public void dismissLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
    }

    // -- About --
    public void showAbout() {
        String msg = "cP Terminal\n\n" +
                "Version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n\n" +
                "Lib path: " + activity.getApplicationInfo().nativeLibraryDir + "\n\n" +
                "Developer: Codeplug\n© 2026 All Rights Reserved\n\nTerminal Android.\n";

        TextView view = new TextView(activity);
        view.setText(msg);
        view.setTextColor(Color.WHITE);
        view.setPadding(50, 30, 50, 30);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Info App")
                .setView(view)
                .setPositiveButton("Copy", (d, w) -> copyToClipboard(msg))
                .create();

        dialog.setOnShowListener(d -> styleDialog(dialog));
        dialog.show();
    }

    private void copyToClipboard(String text) {
        ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("About Info", text));
        Toast.makeText(activity, "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void styleDialog(AlertDialog dialog) {
        TextView title = dialog.findViewById(
                activity.getResources().getIdentifier("alertTitle", "id", "android"));
        if (title != null) title.setTextColor(Color.WHITE);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(DIALOG_BG));
    }

    // -- Radio mode picker --
   public void showRadioDialog() {
    final String[] options = {"Alpine","Android", "Devuan"};

    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            activity, android.R.layout.simple_list_item_single_choice, options) {
        @Override public View getView(int p, View c, ViewGroup g) {
            View v = super.getView(p, c, g);
            TextView t = v.findViewById(android.R.id.text1);
            t.setTextColor(Color.WHITE);
            t.setTypeface(Typeface.MONOSPACE);
            v.setBackgroundColor(DIALOG_BG);
            return v;
        }
    };

    TextView title = new TextView(activity);
    title.setText("Pilih Mode");
    title.setTextColor(Color.WHITE);
    title.setTextSize(20);
    title.setPadding(40, 40, 40, 20);

    AlertDialog dialog = new AlertDialog.Builder(activity)
            .setCustomTitle(title)
            .setSingleChoiceItems(adapter,
                    activity.getController().getLastEnvIndex(),
                    (d, which) -> {
                        activity.getController().setLastEnvIndex(which);
                        d.dismiss(); // tutup langsung
                        activity.getController().addNewSession(); // <-- LANGSUNG BIKIN SESSION
                    })
            .create();

    if (dialog.getWindow() != null)
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(DIALOG_BG));
    
    dialog.show(); // <-- CUMA SEKALI
}
}