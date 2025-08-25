@echo off
echo UI Automator MCP Service - Build

echo 正在构建APK...
call gradlew assembleDebug

if %ERRORLEVEL% neq 0 (
    echo 构建失败！
    pause
    exit /b 1
)

echo 构建成功！
echo APK位置: app\build\outputs\apk\debug\app-debug.apk

echo 是否安装到设备？(Y/N)
set /p choice=

if /i "%choice%"=="Y" (
    adb install -r "app\build\outputs\apk\debug\app-debug.apk"
    if %ERRORLEVEL% equ 0 (
        echo 安装成功！
        adb shell am start -n com.mcp.uiautomator/.MainActivity
    )
)

pause
