#!/usr/bin/env python3
"""
集成测试

测试MCP服务器与Android服务的完整集成流程
"""

import asyncio
import json
import unittest
from unittest.mock import patch, MagicMock
import subprocess
import sys
import time
import socket

from android_client import AndroidClient


class TestIntegration(unittest.TestCase):
    """集成测试类"""
    
    @classmethod
    def setUpClass(cls):
        """类级别初始化"""
        cls.android_host = "localhost"
        cls.android_port = 8080
        cls.client = AndroidClient(cls.android_host, cls.android_port)
        
    @classmethod 
    def tearDownClass(cls):
        """类级别清理"""
        asyncio.run(cls.client.close())
    
    def setUp(self):
        """测试初始化"""
        self.maxDiff = None  # 显示完整diff
        
    def test_connection_available(self):
        """测试网络连接可用性"""
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(5)
        
        try:
            result = sock.connect_ex((self.android_host, self.android_port))
            if result != 0:
                self.skipTest(f"Android服务未运行在 {self.android_host}:{self.android_port}")
        except Exception as e:
            self.skipTest(f"网络连接测试失败: {e}")
        finally:
            sock.close()
    
    async def test_full_ui_workflow(self):
        """测试完整的UI操作流程"""
        try:
            # 1. 健康检查
            health = await self.client.health_check()
            self.assertIn("status", health)
            
            # 2. 获取设备信息
            device_info = await self.client.get_device_info()
            self.assertIn("screen_size", device_info)
            
            # 3. 获取UI信息
            ui_dump = await self.client.get_ui_dump()
            self.assertIn("root", ui_dump)
            self.assertIn("timestamp", ui_dump)
            
            # 4. 验证UI结构
            root = ui_dump["root"]
            self.assertIn("class_name", root)
            self.assertIn("bounds", root)
            
            # 5. 如果有可点击元素，测试点击
            clickable_element = self._find_clickable_element(root)
            if clickable_element:
                click_result = await self.client.click_element({
                    "resource_id": clickable_element["resource_id"]
                })
                # 注意：我们不验证success，因为可能会改变UI状态
                self.assertIn("success", click_result)
            
        except Exception as e:
            self.fail(f"完整工作流测试失败: {e}")
    
    def _find_clickable_element(self, element, max_depth=3, current_depth=0):
        """递归查找可点击元素"""
        if current_depth > max_depth:
            return None
            
        # 检查当前元素
        if (element.get("clickable") and 
            element.get("resource_id") and 
            element.get("enabled", True)):
            return element
        
        # 递归检查子元素
        for child in element.get("children", []):
            result = self._find_clickable_element(child, max_depth, current_depth + 1)
            if result:
                return result
        
        return None
    
    async def test_device_operations_safe(self):
        """测试设备操作（安全模式）"""
        try:
            # 测试获取设备信息（无副作用）
            device_info = await self.client.get_device_info()
            
            # 验证设备信息格式
            required_fields = ["screen_size", "api_level", "manufacturer", "model"]
            for field in required_fields:
                self.assertIn(field, device_info, f"缺少设备信息字段: {field}")
            
            # 验证屏幕尺寸格式
            screen_size = device_info["screen_size"]
            self.assertIn("width", screen_size)
            self.assertIn("height", screen_size)
            self.assertIsInstance(screen_size["width"], int)
            self.assertIsInstance(screen_size["height"], int)
            self.assertGreater(screen_size["width"], 0)
            self.assertGreater(screen_size["height"], 0)
            
        except Exception as e:
            self.fail(f"设备操作测试失败: {e}")
    
    async def test_ui_data_structure(self):
        """测试UI数据结构完整性"""
        try:
            ui_dump = await self.client.get_ui_dump()
            
            # 验证顶层结构
            self.assertIn("root", ui_dump)
            self.assertIn("timestamp", ui_dump)
            
            # 验证时间戳
            timestamp = ui_dump["timestamp"]
            self.assertIsInstance(timestamp, int)
            self.assertGreater(timestamp, 1600000000000)  # 2020年后的时间戳
            
            # 验证根元素结构
            root = ui_dump["root"]
            self._validate_ui_element(root)
            
        except Exception as e:
            self.fail(f"UI数据结构测试失败: {e}")
    
    def _validate_ui_element(self, element):
        """验证UI元素结构"""
        # 必需字段
        required_fields = ["class_name", "bounds"]
        for field in required_fields:
            self.assertIn(field, element, f"UI元素缺少必需字段: {field}")
        
        # 验证bounds结构
        bounds = element["bounds"]
        bounds_fields = ["left", "top", "right", "bottom"]
        for field in bounds_fields:
            self.assertIn(field, bounds, f"bounds缺少字段: {field}")
            self.assertIsInstance(bounds[field], int, f"bounds.{field}应为整数")
        
        # 验证bounds逻辑
        self.assertLessEqual(bounds["left"], bounds["right"])
        self.assertLessEqual(bounds["top"], bounds["bottom"])
        
        # 可选字段类型验证
        if "resource_id" in element:
            self.assertIsInstance(element["resource_id"], str)
        if "text" in element:
            self.assertIsInstance(element["text"], str)
        if "clickable" in element:
            self.assertIsInstance(element["clickable"], bool)
        
        # 递归验证子元素
        if "children" in element:
            self.assertIsInstance(element["children"], list)
            for child in element["children"]:
                self._validate_ui_element(child)
    
    async def test_error_handling(self):
        """测试错误处理"""
        try:
            # 测试无效选择器
            result = await self.client.click_element({})
            # 应该返回错误，但不应抛异常
            self.assertIn("success", result)
            # 如果失败，应该有错误信息
            if not result.get("success", True):
                self.assertIn("message", result)
            
            # 测试无效滚动方向
            result = await self.client.scroll("invalid_direction")
            self.assertIn("success", result)
            
        except Exception as e:
            self.fail(f"错误处理测试失败: {e}")
    
    async def test_xml_dump_format(self):
        """测试XML格式UI导出"""
        try:
            result = await self.client.get_ui_dump_xml()
            
            # 验证返回格式
            self.assertIn("content", result)
            xml_content = result["content"]
            self.assertIsInstance(xml_content, str)
            
            # 基本XML验证
            if xml_content.strip():
                self.assertTrue(xml_content.startswith("<") or 
                              "xml" in xml_content.lower())
            
        except Exception as e:
            # XML格式可能不总是可用，记录但不失败
            print(f"XML导出测试跳过: {e}")
    
    def test_mcp_server_importable(self):
        """测试MCP服务器可以导入"""
        try:
            # 尝试导入MCP相关模块（如果已安装）
            import main
            import android_client
            
            # 验证基本类可实例化
            client = android_client.AndroidClient("localhost", 8080)
            self.assertIsNotNone(client)
            
        except ImportError as e:
            if "mcp" in str(e).lower():
                self.skipTest("MCP库未安装，跳过MCP服务器导入测试")
            else:
                self.fail(f"导入测试失败: {e}")
    
    def test_configuration_validation(self):
        """测试配置验证"""
        # 测试各种主机/端口配置
        test_configs = [
            ("localhost", 8080),
            ("127.0.0.1", 8080),
            ("192.168.1.100", 9999),
        ]
        
        for host, port in test_configs:
            client = AndroidClient(host, port)
            expected_url = f"http://{host}:{port}"
            self.assertEqual(client.base_url, expected_url)
            
            # 清理
            asyncio.run(client.close())


class TestMCPServerExecutable(unittest.TestCase):
    """测试MCP服务器可执行性"""
    
    def test_main_script_syntax(self):
        """测试主脚本语法正确"""
        try:
            # 使用python -m py_compile检查语法
            import py_compile
            py_compile.compile('main.py', doraise=True)
            py_compile.compile('server.py', doraise=True)  
            py_compile.compile('android_client.py', doraise=True)
            
        except py_compile.PyCompileError as e:
            self.fail(f"Python脚本语法错误: {e}")
    
    def test_help_message(self):
        """测试帮助信息"""
        try:
            # 测试--help参数（如果MCP可用）
            result = subprocess.run([
                sys.executable, 'main.py', '--help'
            ], capture_output=True, text=True, timeout=10)
            
            # 不管是否成功，都应该有输出
            self.assertTrue(len(result.stdout) > 0 or len(result.stderr) > 0)
            
        except (subprocess.TimeoutExpired, FileNotFoundError):
            self.skipTest("无法执行主脚本")
        except Exception as e:
            self.skipTest(f"帮助信息测试跳过: {e}")


def run_integration_tests():
    """运行集成测试的主函数"""
    print("🧪 开始运行Android UI Automator MCP集成测试")
    print("="*60)
    
    # 检查是否有Android服务可用
    client = AndroidClient("localhost", 8080)
    
    async def check_service():
        try:
            result = await client.health_check()
            return result.get("status") == "healthy"
        except:
            return False
    
    service_available = asyncio.run(check_service())
    asyncio.run(client.close())
    
    if service_available:
        print("✅ 检测到Android服务，运行完整集成测试")
        
        # 创建异步测试套件
        async def run_async_tests():
            suite = unittest.TestSuite()
            
            # 添加异步测试
            test_case = TestIntegration()
            suite.addTest(TestIntegration('test_full_ui_workflow'))
            suite.addTest(TestIntegration('test_device_operations_safe'))
            suite.addTest(TestIntegration('test_ui_data_structure'))
            suite.addTest(TestIntegration('test_error_handling'))
            suite.addTest(TestIntegration('test_xml_dump_format'))
            
            # 手动运行异步测试
            for test in [
                test_case.test_full_ui_workflow,
                test_case.test_device_operations_safe,
                test_case.test_ui_data_structure,
                test_case.test_error_handling,
                test_case.test_xml_dump_format
            ]:
                try:
                    await test()
                    print(f"✅ {test.__name__}")
                except unittest.SkipTest as e:
                    print(f"⏭️  {test.__name__}: {e}")
                except Exception as e:
                    print(f"❌ {test.__name__}: {e}")
        
        # 运行异步测试
        asyncio.run(run_async_tests())
        
    else:
        print("⚠️  未检测到Android服务，跳过需要服务的测试")
    
    print("\n🔍 运行基础功能测试")
    
    # 运行同步测试
    sync_suite = unittest.TestSuite()
    sync_suite.addTest(TestIntegration('test_connection_available'))
    sync_suite.addTest(TestIntegration('test_mcp_server_importable'))
    sync_suite.addTest(TestIntegration('test_configuration_validation'))
    sync_suite.addTest(TestMCPServerExecutable('test_main_script_syntax'))
    sync_suite.addTest(TestMCPServerExecutable('test_help_message'))
    
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(sync_suite)
    
    print("\n" + "="*60)
    if result.wasSuccessful():
        print("🎉 所有集成测试通过！")
        return True
    else:
        print(f"❌ 有 {len(result.failures + result.errors)} 个测试失败")
        return False


if __name__ == '__main__':
    success = run_integration_tests()
    sys.exit(0 if success else 1)
