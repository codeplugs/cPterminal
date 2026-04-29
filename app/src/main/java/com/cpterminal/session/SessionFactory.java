package com.cpterminal.session;

import android.content.Context;

import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSession.SessionChangedCallback;

/**
 * Creates {@link TerminalSession} instances for Alpine, Devuan, and Android modes.
 */
public class SessionFactory {

    private final Context context;
    private final SessionChangedCallback callback;

    public SessionFactory(Context context, SessionChangedCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    public TerminalSession createAndroidSession() {
        TerminalSession s = new TerminalSession(
                "/system/bin/sh",
                context.getFilesDir().getAbsolutePath(),
                new String[0], new String[0], callback);
        s.mSessionName = "Android";
        return s;
    }

    /** Plain shell used only during Alpine installation. */
    public TerminalSession createAlpineSetupSession() {
        TerminalSession s = new TerminalSession(
                "/system/bin/sh",
                context.getFilesDir().getAbsolutePath(),
                new String[0], new String[0], callback);
        s.mSessionName = "Setup Alpine";
        return s;
    }

    /** Launches proot into the installed Alpine rootfs. */
    public TerminalSession createInstalledAlpineSession() {
        String home = context.getFilesDir().getAbsolutePath();
        String proot = home + "/usr/bin/proot.bin";
        String linker = "/system/bin/linker64";
        String loader = context.getApplicationInfo().nativeLibraryDir + "/libproot-loader.so";

        String[] args = {
                linker, proot,
                "-r", "/data/data/com.cpterminal/files/rootfs",
                "-0",
                "-b", "/dev", "-b", "/proc", "-b", "/sys",
                "-b", "/sdcard", "-b", "/storage",
                "-w", "/root",
                "/bin/sh", "-c", "cat /etc/motd; exec /bin/sh -l"
        };

        String[] env = {
                "PROOT_TMP_DIR=/data/data/com.cpterminal/files/tmp",
                "LD_LIBRARY_PATH=/data/data/com.cpterminal/files/usr/lib",
                "PROOT_LOADER=" + loader,
                "HOME=/root",
                "TERM=xterm-256color",
                "PATH=/usr/bin:/bin:/usr/sbin:/sbin"
        };

        TerminalSession s = new TerminalSession(linker, home, args, env, callback);
        s.mSessionName = "Alpine";
        return s;
    }

 public TerminalSession devsetup() {
        TerminalSession s = new TerminalSession(
                "/system/bin/sh",
                context.getFilesDir().getAbsolutePath(),
                new String[0], new String[0], callback);
        s.mSessionName = "Setup Devuan";
        return s;
    }
	
 public TerminalSession devinstall() {
        String home = context.getFilesDir().getAbsolutePath();
        String proot = home + "/usr/bin/proot.bin";
        String linker = "/system/bin/linker64";
        String loader = context.getApplicationInfo().nativeLibraryDir + "/libproot-loader.so";

        String[] args = {
                linker, proot,
                "-r", "/data/data/com.cpterminal/files/rootfs",
                "-0",
                "-b", "/dev", "-b", "/proc", "-b", "/sys",
                "-b", "/sdcard", "-b", "/storage",
                "-w", "/root",
                "/bin/sh", "-c", "cat /etc/motd; exec /bin/sh -l"
        };

        String[] env = {
                "PROOT_TMP_DIR=/data/data/com.cpterminal/files/tmp",
                "LD_LIBRARY_PATH=/data/data/com.cpterminal/files/usr/lib",
                "PROOT_LOADER=" + loader,
                "HOME=/root",
                "TERM=xterm-256color",
                "PATH=/usr/bin:/bin:/usr/sbin:/sbin"
        };

        TerminalSession s = new TerminalSession(linker, home, args, env, callback);
        s.mSessionName = "Devuan";
        return s;
    }



    // --- TAMBAHAN DEVUAN ---

    /** Plain shell used only during Devuan installation. */
    public TerminalSession createDevuanSetupSession() {
        TerminalSession s = new TerminalSession(
                "/system/bin/sh",
                context.getFilesDir().getAbsolutePath(),
                new String[0], new String[0], callback);
        s.mSessionName = "Setup Devuan";
        return s;
    }










    /** Launches proot into Devuan with symlink and sysvipc support */
    public TerminalSession createInstalledDevuanSession() {
        String home = context.getFilesDir().getAbsolutePath();
        String proot = home + "/usr/bin/proot.bin";
        String linker = "/system/bin/linker64";
        String loader = context.getApplicationInfo().nativeLibraryDir + "/libproot-loader.so";
        String rootfs = "/data/data/com.cpterminal/files/rootfsdevuan";

        String[] args = {
                linker, proot,
                "--link2symlink", // penting buat Devuan/Debian
                "--sysvipc", // enable shared memory
                "-0",
                "-r", rootfs,
                "-b", "/dev",
                "-b", "/proc",
                "-b", "/sys",
                "-b", "/sdcard",
                "-b", "/storage",
                "-b", "/apex", // Android 10+
                "-w", "/root",
               "/bin/sh", "-c", "cat /etc/motd; exec /bin/sh -l"
        };

        String[] env = {
                "PROOT_TMP_DIR=/data/data/com.cpterminal/files/tmp",
                "LD_LIBRARY_PATH=/data/data/com.cpterminal/files/usr/lib",
                "PROOT_LOADER=" + loader,
                "HOME=/root",
                "TERM=xterm-256color",
                "LANG=C.UTF-8",
                "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        };

        TerminalSession s = new TerminalSession(linker, home, args, env, callback);
        s.mSessionName = "Devuan";
        return s;
    }
}