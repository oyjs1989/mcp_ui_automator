# UI Automator MCP Service

Android UI自动化服务，提供HTTP API接口。

## 快速开始

1. 构建APK：
   ```bash
   cd android_service
   build_and_install.bat
   ```

2. 启动应用并启用调试模式

3. 点击"启动服务"开始HTTP服务器

4. 使用API：
   ```bash
   curl http://设备IP:8080/health
   curl http://设备IP:8080/ui/dump
   ```

## 调试

- 启用调试模式查看详细日志
- 日志文件：`/data/data/com.mcp.uiautomator/files/logs/`
- 导出日志：`adb pull /data/data/com.mcp.uiautomator/files/logs/ui_automator_debug.log`

## API端点

- `GET /health` - 健康检查
- `GET /ui/dump` - 获取UI树
- `POST /ui/click` - 点击元素
- `POST /ui/input` - 输入文本
- `GET /debug/logs` - 获取调试日志 