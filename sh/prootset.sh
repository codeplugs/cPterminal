#!/system/bin/sh

echo "[*] Starting proot + libtalloc setup..."

# ===== CONFIG =====
BASE_DIR="$(pwd)"
USR_DIR="$BASE_DIR/usr"
BIN_DIR="$USR_DIR/bin"
LIB_DIR="$USR_DIR/lib"

PROOT_BIN="$BIN_DIR/proot"
TALLOC_LIB="$LIB_DIR/libtalloc.so"

# URLs
PROOT_URL="https://proot.gitlab.io/proot/bin/proot"
TALLOC_URL="https://github.com/termux/termux-packages/releases/download/bootstrap/libtalloc.so"

# ===== STEP 1: Create directories =====
echo "[*] Creating usr/bin and usr/lib..."
mkdir -p "$BIN_DIR"
mkdir -p "$LIB_DIR"

# ===== STEP 2: Download proot =====
echo "[*] Downloading proot..."
if command -v curl >/dev/null 2>&1; then
    curl -L "$PROOT_URL" -o "$PROOT_BIN"
elif command -v wget >/dev/null 2>&1; then
    wget "$PROOT_URL" -O "$PROOT_BIN"
else
    echo "[!] curl or wget not found!"
    exit 1
fi

# Set permission (important)
chmod 755 "$PROOT_BIN"

# ===== STEP 3: Download libtalloc =====
echo "[*] Downloading libtalloc..."
if command -v curl >/dev/null 2>&1; then
    curl -L "$TALLOC_URL" -o "$TALLOC_LIB"
else
    wget "$TALLOC_URL" -O "$TALLOC_LIB"
fi

# Set permission
chmod 755 "$TALLOC_LIB"

# ===== STEP 4: Set environment =====
export PATH="$BIN_DIR:$PATH"
export LD_LIBRARY_PATH="$LIB_DIR:$LD_LIBRARY_PATH"

# ===== STEP 5: Test =====
echo "[*] Testing proot..."
if proot --help >/dev/null 2>&1; then
    echo "[✓] Proot is working!"
else
    echo "[!] Proot failed! Check library dependencies."
    exit 1
fi

# ===== STEP 6: Save environment =====
PROFILE="$HOME/.profile"

echo "[*] Saving environment..."

grep -q "$BIN_DIR" "$PROFILE" 2>/dev/null || \
echo "export PATH=$BIN_DIR:\$PATH" >> "$PROFILE"

grep -q "$LIB_DIR" "$PROFILE" 2>/dev/null || \
echo "export LD_LIBRARY_PATH=$LIB_DIR:\$LD_LIBRARY_PATH" >> "$PROFILE"

echo ""
echo "[✓] DONE!"
echo "[✓] Command ready: proot"
echo "[*] Reload shell if needed:"
echo "    source ~/.profile"