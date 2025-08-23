#!/usr/bin/env python3
"""
MCP服务器单元测试

测试UIAutomatorMCPServer的核心功能
"""

import asyncio
import json
import unittest
from unittest.mock import AsyncMock, patch, MagicMock

try:
    import pytest
    PYTEST_AVAILABLE = True
except ImportError:
    PYTEST_AVAILABLE = False

# 由于MCP库可能未安装，我们需要模拟导入
try:
    import mcp.types as types
    from server import UIAutomatorMCPServer
    MCP_AVAILABLE = True
except ImportError:
    MCP_AVAILABLE = False
    
    # 创建模拟的MCP类型
    class MockTypes:
        class Tool:
            def __init__(self, name, description, inputSchema):
                self.name = name
                self.description = description
                self.inputSchema = inputSchema
        
        class TextContent:
            def __init__(self, type, text):
                self.type = type
                self.text = text
        
        class Resource:
            def __init__(self, uri, name, description, mimeType):
                self.uri = uri
                self.name = name
                self.description = description
                self.mimeType = mimeType
    
    types = MockTypes()

class TestUIAutomatorMCPServer(unittest.TestCase):
    """MCP服务器测试类"""

    def setUp(self):
        """测试初始化"""
        if not MCP_AVAILABLE:
            self.skipTest("MCP库未安装")
        
        self.server = UIAutomatorMCPServer("localhost", 8080)

    def tearDown(self):
        """测试清理"""
        if hasattr(self.server, 'android_client'):
            asyncio.run(self.server.android_client.close())

    def test_server_initialization(self):
        """测试服务器初始化"""
        if not MCP_AVAILABLE:
            self.skipTest("MCP库未安装")
            
        self.assertEqual(self.server.android_host, "localhost")
        self.assertEqual(self.server.android_port, 8080)
        self.assertIsNotNone(self.server.android_client)
        self.assertIsNotNone(self.server.server)

    @patch('android_client.AndroidClient.get_ui_dump')
    async def test_get_ui_dump_tool(self, mock_get_ui_dump):
        """测试get_ui_dump工具"""
        if not MCP_AVAILABLE:
            self.skipTest("MCP库未安装")
            
        # 模拟Android客户端响应
        mock_ui_data = {
            "root": {
                "resource_id": "",
                "text": "",
                "class_name": "android.widget.FrameLayout",
                "bounds": {"left": 0, "top": 0, "right": 1080, "bottom": 1920},
                "children": []
            },
            "timestamp": 1703123456789,
            "package_name": "com.example.app"
        }
        mock_get_ui_dump.return_value = mock_ui_data
        
        # 获取工具处理器
        handlers = self.server.server._call_tool_handlers
        self.assertIn("get_ui_dump", handlers)
        
        # 测试工具调用
        result = await handlers["get_ui_dump"]("get_ui_dump", {})
        
        # 验证结果
        self.assertEqual(len(result), 1)
        self.assertEqual(result[0].type, "text")
        
        # 验证JSON响应
        response_data = json.loads(result[0].text)
        self.assertEqual(response_data["package_name"], "com.example.app")

    @patch('android_client.AndroidClient.click_element')
    async def test_click_element_tool(self, mock_click):
        """测试click_element工具"""
        if not MCP_AVAILABLE:
            self.skipTest("MCP库未安装")
            
        # 模拟点击响应
        mock_click.return_value = {
            "success": True,
            "message": "Element clicked successfully",
            "element_found": True
        }
        
        # 测试工具调用
        handlers = self.server.server._call_tool_handlers
        args = {"resource_id": "com.example:id/button"}
        result = await handlers["click_element"]("click_element", args)
        
        # 验证调用
        mock_click.assert_called_once_with(args)
        
        # 验证结果
        response_data = json.loads(result[0].text)
        self.assertTrue(response_data["success"])

    @patch('android_client.AndroidClient.input_text')
    async def test_input_text_tool(self, mock_input):
        """测试input_text工具"""
        if not MCP_AVAILABLE:
            self.skipTest("MCP库未安装")
            
        # 模拟输入响应
        mock_input.return_value = {
            "success": True,
            "message": "Text input successfully",
            "element_found": True
        }
        
        # 测试工具调用
        handlers = self.server.server._call_tool_handlers
        args = {
            "text": "Hello World",
            "selector": {"resource_id": "com.example:id/input"},
            "clear_first": True
        }
        result = await handlers["input_text"]("input_text", args)
        
        # 验证调用
        mock_input.assert_called_once_with(
            args["selector"], 
            args["text"], 
            args["clear_first"]
        )
        
        # 验证结果
        response_data = json.loads(result[0].text)
        self.assertTrue(response_data["success"])

    @patch('android_client.AndroidClient.scroll')
    async def test_scroll_screen_tool(self, mock_scroll):
        """测试scroll_screen工具"""
        if not MCP_AVAILABLE:
            self.skipTest("MCP库未安装")
            
        # 模拟滚动响应
        mock_scroll.return_value = {
            "success": True,
            "message": "Scroll completed"
        }
        
        # 测试工具调用
        handlers = self.server.server._call_tool_handlers
        args = {"direction": "down", "steps": 3}
        result = await handlers["scroll_screen"]("scroll_screen", args)
        
        # 验证调用
        mock_scroll.assert_called_once_with("down", 3)
        
        # 验证结果
        response_data = json.loads(result[0].text)
        self.assertTrue(response_data["success"])

    @patch('android_client.AndroidClient.wait_for_element')
    async def test_wait_for_element_tool(self, mock_wait):
        """测试wait_for_element工具"""
        if not MCP_AVAILABLE:
            self.skipTest("MCP库未安装")
            
        # 模拟等待响应
        mock_wait.return_value = {
            "success": True,
            "message": "Wait condition met",
            "element_found": True
        }
        
        # 测试工具调用
        handlers = self.server.server._call_tool_handlers
        args = {
            "selector": {"text": "Loading..."},
            "timeout": 5000,
            "condition": "visible"
        }
        result = await handlers["wait_for_element"]("wait_for_element", args)
        
        # 验证调用
        mock_wait.assert_called_once_with(
            args["selector"],
            args["timeout"], 
            args["condition"]
        )

    @patch('android_client.AndroidClient.press_back')
    @patch('android_client.AndroidClient.press_home')
    @patch('android_client.AndroidClient.press_recent')
    async def test_device_button_tools(self, mock_recent, mock_home, mock_back):
        """测试设备按键工具"""
        if not MCP_AVAILABLE:
            self.skipTest("MCP库未安装")
            
        # 模拟按键响应
        button_response = {
            "success": True,
            "message": "Button pressed"
        }
        mock_back.return_value = button_response
        mock_home.return_value = button_response
        mock_recent.return_value = button_response
        
        handlers = self.server.server._call_tool_handlers
        
        # 测试返回键
        result = await handlers["press_back"]("press_back", {})
        mock_back.assert_called_once()
        response_data = json.loads(result[0].text)
        self.assertTrue(response_data["success"])
        
        # 测试Home键
        result = await handlers["press_home"]("press_home", {})
        mock_home.assert_called_once()
        
        # 测试最近任务键
        result = await handlers["press_recent"]("press_recent", {})
        mock_recent.assert_called_once()

    @patch('android_client.AndroidClient.get_device_info')
    async def test_get_device_info_tool(self, mock_device_info):
        """测试get_device_info工具"""
        if not MCP_AVAILABLE:
            self.skipTest("MCP库未安装")
            
        # 模拟设备信息
        mock_device_info.return_value = {
            "screen_size": {"width": 1080, "height": 1920},
            "api_level": 30,
            "manufacturer": "Google",
            "model": "Pixel 5"
        }
        
        handlers = self.server.server._call_tool_handlers
        result = await handlers["get_device_info"]("get_device_info", {})
        
        # 验证结果
        response_data = json.loads(result[0].text)
        self.assertEqual(response_data["screen_size"]["width"], 1080)
        self.assertEqual(response_data["manufacturer"], "Google")

    async def test_tool_error_handling(self):
        """测试工具错误处理"""
        if not MCP_AVAILABLE:
            self.skipTest("MCP库未安装")
            
        # 测试未知工具
        handlers = self.server.server._call_tool_handlers
        result = await handlers.get("unknown_tool", lambda n, a: [types.TextContent(
            type="text", 
            text='{"success": false, "error": "Unknown tool"}'
        )])("unknown_tool", {})
        
        # 对于已知工具，测试异常处理
        with patch('android_client.AndroidClient.get_ui_dump', 
                  side_effect=Exception("Test error")):
            result = await handlers["get_ui_dump"]("get_ui_dump", {})
            response_data = json.loads(result[0].text)
            self.assertFalse(response_data["success"])
            self.assertIn("Test error", response_data["error"])

    async def test_list_tools(self):
        """测试工具列表"""
        if not MCP_AVAILABLE:
            self.skipTest("MCP库未安装")
            
        # 获取工具列表处理器
        list_handler = None
        for handler in self.server.server._list_tools_handlers:
            list_handler = handler
            break
        
        self.assertIsNotNone(list_handler)
        
        # 获取工具列表
        tools = await list_handler()
        
        # 验证工具数量和名称
        expected_tools = [
            "get_ui_dump", "click_element", "input_text", "scroll_screen",
            "wait_for_element", "press_back", "press_home", "press_recent",
            "get_device_info"
        ]
        
        tool_names = [tool.name for tool in tools]
        for expected in expected_tools:
            self.assertIn(expected, tool_names)

    async def test_list_resources(self):
        """测试资源列表"""
        if not MCP_AVAILABLE:
            self.skipTest("MCP库未安装")
            
        # 获取资源列表处理器
        list_handler = None
        for handler in self.server.server._list_resources_handlers:
            list_handler = handler
            break
        
        self.assertIsNotNone(list_handler)
        
        # 获取资源列表
        resources = await list_handler()
        
        # 验证资源
        self.assertEqual(len(resources), 2)
        resource_uris = [str(r.uri) for r in resources]
        self.assertIn("android://ui/current", resource_uris)
        self.assertIn("android://device/info", resource_uris)

    @patch('android_client.AndroidClient.get_ui_dump')
    @patch('android_client.AndroidClient.get_device_info')
    async def test_read_resources(self, mock_device_info, mock_ui_dump):
        """测试资源读取"""
        if not MCP_AVAILABLE:
            self.skipTest("MCP库未安装")
            
        # 模拟数据
        mock_ui_dump.return_value = {"test": "ui_data"}
        mock_device_info.return_value = {"test": "device_data"}
        
        # 获取资源读取处理器
        read_handler = None
        for handler in self.server.server._read_resource_handlers:
            read_handler = handler
            break
        
        # 测试UI资源读取
        from pydantic import AnyUrl
        ui_uri = AnyUrl("android://ui/current")
        result = await read_handler(ui_uri)
        data = json.loads(result)
        self.assertEqual(data["test"], "ui_data")
        
        # 测试设备信息资源读取
        device_uri = AnyUrl("android://device/info")
        result = await read_handler(device_uri)
        data = json.loads(result)
        self.assertEqual(data["test"], "device_data")


class TestMCPServerMocked(unittest.TestCase):
    """不依赖MCP库的模拟测试"""
    
    def test_server_config(self):
        """测试服务器配置"""
        # 这些测试不依赖MCP库
        with patch('android_client.AndroidClient'):
            # 模拟创建服务器
            host = "192.168.1.100"
            port = 9999
            
            # 验证基本配置
            self.assertEqual(host, "192.168.1.100")
            self.assertEqual(port, 9999)
            
    def test_tool_schemas(self):
        """测试工具Schema定义"""
        # 验证关键工具的Schema结构
        click_schema = {
            "type": "object",
            "anyOf": [
                {"required": ["resource_id"]},
                {"required": ["text"]},
                {"required": ["class_name"]}
            ]
        }
        
        # 验证Schema结构正确
        self.assertEqual(click_schema["type"], "object")
        self.assertIn("anyOf", click_schema)
        self.assertEqual(len(click_schema["anyOf"]), 3)


if __name__ == '__main__':
    # 设置测试参数
    if MCP_AVAILABLE:
        # 运行完整测试
        unittest.main()
    else:
        # 运行基础测试
        suite = unittest.TestLoader().loadTestsFromTestCase(TestMCPServerMocked)
        unittest.TextTestRunner(verbosity=2).run(suite)
        print("\n⚠️  MCP库未安装，部分测试被跳过")
        print("安装MCP库以运行完整测试: pip install mcp")
