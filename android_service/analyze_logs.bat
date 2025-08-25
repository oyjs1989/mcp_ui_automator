@echo off
echo ========================================
echo Android应用日志分析工具
echo ========================================
echo.

REM 检查ADB连接
echo [1/5] 检查ADB连接...
adb devices | findstr "device$" >nul
if %errorlevel% neq 0 (
    echo 错误: 没有找到已连接的Android设备
    echo 请确保设备已连接并启用USB调试
    pause
    exit /b 1
)
echo ADB连接正常

REM 导出完整日志
echo.
echo [2/5] 导出设备日志...
adb logcat -d > full_device_log.txt
if %errorlevel% equ 0 (
    echo 日志已导出到: full_device_log.txt
) else (
    echo 错误: 导出日志失败
    pause
    exit /b 1
)

REM 提取应用相关日志
echo.
echo [3/5] 提取应用相关日志...
echo 正在提取UIAutomator相关日志...
findstr /i "UIAutomator\|DebugLogger\|HttpServer\|MainActivity\|com.mcp" full_device_log.txt > app_logs.txt
echo 应用日志已提取到: app_logs.txt

echo 正在提取错误日志...
findstr /i "error\|Error\|ERROR\|exception\|Exception\|EXCEPTION\|failed\|Failed\|FAILED" full_device_log.txt | findstr "com.mcp" > error_logs.txt
echo 错误日志已提取到: error_logs.txt

REM 分析关键问题
echo.
echo [4/5] 分析关键问题...
echo ========================================
echo 应用启动问题分析
echo ========================================

REM 检查前台服务类型错误
echo.
echo 检查前台服务类型错误:
findstr /i "MissingForegroundServiceTypeException\|FGS without a type" error_logs.txt
if %errorlevel% equ 0 (
    echo [发现] 前台服务类型错误 - 这是主要问题
    echo 解决方案: 已在AndroidManifest.xml中添加foregroundServiceType="dataSync"
) else (
    echo [通过] 未发现前台服务类型错误
)

REM 检查权限问题
echo.
echo 检查权限问题:
findstr /i "permission\|Permission\|PERMISSION" error_logs.txt
if %errorlevel% equ 0 (
    echo [发现] 权限相关问题
    findstr /i "permission\|Permission\|PERMISSION" error_logs.txt
) else (
    echo [通过] 未发现权限问题
)

REM 检查网络问题
echo.
echo 检查网络连接问题:
findstr /i "network\|Network\|NETWORK\|connection\|Connection\|CONNECTION" error_logs.txt
if %errorlevel% equ 0 (
    echo [发现] 网络相关问题
    findstr /i "network\|Network\|NETWORK\|connection\|Connection\|CONNECTION" error_logs.txt
) else (
    echo [通过] 未发现网络问题
)

REM 检查HTTP服务器启动
echo.
echo 检查HTTP服务器启动状态:
findstr /i "HTTP_SERVER_STARTED\|HTTP_SERVER_STOPPED\|Server listening" app_logs.txt
if %errorlevel% equ 0 (
    echo [发现] HTTP服务器状态信息:
    findstr /i "HTTP_SERVER_STARTED\|HTTP_SERVER_STOPPED\|Server listening" app_logs.txt
) else (
    echo [警告] 未找到HTTP服务器启动信息
)

REM 生成分析报告
echo.
echo [5/5] 生成分析报告...
echo ========================================
echo 日志分析报告
echo ========================================
echo 生成时间: %date% %time%
echo 设备日志文件: full_device_log.txt
echo 应用日志文件: app_logs.txt
echo 错误日志文件: error_logs.txt
echo.

REM 显示最近的错误日志
echo 最近的错误日志 (最后10条):
echo ----------------------------------------
if exist error_logs.txt (
    powershell "Get-Content error_logs.txt | Select-Object -Last 10"
) else (
    echo 未发现错误日志
)

echo.
echo ========================================
echo 分析完成！
echo ========================================
echo.
echo 建议操作:
echo 1. 如果发现前台服务类型错误，请重新安装应用
echo 2. 检查设备网络连接
echo 3. 确认应用权限设置
echo 4. 查看详细日志文件进行进一步分析
echo.
pause
