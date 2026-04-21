#!/system/bin/sh

echo "[*] Setting up proot command..."

BASE_DIR="$(pwd)"
USR_DIR="$BASE_DIR/usr"
BIN_DIR="$USR_DIR/bin"
LIB_DIR="$USR_DIR/lib"

mkdir -p "$BIN_DIR" "$LIB_DIR"



# ===== Download binary =====
echo "[*] Downloading proot..."
curl -f -L "https://dl.dropboxusercontent.com/scl/fi/ql02zp6p697qqt2nprbld/proot?rlkey=6xvve1zgs547gb36wxsa04eyf&st=zs7gzzb2&raw=1" -o "$BIN_DIR/proot.bin"
chmod 755 "$BIN_DIR/proot.bin"

# ===== Download library =====
echo "[*] Downloading libtalloc..."
curl -f -L "https://dl.dropboxusercontent.com/scl/fi/17wvspcb416m83vyi23q4/libtalloc.so.2?rlkey=mxew3cs03z86x0n860op46atp&st=4u744fi6&raw=1" -o "$LIB_DIR/libtalloc.so.2"
chmod 755 "$LIB_DIR/libtalloc.so.2"


# ===== Download Alpine RootFS =====
echo "[*] Downloading Alpine rootfs..."

ROOTFS_DIR="$BASE_DIR/rootfs"
mkdir -p "$ROOTFS_DIR"

curl -L https://cdimage.debian.org/mirror/alpinelinux.org/v3.21/releases/aarch64/alpine-minirootfs-3.21.5-aarch64.tar.gz -o alpine.tar.gz

# ===== Extract RootFS =====
echo "[*] Extracting rootfs..."

tar -xzf alpine.tar.gz -C "$ROOTFS_DIR"

# ===== Cleanup =====
rm alpine.tar.gz

echo "[✓] RootFS ready at $ROOTFS_DIR"

# ===== Basic System config =====

echo "127.0.0.1 localhost localhost.localdomain" > "$ROOTFS_DIR/etc/hosts"
echo "::1 localhost localhost.localdomain" >> "$ROOTFS_DIR/etc/hosts"


echo "nameserver 8.8.8.8" > "$ROOTFS_DIR/etc/resolv.conf"
echo "nameserver 1.1.1.1" >> "$ROOTFS_DIR/etc/resolv.conf"
echo "[*] Checking resolv.conf:"
cat "$ROOTFS_DIR/etc/resolv.conf"
ls -l "$ROOTFS_DIR/etc/resolv.conf"

echo "Welcome to Alpine!" > "$ROOTFS_DIR/etc/motd"
echo "" >> "$ROOTFS_DIR/etc/motd"
echo "The Alpine Wiki contains a large amount of how-to information about administrating Alpine systems." >> "$ROOTFS_DIR/etc/motd"
echo "See <https://wiki.alpinelinux.org/>." >> "$ROOTFS_DIR/etc/motd"
echo "" >> "$ROOTFS_DIR/etc/motd"
echo "Installing : apk add <pkg>" >> "$ROOTFS_DIR/etc/motd"
echo "Updating : apk update && apk upgrade" >> "$ROOTFS_DIR/etc/motd"

# ===== Remove setup alpine =====
rm -f "$ROOTFS_DIR/sbin/setup-alpine"
rm -rf "$ROOTFS_DIR/etc/alpine-release" 2>/dev/null



# ===== Create wrapper command =====
echo "[*] Creating proot command..."