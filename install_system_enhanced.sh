#!/system/bin/sh

# Resolution Changer - Enhanced System App Installation Script
# This script installs the Resolution Changer app as a privileged system app with proper permissions

echo "Resolution Changer - Enhanced System App Installer"
echo "================================================="

# Check if running as root
if [ "$(whoami)" != "root" ]; then
   echo "Error: This script must be run as root"
   echo "Please run: su"
   echo "Then run: sh install_system_enhanced.sh"
   exit 1
fi

# Variables
APK_NAME="app-debug.apk"
APP_PACKAGE="com.duc1607.resolutionchanger"
PRIV_APP_DIR="/system/priv-app/ResolutionChanger"
PERMISSIONS_DIR="/system/etc/permissions"
PERMISSION_FILE="privapp-permissions-resolutionchanger.xml"

echo "Checking for APK file..."
if [ ! -f "$APK_NAME" ]; then
    echo "Error: $APK_NAME not found in current directory"
    echo "Please copy the APK to this directory first"
    exit 1
fi

echo "Making system partition writable..."
mount -o remount,rw /system
if [ $? -ne 0 ]; then
    echo "Trying alternative mount command..."
    mount -o rw,remount /system
    if [ $? -ne 0 ]; then
        echo "Error: Could not make system partition writable"
        exit 1
    fi
fi

echo "Creating privileged app directory..."
mkdir -p "$PRIV_APP_DIR"

echo "Copying APK to system partition..."
cp "$APK_NAME" "$PRIV_APP_DIR/ResolutionChanger.apk"

echo "Setting proper permissions for APK..."
chmod 644 "$PRIV_APP_DIR/ResolutionChanger.apk"
chown root:root "$PRIV_APP_DIR/ResolutionChanger.apk"
chmod 755 "$PRIV_APP_DIR"

echo "Creating privileged app permissions file..."
cat > "$PERMISSIONS_DIR/$PERMISSION_FILE" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<permissions>
    <privapp-permissions package="com.duc1607.resolutionchanger">
        <permission name="android.permission.WRITE_SECURE_SETTINGS"/>
        <permission name="android.permission.CHANGE_CONFIGURATION"/>
        <permission name="android.permission.MANAGE_ACTIVITY_STACKS"/>
        <permission name="android.permission.INTERNAL_SYSTEM_WINDOW"/>
        <permission name="android.permission.STATUS_BAR_SERVICE"/>
        <permission name="android.permission.WRITE_SETTINGS"/>
        <permission name="android.permission.SYSTEM_ALERT_WINDOW"/>
    </privapp-permissions>
</permissions>
EOF

echo "Setting permissions for permission file..."
chmod 644 "$PERMISSIONS_DIR/$PERMISSION_FILE"
chown root:root "$PERMISSIONS_DIR/$PERMISSION_FILE"

echo "Creating SELinux policy (if needed)..."
# Some devices may need additional SELinux policies
if [ -d "/system/etc/selinux" ]; then
    echo "SELinux directory found, you may need to add custom policies"
fi

echo "Making system partition read-only again..."
mount -o remount,ro /system

echo "Installation complete!"
echo ""
echo "The Resolution Changer app has been installed as a privileged system app."
echo "Installed files:"
echo "  - APK: $PRIV_APP_DIR/ResolutionChanger.apk"
echo "  - Permissions: $PERMISSIONS_DIR/$PERMISSION_FILE"
echo ""
echo "Rebooting the device is recommended to apply changes."
reboot
