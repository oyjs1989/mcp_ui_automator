@echo off
echo 快速日志分析工具
echo =================

REM 导出日志
echo 正在导出设备日志...
adb logcat -d > device_log.txt

REM 快速分析关键问题
echo.
echo 分析结果:
echo =================

REM 检查前台服务类型错误
echo 1. 前台服务类型错误:
findstr /i "MissingForegroundServiceTypeException\|FGS without a type" device_log.txt
if %errorlevel% equ 0 (
    echo [问题] 发现前台服务类型错误 - 需要修复
) else (
    echo [正常] 未发现前台服务类型错误
)

REM 检查应用崩溃
echo.
echo 2. 应用崩溃检查:
findstr /i "FATAL EXCEPTION\|AndroidRuntime" device_log.txt | findstr "com.mcp"
if %errorlevel% equ 0 (
    echo [问题] 发现应用崩溃
) else (
    echo [正常] 未发现应用崩溃
)

REM 检查HTTP服务器状态
echo.
echo 3. HTTP服务器状态:
findstr /i "HTTP_SERVER_STARTED\|Server listening" device_log.txt
if %errorlevel% equ 0 (
    echo [正常] HTTP服务器已启动
) else (
    echo [问题] HTTP服务器未启动
)

REM 检查权限问题
echo.
echo 4. 权限问题检查:
findstr /i "permission\|Permission" device_log.txt | findstr "com.mcp"
if %errorlevel% equ 0 (
    echo [问题] 发现权限问题
) else (
    echo [正常] 未发现权限问题
)

echo.
echo 详细日志已保存到: device_log.txt
echo 使用以下命令查看详细错误:
echo   findstr /i "error\|exception\|failed" device_log.txt
echo.
pause
