@echo off
echo ========================================
echo 调试日志分析工具
echo ========================================
echo.

REM 导出调试日志
echo [1/4] 导出调试日志...
adb logcat -d | findstr "DebugLogger" > debug_analysis.txt
echo 调试日志已导出到: debug_analysis.txt

REM 分析服务启动流程
echo.
echo [2/4] 分析服务启动流程...
echo ========================================
echo 服务启动时间线:
echo ========================================
findstr /i "Starting HTTP server\|HTTP_SERVER_STARTED\|Server listening\|Service Status" debug_analysis.txt

REM 分析错误信息
echo.
echo [3/4] 分析错误信息...
echo ========================================
echo 错误详情:
echo ========================================
findstr /i "error\|Error\|ERROR\|failed\|Failed\|FAILED\|exception\|Exception\|EXCEPTION" debug_analysis.txt

REM 分析UI状态更新
echo.
echo [4/4] 分析UI状态更新...
echo ========================================
echo UI状态更新:
echo ========================================
findstr /i "Updating UI\|Service running\|isRunning" debug_analysis.txt

echo.
echo ========================================
echo 分析完成！
echo ========================================
echo.
echo 主要发现:
echo 1. 检查前台服务类型权限是否缺失
echo 2. 检查WiFi权限是否缺失  
echo 3. 检查服务启动是否成功
echo 4. 检查UI状态更新是否正确
echo.
echo 详细日志文件: debug_analysis.txt
echo.
pause
