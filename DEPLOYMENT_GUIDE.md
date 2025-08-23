# Android UI Automator MCP 部署指南

## 项目概览

这是一个基于模型上下文协议(MCP)的Android UI自动化工具，使AI助手能够直接操作Android设备界面。

### 架构

```
┌─────────────────┐    JSON-RPC 2.0     ┌─────────────────┐    HTTP API    ┌─────────────────┐
│   AI Assistant  │ ◄─────── stdio ────►│   MCP Server    │ ◄─── 8080 ───►│ Android Service │
│ (Claude/Cursor) │                     │    (Python)     │               │      (APK)      │
└─────────────────┘                     └─────────────────┘               └─────────────────┘
                                                                                    │
                                                                         UI Automator 2.0
                                                                                    │
                                                                          ┌─────────────────┐
                                                                          │ Android Device  │
                                                                          │   UI Elements   │
                                                                          └─────────────────┘
```

## 第一步：Android端部署

### 1.1 编译Android APK

```bash
cd android_service
./gradlew assembleDebug
```

### 1.2 安装到设备

```bash
# USB连接
adb install app/build/outputs/apk/debug/app-debug.apk

# 或从Android Studio直接安装
```

### 1.3 启动服务

1. **在设备上打开"UI Automator MCP"应用**
2. **授予必要权限** (系统警告窗权限)
3. **设置端口** (默认8080)
4. **点击"启动服务"**
5. **记录显示的服务地址** (例如: http://192.168.1.100:8080)

### 1.4 验证Android服务

```bash
# 测试健康检查
curl http://192.168.1.100:8080/health

# 获取UI信息
curl http://192.168.1.100:8080/ui/dump
```

## 第二步：MCP服务器部署

### 2.1 安装Python依赖

```bash
cd mcp_server
pip install -r requirements.txt
```

### 2.2 测试MCP服务器

```bash
# 测试与Android服务连接
python test_mcp_server.py --android-host 192.168.1.100 --android-port 8080
```

应该看到：
```
✅ Android service connection successful
✅ UI dump successful
✅ Device info successful
✅ Element detection successful
🎉 All tests passed! MCP server is ready to use.
```

## 第三步：AI助手集成

### 3.1 Claude Desktop

1. **找到配置文件位置:**
   - macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
   - Windows: `%APPDATA%\Claude\claude_desktop_config.json`

2. **编辑配置文件:**
   ```json
   {
     "mcpServers": {
       "android-ui-automator": {
         "command": "python",
         "args": [
           "/完整路径/mcp_ui_automator/mcp_server/main.py",
           "--android-host",
           "192.168.1.100"
         ],
         "cwd": "/完整路径/mcp_ui_automator/mcp_server"
       }
     }
   }
   ```

3. **重启Claude Desktop**

4. **验证连接:**
   - 打开新对话
   - 发送: "请获取Android设备的UI信息"
   - Claude应该能调用get_ui_dump工具

### 3.2 VS Code (需要MCP扩展)

1. **在项目根目录创建 `.vscode/mcp.json`:**
   ```json
   {
     "servers": {
       "android-ui-automator": {
         "command": "python",
         "args": [
           "/完整路径/mcp_ui_automator/mcp_server/main.py",
           "--android-host",
           "192.168.1.100"
         ],
         "cwd": "/完整路径/mcp_ui_automator/mcp_server"
       }
     }
   }
   ```

2. **重新加载VS Code窗口**

### 3.3 Cursor

1. **打开Cursor设置 (Cmd/Ctrl + ,)**

2. **搜索 "MCP" 或 "Model Context Protocol"**

3. **添加服务器配置:**
   ```json
   {
     "mcpServers": {
       "android-ui-automator": {
         "command": "python",
         "args": [
           "/完整路径/mcp_ui_automator/mcp_server/main.py",
           "--android-host",
           "192.168.1.100"
         ],
         "cwd": "/完整路径/mcp_ui_automator/mcp_server"
       }
     }
   }
   ```

4. **重启Cursor**

## 第四步：使用示例

### 基础UI操作

```
用户: "获取当前Android屏幕的UI信息"

AI助手会调用: get_ui_dump()
返回: 完整的UI元素树，包含所有可交互元素
```

```
用户: "点击登录按钮"

AI助手会:
1. 先调用 get_ui_dump() 查看界面
2. 找到登录按钮的resource_id
3. 调用 click_element({"resource_id": "com.example:id/login"})
```

```
用户: "在用户名框输入 john@example.com"

AI助手会:
1. 获取UI信息找到用户名输入框
2. 调用 input_text({
     "selector": {"resource_id": "com.example:id/username"},
     "text": "john@example.com"
   })
```

### 复杂自动化流程

```
用户: "帮我完成登录流程：用户名john@example.com，密码123456"

AI助手会:
1. get_ui_dump() - 分析当前界面
2. input_text() - 输入用户名
3. input_text() - 输入密码  
4. click_element() - 点击登录按钮
5. wait_for_element() - 等待页面跳转
6. get_ui_dump() - 确认登录结果
```

## 故障排除

### Android连接问题

1. **检查防火墙设置**
   ```bash
   # Android设备防火墙
   # Windows/macOS防火墙
   ```

2. **确认网络连通性**
   ```bash
   ping 192.168.1.100
   telnet 192.168.1.100 8080
   ```

3. **检查Android服务状态**
   - 在Android应用中查看服务状态
   - 查看应用日志

### MCP集成问题

1. **检查Python路径**
   ```bash
   which python
   python --version
   ```

2. **验证依赖安装**
   ```bash
   pip list | grep mcp
   ```

3. **查看MCP服务器日志**
   ```bash
   tail -f /tmp/mcp-ui-automator.log
   ```

4. **测试stdio通信**
   ```bash
   echo '{"jsonrpc":"2.0","method":"tools/list","id":1}' | python main.py --stdio
   ```

### 元素选择问题

1. **获取准确的UI信息**
   ```
   AI: "调用get_ui_dump获取当前界面信息"
   ```

2. **优先使用resource_id选择器**
   - 最稳定的元素标识
   - 不受界面语言影响

3. **备选选择器策略**
   - text: 精确文本匹配
   - text_contains: 部分文本匹配
   - class_name: 元素类型匹配

## 安全考虑

1. **网络安全**
   - Android服务运行在本地网络
   - 确保网络环境安全
   - 考虑使用VPN或SSH隧道

2. **权限管理**
   - UI Automator服务具有系统级权限
   - 仅在可信环境中运行
   - 定期检查Android权限授予情况

3. **数据保护**
   - UI信息可能包含敏感数据
   - 注意日志文件的访问权限
   - 不要在生产环境中启用调试模式

## 扩展配置

### 多设备支持

为每个设备运行独立的MCP服务器实例：

```json
{
  "mcpServers": {
    "android-device-1": {
      "command": "python",
      "args": ["main.py", "--android-host", "192.168.1.100"],
      "cwd": "/path/to/mcp_server"
    },
    "android-device-2": {
      "command": "python", 
      "args": ["main.py", "--android-host", "192.168.1.101"],
      "cwd": "/path/to/mcp_server"
    }
  }
}
```

### 高级调试

启用详细日志记录：

```json
{
  "mcpServers": {
    "android-ui-automator": {
      "command": "python",
      "args": [
        "/path/to/main.py",
        "--android-host", "192.168.1.100",
        "--debug"
      ],
      "cwd": "/path/to/mcp_server"
    }
  }
}
```

## 维护和更新

### 更新Android服务

1. 重新编译APK
2. 卸载旧版本: `adb uninstall com.mcp.uiautomator`
3. 安装新版本: `adb install app-debug.apk`

### 更新MCP服务器

1. 拉取最新代码
2. 更新Python依赖: `pip install -r requirements.txt --upgrade`
3. 重启AI助手应用

### 监控和日志

- **Android服务日志**: 通过应用界面查看
- **MCP服务器日志**: `/tmp/mcp-ui-automator.log`
- **AI助手日志**: 查看相应应用的日志文件

## 支持

如果遇到问题：

1. **查看故障排除部分**
2. **检查日志文件**
3. **运行测试脚本验证各组件**
4. **在GitHub提交Issue**

---

**完成部署后，您的AI助手就能直接操作Android设备了！** 🎉
