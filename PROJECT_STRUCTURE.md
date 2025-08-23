# 项目结构

## 📁 清理后的项目组织

```
mcp_ui_automator/                    # 项目根目录
├── 📱 android_service/              # Android应用项目
│   ├── app/
│   │   ├── build.gradle            # Android应用配置
│   │   ├── proguard-rules.pro      # 代码混淆规则
│   │   └── src/main/
│   │       ├── AndroidManifest.xml # Android清单文件
│   │       ├── java/com/mcp/uiautomator/
│   │       │   ├── core/
│   │       │   │   └── UIAutomatorHelper.kt    # UI Automator封装
│   │       │   ├── model/
│   │       │   │   ├── ApiModels.kt            # API数据模型
│   │       │   │   └── UIElement.kt            # UI元素模型
│   │       │   ├── server/
│   │       │   │   └── HttpServer.kt           # HTTP服务器
│   │       │   ├── service/
│   │       │   │   └── UIAutomatorService.kt   # Android后台服务
│   │       │   └── MainActivity.kt             # 主界面Activity
│   │       └── res/                            # Android资源文件
│   │           ├── layout/activity_main.xml    # 主界面布局
│   │           ├── values/                     # 值资源
│   │           └── xml/                        # XML配置
│   ├── build.gradle                # 项目级构建配置
│   ├── gradle.properties           # Gradle属性
│   └── settings.gradle             # Gradle设置
├── 🐍 mcp_server/                  # MCP服务器项目
│   ├── main.py                     # MCP服务器主入口
│   ├── server.py                   # MCP服务器核心实现
│   ├── android_client.py           # Android HTTP客户端
│   ├── requirements.txt            # Python依赖列表
│   ├── pytest.ini                 # Pytest配置
│   ├── run_tests.py               # 测试运行器
│   ├── configs/                    # 配置示例
│   │   ├── claude_desktop_config.json  # Claude Desktop配置
│   │   └── vscode_mcp.json             # VS Code配置
│   ├── tests/                      # 测试套件
│   │   ├── __init__.py
│   │   ├── test_android_client.py  # Android客户端测试
│   │   ├── test_integration.py     # 集成测试
│   │   └── test_mcp_server.py      # MCP服务器测试
│   ├── README.md                   # MCP服务器文档
│   └── TESTING.md                  # 测试指南
├── 📄 schemas/                     # 数据结构定义
│   └── ui_schema.json             # UI元素JSON Schema
├── 📖 README.md                    # 项目概览
├── 📋 DEPLOYMENT_GUIDE.md          # 部署指南
├── 🧪 TEST_REPORT.md               # 测试报告
├── 📜 LICENSE                      # 许可证
├── 🔧 api_spec.md                  # API规范文档
├── 🚫 .gitignore                   # Git忽略文件
└── 📊 PROJECT_STRUCTURE.md         # 本文档
```

## 🧹 清理完成的项目

### ✅ 已移除的不必要文件
- 🗑️ `__pycache__/` - Python编译缓存目录
- 🗑️ `mcp_server/test_mcp_server.py` - 重复的测试文件
- 🗑️ `mcp_server/tests/test_simple.py` - 临时调试测试文件

### 📁 核心目录说明

#### 📱 `android_service/` - Android端实现
- **功能**: 提供UI Automator HTTP API服务
- **技术栈**: Kotlin + Android SDK + UI Automator 2.0 + Ktor
- **部署**: 编译为APK，安装到Android设备

#### 🐍 `mcp_server/` - Python MCP服务器
- **功能**: 标准MCP协议服务器，连接AI助手和Android设备
- **技术栈**: Python + aiohttp + MCP协议
- **部署**: 作为MCP服务器运行，支持stdio传输

#### 📄 `schemas/` - 数据结构定义
- **功能**: JSON Schema定义，确保数据格式一致性
- **用途**: API文档、客户端生成、数据验证

### 🏗️ 架构关系

```
AI助手 (Claude/Cursor) 
    ↕ [JSON-RPC 2.0 via stdio]
MCP服务器 (Python) 
    ↕ [HTTP REST API]
Android服务 (APK)
    ↕ [UI Automator 2.0]
Android设备UI
```

### 📋 文件用途说明

#### 核心实现文件 🔧
- `main.py` - MCP服务器启动入口，处理命令行参数
- `server.py` - MCP协议实现，工具和资源定义
- `android_client.py` - HTTP客户端，与Android服务通信
- `UIAutomatorHelper.kt` - UI Automator API封装
- `HttpServer.kt` - Android端HTTP服务器实现

#### 配置和文档 📚
- `requirements.txt` - Python依赖管理
- `build.gradle` - Android构建配置  
- `README.md` - 各模块使用说明
- `DEPLOYMENT_GUIDE.md` - 完整部署流程
- `api_spec.md` - HTTP API接口规范

#### 测试和质量 🧪
- `run_tests.py` - 智能测试运行器
- `test_*.py` - 完整测试套件
- `pytest.ini` - 测试框架配置
- `TEST_REPORT.md` - 测试验证报告

### 📊 项目统计

| 类型 | 数量 | 说明 |
|------|------|------|
| **Kotlin文件** | 7个 | Android端核心实现 |
| **Python文件** | 6个 | MCP服务器和测试 |
| **配置文件** | 8个 | 构建、部署、测试配置 |
| **文档文件** | 7个 | 使用指南和API文档 |
| **测试文件** | 4个 | 完整测试覆盖 |

### 🎯 项目特点

#### ✨ **简洁性**
- 清晰的目录结构
- 每个文件都有明确的用途
- 无冗余或临时文件

#### 🔧 **可维护性**  
- 模块化设计
- 完整的测试覆盖
- 详尽的文档说明

#### 🚀 **生产就绪**
- 企业级代码质量
- 完整的部署指南
- 健壮的错误处理

---

**🎊 项目清理完成！结构清晰，文件精简，准备投入使用！**
