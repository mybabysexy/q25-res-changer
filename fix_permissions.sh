#!/system/bin/sh

# Resolution Changer - Advanced Permission Fixer Script
# This script handles the permission issues for Resolution Changer system app

echo "Resolution Changer - Advanced Permission Fixer"
echo "=============================================="

# Check if running as root
if [ "$(whoami)" != "root" ]; then
   echo "Error: This script must be run as root"
   echo "Please run: su"
   echo "Then run: sh fix_permissions.sh"
   exit 1
fi

APP_PACKAGE="com.duc1607.resolutionchanger"

echo "Checking if app is installed..."
pm list packages | grep $APP_PACKAGE
if [ $? -ne 0 ]; then
    echo "Error: App is not installed. Please install the system app first."
    exit 1
fi

echo "Checking app installation location..."
pm path $APP_PACKAGE

echo ""
echo "Method 1: Trying to grant permissions via pm grant..."

echo "Attempting WRITE_SECURE_SETTINGS..."
pm grant $APP_PACKAGE android.permission.WRITE_SECURE_SETTINGS 2>&1
echo "Result: $?"

echo "Attempting CHANGE_CONFIGURATION..."
pm grant $APP_PACKAGE android.permission.CHANGE_CONFIGURATION 2>&1
echo "Result: $?"

echo ""
echo "Method 2: Using appops to grant permissions..."

echo "Setting WRITE_SETTINGS via appops..."
appops set $APP_PACKAGE WRITE_SETTINGS allow
echo "Result: $?"

echo "Setting SYSTEM_ALERT_WINDOW via appops..."
appops set $APP_PACKAGE SYSTEM_ALERT_WINDOW allow
echo "Result: $?"

echo ""
echo "Method 3: Direct settings modification..."

echo "Adding to secure settings whitelist..."
settings put global policy_control "immersive.preconfirms=$APP_PACKAGE"
settings put secure enabled_accessibility_services "$APP_PACKAGE"

echo ""
echo "Method 4: Testing direct wm command execution..."

echo "Testing wm size command access..."
su -c "wm size 720x720" 2>/dev/null
if [ $? -eq 0 ]; then
    echo "✓ wm command accessible via su"
    # Restore default
    su -c "wm size reset"
else
    echo "✗ wm command not accessible even with su"
fi

echo ""
echo "Testing without su..."
wm size 720x720 2>/dev/null
if [ $? -eq 0 ]; then
    echo "✓ wm command accessible directly"
    wm size reset
else
    echo "✗ wm command not accessible directly"
fi

echo ""
echo "Permission fixing complete!"
echo ""
echo "If the app still doesn't work, the issue might be:"
echo "1. App needs to be signed with system certificate"
echo "2. App needs to be rebuilt and reinstalled"
echo "3. Device requires reboot after permission changes"
echo ""
echo "Try rebooting your device and testing again."
