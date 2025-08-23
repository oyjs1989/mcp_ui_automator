#!/usr/bin/env python3
"""
Android客户端单元测试

测试AndroidClient与模拟Android服务的通信功能
"""

import asyncio
import json
import unittest
from unittest.mock import AsyncMock, patch, MagicMock
import aiohttp

try:
    import pytest
    PYTEST_AVAILABLE = True
except ImportError:
    PYTEST_AVAILABLE = False

from android_client import AndroidClient

class TestAndroidClient(unittest.TestCase):
    """Android客户端测试类"""

    def setUp(self):
        """测试初始化"""
        self.client = AndroidClient("localhost", 8080)

    def tearDown(self):
        """测试清理"""
        asyncio.run(self.client.close())

    @patch('aiohttp.ClientSession.request')
    def test_health_check_success(self, mock_request):
        """测试健康检查成功"""
        # 模拟成功响应
        mock_response = AsyncMock()
        mock_response.status = 200
        mock_response.content_type = 'application/json'
        mock_response.json.return_value = {
            "status": "healthy",
            "timestamp": 1703123456789,
            "version": "1.0.0"
        }
        mock_request.return_value.__aenter__.return_value = mock_response

        result = asyncio.run(self.client.health_check())
        
        # 验证结果
        self.assertEqual(result["status"], "healthy")
        self.assertIn("timestamp", result)
        mock_request.assert_called_once()

    @patch('aiohttp.ClientSession.request')
    async def test_get_ui_dump_success(self, mock_request):
        """测试获取UI信息成功"""
        mock_ui_data = {
            "root": {
                "resource_id": "",
                "text": "",
                "class_name": "android.widget.FrameLayout",
                "bounds": {"left": 0, "top": 0, "right": 1080, "bottom": 1920},
                "clickable": False,
                "children": [
                    {
                        "resource_id": "com.example:id/button",
                        "text": "Click Me",
                        "class_name": "android.widget.Button",
                        "bounds": {"left": 100, "top": 500, "right": 300, "bottom": 600},
                        "clickable": True,
                        "children": []
                    }
                ]
            },
            "timestamp": 1703123456789,
            "package_name": "com.example.app"
        }

        mock_response = AsyncMock()
        mock_response.status = 200
        mock_response.content_type = 'application/json'
        mock_response.json.return_value = mock_ui_data
        mock_request.return_value.__aenter__.return_value = mock_response

        result = await self.client.get_ui_dump()
        
        # 验证结果
        self.assertIn("root", result)
        self.assertEqual(result["package_name"], "com.example.app")
        self.assertEqual(len(result["root"]["children"]), 1)

    @patch('aiohttp.ClientSession.request')
    async def test_click_element_success(self, mock_request):
        """测试点击元素成功"""
        mock_response_data = {
            "success": True,
            "message": "Element clicked successfully",
            "timestamp": 1703123456789,
            "element_found": True
        }

        mock_response = AsyncMock()
        mock_response.status = 200
        mock_response.content_type = 'application/json'
        mock_response.json.return_value = mock_response_data
        mock_request.return_value.__aenter__.return_value = mock_response

        selector = {"resource_id": "com.example:id/button"}
        result = await self.client.click_element(selector)
        
        # 验证结果
        self.assertTrue(result["success"])
        self.assertTrue(result["element_found"])
        
        # 验证请求参数
        args, kwargs = mock_request.call_args
        self.assertEqual(args[0], "POST")
        self.assertEqual(args[1], "http://localhost:8080/ui/click")
        self.assertEqual(kwargs["json"]["selector"], selector)

    @patch('aiohttp.ClientSession.request')
    async def test_input_text_success(self, mock_request):
        """测试文本输入成功"""
        mock_response_data = {
            "success": True,
            "message": "Text input successfully",
            "timestamp": 1703123456789,
            "element_found": True
        }

        mock_response = AsyncMock()
        mock_response.status = 200
        mock_response.content_type = 'application/json'
        mock_response.json.return_value = mock_response_data
        mock_request.return_value.__aenter__.return_value = mock_response

        selector = {"resource_id": "com.example:id/input"}
        text = "Hello World"
        result = await self.client.input_text(selector, text, True)
        
        # 验证结果
        self.assertTrue(result["success"])
        
        # 验证请求参数
        args, kwargs = mock_request.call_args
        expected_data = {
            "selector": selector,
            "text": text,
            "clear_first": True
        }
        self.assertEqual(kwargs["json"], expected_data)

    @patch('aiohttp.ClientSession.request')
    async def test_scroll_success(self, mock_request):
        """测试滚动成功"""
        mock_response_data = {
            "success": True,
            "message": "Scroll completed",
            "timestamp": 1703123456789
        }

        mock_response = AsyncMock()
        mock_response.status = 200
        mock_response.content_type = 'application/json'
        mock_response.json.return_value = mock_response_data
        mock_request.return_value.__aenter__.return_value = mock_response

        result = await self.client.scroll("down", 3)
        
        # 验证结果
        self.assertTrue(result["success"])
        
        # 验证请求参数
        args, kwargs = mock_request.call_args
        expected_data = {
            "direction": "down",
            "steps": 3
        }
        self.assertEqual(kwargs["json"], expected_data)

    @patch('aiohttp.ClientSession.request')
    async def test_wait_for_element_success(self, mock_request):
        """测试等待元素成功"""
        mock_response_data = {
            "success": True,
            "message": "Wait condition met",
            "timestamp": 1703123456789,
            "element_found": True
        }

        mock_response = AsyncMock()
        mock_response.status = 200
        mock_response.content_type = 'application/json'
        mock_response.json.return_value = mock_response_data
        mock_request.return_value.__aenter__.return_value = mock_response

        selector = {"text": "Loading..."}
        result = await self.client.wait_for_element(selector, 5000, "visible")
        
        # 验证结果
        self.assertTrue(result["success"])
        self.assertTrue(result["element_found"])

    @patch('aiohttp.ClientSession.request')  
    async def test_device_buttons(self, mock_request):
        """测试设备按键操作"""
        mock_response_data = {
            "success": True,
            "message": "Button pressed",
            "timestamp": 1703123456789
        }

        mock_response = AsyncMock()
        mock_response.status = 200
        mock_response.content_type = 'application/json'
        mock_response.json.return_value = mock_response_data
        mock_request.return_value.__aenter__.return_value = mock_response

        # 测试返回键
        result = await self.client.press_back()
        self.assertTrue(result["success"])
        
        # 测试Home键
        result = await self.client.press_home()
        self.assertTrue(result["success"])
        
        # 测试最近任务键
        result = await self.client.press_recent()
        self.assertTrue(result["success"])

    @patch('aiohttp.ClientSession.request')
    async def test_get_device_info_success(self, mock_request):
        """测试获取设备信息成功"""
        mock_device_data = {
            "screen_size": {"width": 1080, "height": 1920},
            "api_level": 30,
            "manufacturer": "Google", 
            "model": "Pixel 5",
            "version": "11"
        }

        mock_response = AsyncMock()
        mock_response.status = 200
        mock_response.content_type = 'application/json'
        mock_response.json.return_value = mock_device_data
        mock_request.return_value.__aenter__.return_value = mock_response

        result = await self.client.get_device_info()
        
        # 验证结果
        self.assertEqual(result["screen_size"]["width"], 1080)
        self.assertEqual(result["api_level"], 30)
        self.assertEqual(result["manufacturer"], "Google")

    @patch('aiohttp.ClientSession.request')
    async def test_http_error_handling(self, mock_request):
        """测试HTTP错误处理"""
        mock_response = AsyncMock()
        mock_response.status = 500
        mock_response.content_type = 'application/json'
        mock_response.json.return_value = {
            "success": False,
            "error": "Internal server error"
        }
        mock_request.return_value.__aenter__.return_value = mock_response

        result = await self.client.health_check()
        
        # 验证错误响应
        self.assertEqual(result["http_status"], 500)
        self.assertFalse(result["success"])

    @patch('aiohttp.ClientSession.request')
    async def test_network_error_handling(self, mock_request):
        """测试网络错误处理"""
        # 模拟网络连接错误
        mock_request.side_effect = aiohttp.ClientConnectorError(
            connection_key=None, os_error=None
        )

        result = await self.client.health_check()
        
        # 验证错误响应
        self.assertFalse(result["success"])
        self.assertEqual(result["error_type"], "client_error")
        self.assertIn("HTTP client error", result["error"])

    def test_client_initialization(self):
        """测试客户端初始化"""
        client = AndroidClient("192.168.1.100", 9999)
        
        self.assertEqual(client.host, "192.168.1.100")
        self.assertEqual(client.port, 9999)
        self.assertEqual(client.base_url, "http://192.168.1.100:9999")

    def test_url_construction(self):
        """测试URL构造"""
        client = AndroidClient("test.com", 8080)
        self.assertEqual(client.base_url, "http://test.com:8080")


# Pytest异步测试运行器（如果pytest可用）
if PYTEST_AVAILABLE:
    @pytest.mark.asyncio
    class TestAndroidClientPytest:
        """使用pytest的异步测试"""
        
        @pytest.fixture
        def client(self):
            """测试客户端fixture"""
            return AndroidClient("localhost", 8080)
        
        async def test_session_management(self, client):
            """测试会话管理"""
            # 测试会话创建
            session = await client._get_session()
            assert session is not None
            assert isinstance(session, aiohttp.ClientSession)
            
            # 测试会话复用
            session2 = await client._get_session()
            assert session is session2
            
            # 测试会话关闭
            await client.close()
            assert client.session is None or client.session.closed


if __name__ == '__main__':
    # 运行unittest测试
    unittest.main()
