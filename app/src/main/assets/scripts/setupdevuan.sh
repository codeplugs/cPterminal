#!/system/bin/sh

echo "[*] Setting up proot command..."

BASE_DIR="$(pwd)"
LINKER="/system/bin/linker64"
PROOT="$BASE_DIR/usr/bin/proot.bin"
LIB_PATH="$BASE_DIR/rootfs/usr/lib"
USR_DIR="$BASE_DIR/usr"
BIN_DIR="$USR_DIR/bin"
LIB_DIR="$USR_DIR/lib"

mkdir -p "$BIN_DIR" "$LIB_DIR"



# ===== Download binary =====
echo "[*] Downloading proot..."
curl -f -L "https://dl.dropboxusercontent.com/scl/fi/3pb3f08lap9vta5t01bvn/xz?rlkey=0m3sx2whel4jaamqs1bqyhe1k&st=8u62hyp1&raw=1" -o "$BIN_DIR/xz"
chmod 755 "$BIN_DIR/xz"

# ===== Download library =====
echo "[*] Downloading libtalloc..."
curl -f -L "https://dl.dropboxusercontent.com/scl/fi/95t80nwkdplinjirkyld9/liblzma.so.5?rlkey=40x2zvya5pg13gi0kud7quh1e&st=o4bqbs1n&raw=1" -o "$LIB_DIR/liblzma.so.5"
chmod 755 "$LIB_DIR/liblzma.so.5"


# ===== Download Alpine RootFS =====
echo "[*] Downloading Alpine rootfs..."

ROOTFS_DIR="$BASE_DIR/rootfsdevuan"
mkdir -p "$ROOTFS_DIR"
export LD_LIBRARY_PATH="$LIB_PATH:$LD_LIBRARY_PATH"
curl -f -L "https://dl.dropboxusercontent.com/scl/fi/018yluhvor7njmxbthupg/devuan-arm64-rootfs.tar.xz?rlkey=ytklvkoe97xw63njiejw5a85o&st=jtgvhg4g&raw=1" -o "devuan-arm64-rootfs.tar.xz"

# ===== Extract RootFS =====
echo "[*] Extracting rootfs..."
export LD_LIBRARY_PATH="$LIB_DIR"
/system/bin/linker64 "$BIN_DIR/xz" -dc /data/data/com.cpterminal/files/devuan-arm64-rootfs.tar.xz | /system/bin/tar -xf - -C "$ROOTFS_DIR"

# ===== Cleanup =====
rm devuan-arm64-rootfs.tar.xz

echo "[✓] RootFS ready at $ROOTFS_DIR"

# ===== Basic System config =====

echo "127.0.0.1 localhost localhost.localdomain" > "$ROOTFS_DIR/etc/hosts"
echo "::1 localhost localhost.localdomain" >> "$ROOTFS_DIR/etc/hosts"


echo "nameserver 8.8.8.8" > "$ROOTFS_DIR/etc/resolv.conf"
echo "nameserver 1.1.1.1" >> "$ROOTFS_DIR/etc/resolv.conf"
echo "[*] Checking resolv.conf:"
cat "$ROOTFS_DIR/etc/resolv.conf"
ls -l "$ROOTFS_DIR/etc/resolv.conf"