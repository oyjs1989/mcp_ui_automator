# 测试指南

## 📋 测试概览

Android UI Automator MCP项目包含完整的测试套件，确保系统可靠性和功能正确性。

## 🧪 测试类型

### 1. 单元测试 (Unit Tests)
- **AndroidClient测试** (`test_android_client.py`) - HTTP客户端功能
- **MCP服务器测试** (`test_mcp_server.py`) - MCP工具和资源接口

### 2. 集成测试 (Integration Tests) 
- **端到端测试** (`test_integration.py`) - 完整工作流验证
- **Android服务集成** - 实际设备/模拟器通信测试

### 3. 功能测试 (Functional Tests)
- **语法验证** - Python代码语法检查
- **导入测试** - 模块导入能力验证
- **配置测试** - 参数和配置验证

## 🚀 快速开始

### 运行所有测试

```bash
cd mcp_server
python run_tests.py
```

### 运行特定测试类型

```bash
# 仅单元测试
python -m unittest tests.test_android_client

# 仅集成测试  
python tests/test_integration.py

# 使用pytest (如果已安装)
pytest tests/ -v
```

## 📦 测试依赖

### 必需依赖
```bash
pip install aiohttp
```

### 可选依赖
```bash
pip install mcp pytest pytest-asyncio
```

### 完整测试环境
```bash
pip install -r requirements.txt
```

## 🔧 测试环境配置

### 1. 无Android设备测试
```bash
# 运行模拟测试（不需要实际Android设备）
python -m unittest tests.test_android_client.TestAndroidClient.test_client_initialization
```

### 2. 有Android设备测试
```bash
# 1. 启动Android服务（在设备上）
# 2. 确保网络连接
ping 192.168.1.100

# 3. 运行完整测试
python run_tests.py
```

### 3. 模拟器测试
```bash
# 启动Android模拟器
emulator -avd test_device

# 端口转发
adb forward tcp:8080 tcp:8080

# 运行测试
python run_tests.py --android-host localhost
```

## 📊 测试结果解读

### 成功标识
- ✅ **PASS** - 测试通过
- 🎉 **完全成功** - 所有测试通过

### 警告标识  
- ⚠️ **跳过** - 测试被跳过（缺少依赖或环境）
- ⏭️ **可选失败** - 可选功能测试失败

### 错误标识
- ❌ **FAIL** - 测试失败，需要修复
- 💥 **崩溃** - 测试运行器错误

## 🐛 故障排除

### 常见问题

#### 1. MCP依赖未安装
```
错误: ImportError: No module named 'mcp'
解决: pip install mcp>=0.8.0
状态: 部分测试会被跳过，基本功能仍可测试
```

#### 2. Android服务连接失败  
```
错误: Connection refused
解决: 
  1. 确保Android应用已启动服务
  2. 检查IP地址和端口
  3. 验证网络连接
```

#### 3. 测试超时
```
错误: asyncio.TimeoutError
解决:
  1. 检查Android设备性能
  2. 增加超时时间
  3. 确保UI操作不冲突
```

#### 4. 权限错误
```
错误: Permission denied
解决:
  1. 在Android设备上授予系统警告窗权限
  2. 确保UI Automator权限正常
```

### 调试模式

```bash
# 启用详细日志
python run_tests.py --debug

# 单独运行失败的测试
python -m unittest tests.test_android_client.TestAndroidClient.test_health_check_success -v

# 查看测试覆盖率（如果安装了coverage）
coverage run -m pytest
coverage report
```

## 📈 测试最佳实践

### 1. 测试环境隔离
- 为每个测试创建独立的客户端实例
- 使用mock避免依赖外部服务
- 在tearDown中清理资源

### 2. 异步测试模式
```python
import asyncio
import unittest

class TestAsync(unittest.TestCase):
    def test_async_function(self):
        async def async_test():
            # 异步测试逻辑
            result = await some_async_function()
            self.assertTrue(result)
        
        asyncio.run(async_test())
```

### 3. 模拟外部依赖
```python
from unittest.mock import patch, AsyncMock

@patch('android_client.AndroidClient._make_request')
async def test_with_mock(self, mock_request):
    mock_request.return_value = {"success": True}
    # 测试逻辑
```

### 4. 测试数据验证
```python
def test_ui_data_structure(self):
    # 验证数据结构完整性
    self.assertIn("root", ui_dump)
    self.assertIsInstance(ui_dump["timestamp"], int)
    self._validate_ui_element(ui_dump["root"])
```

## 🔄 持续集成

### GitHub Actions配置
```yaml
name: Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - uses: actions/setup-python@v2
      with:
        python-version: 3.8
    - run: pip install -r requirements.txt  
    - run: python run_tests.py
```

### 本地持续测试
```bash
# 使用pytest-watch监控文件变化
pip install pytest-watch
ptw tests/
```

## 📝 添加新测试

### 1. 单元测试模板
```python
import unittest
from unittest.mock import AsyncMock, patch

class TestNewFeature(unittest.TestCase):
    def setUp(self):
        # 测试初始化
        pass
    
    def test_feature(self):
        # 测试逻辑
        self.assertTrue(True)
    
    async def test_async_feature(self):
        # 异步测试逻辑
        result = await some_function()
        self.assertIsNotNone(result)
```

### 2. 集成测试模板
```python
async def test_new_integration(self):
    """测试新的集成功能"""
    try:
        # 1. 准备测试环境
        client = AndroidClient("localhost", 8080)
        
        # 2. 执行测试操作
        result = await client.new_feature()
        
        # 3. 验证结果
        self.assertIn("expected_field", result)
        
    except Exception as e:
        self.fail(f"集成测试失败: {e}")
```

## 🎯 测试覆盖率

### 查看覆盖率
```bash
pip install coverage
coverage run -m pytest
coverage report -m
coverage html  # 生成HTML报告
```

### 目标覆盖率
- **单元测试**: > 80%
- **集成测试**: > 60% 
- **总体覆盖率**: > 70%

## 📚 相关文档

- [部署指南](../DEPLOYMENT_GUIDE.md) - 完整部署流程
- [MCP服务器文档](README.md) - 服务器使用说明
- [API规范](../api_spec.md) - HTTP API文档

---

**测试是确保项目质量的关键！** 🧪✨
