#!/usr/bin/env python3
"""
测试运行器

运行MCP UI Automator项目的所有测试
"""

import sys
import os
import unittest
import subprocess
import asyncio
from pathlib import Path

# 添加项目根目录到Python路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

def print_header(title):
    """打印测试标题"""
    print(f"\n{'='*60}")
    print(f"  {title}")
    print(f"{'='*60}")

def print_section(title):
    """打印测试段落标题"""
    print(f"\n🔍 {title}")
    print(f"{'-'*40}")

def check_dependencies():
    """检查依赖"""
    print_section("检查依赖")
    
    missing_deps = []
    
    # 检查基础依赖
    try:
        import aiohttp
        print("✅ aiohttp")
    except ImportError:
        missing_deps.append("aiohttp")
        print("❌ aiohttp")
    
    # 检查MCP依赖（可选）
    try:
        import mcp
        print("✅ mcp (完整测试可用)")
    except ImportError:
        print("⚠️  mcp (部分测试将被跳过)")
    
    # 检查测试依赖（可选）
    try:
        import pytest
        print("✅ pytest (可选)")
    except ImportError:
        print("⚠️  pytest (可选，使用unittest)")
    
    if missing_deps:
        print(f"\n❌ 缺少依赖: {', '.join(missing_deps)}")
        print("安装命令: pip install " + " ".join(missing_deps))
        return False
    
    return True

def run_unit_tests():
    """运行单元测试"""
    print_section("运行单元测试")
    
    test_files = [
        'tests/test_android_client.py',
        'tests/test_mcp_server.py'
    ]
    
    success = True
    
    for test_file in test_files:
        if os.path.exists(test_file):
            print(f"\n▶️  运行 {test_file}")
            try:
                # 运行单元测试
                result = subprocess.run([
                    sys.executable, '-m', 'unittest', 
                    test_file.replace('/', '.').replace('.py', '')
                ], capture_output=True, text=True)
                
                if result.returncode == 0:
                    print(f"✅ {test_file} 通过")
                else:
                    print(f"❌ {test_file} 失败")
                    print("STDOUT:", result.stdout)
                    print("STDERR:", result.stderr)
                    success = False
                    
            except Exception as e:
                print(f"❌ {test_file} 运行出错: {e}")
                success = False
        else:
            print(f"⚠️  测试文件未找到: {test_file}")
    
    return success

def run_integration_tests():
    """运行集成测试"""
    print_section("运行集成测试")
    
    try:
        from tests.test_integration import run_integration_tests
        return run_integration_tests()
    except Exception as e:
        print(f"❌ 集成测试运行出错: {e}")
        return False

def run_syntax_check():
    """运行语法检查"""
    print_section("语法检查")
    
    python_files = [
        'main.py',
        'server.py', 
        'android_client.py',
        'test_mcp_server.py'
    ]
    
    success = True
    
    for py_file in python_files:
        if os.path.exists(py_file):
            try:
                import py_compile
                py_compile.compile(py_file, doraise=True)
                print(f"✅ {py_file}")
            except py_compile.PyCompileError as e:
                print(f"❌ {py_file}: {e}")
                success = False
        else:
            print(f"⚠️  文件未找到: {py_file}")
    
    return success

def run_functionality_test():
    """运行功能测试"""
    print_section("基本功能测试")
    
    try:
        # 测试导入
        import android_client
        import main  # 这可能因MCP依赖失败
        
        print("✅ 模块导入成功")
        
        # 测试基本类实例化
        client = android_client.AndroidClient("localhost", 8080)
        print("✅ AndroidClient实例化成功")
        
        # 清理
        asyncio.run(client.close())
        
        return True
        
    except ImportError as e:
        if "mcp" in str(e).lower():
            print("⚠️  MCP依赖未安装，部分功能不可用")
            try:
                import android_client
                client = android_client.AndroidClient("localhost", 8080)
                asyncio.run(client.close())
                print("✅ AndroidClient功能正常")
                return True
            except Exception as e2:
                print(f"❌ AndroidClient测试失败: {e2}")
                return False
        else:
            print(f"❌ 导入失败: {e}")
            return False
    except Exception as e:
        print(f"❌ 功能测试失败: {e}")
        return False

def generate_test_report(results):
    """生成测试报告"""
    print_header("测试报告")
    
    total = len(results)
    passed = sum(1 for r in results.values() if r)
    failed = total - passed
    
    for test_name, success in results.items():
        status = "✅ PASS" if success else "❌ FAIL"
        print(f"{test_name.ljust(25)}: {status}")
    
    print(f"\n📊 总计: {passed}/{total} 测试通过")
    
    if passed == total:
        print("🎉 所有测试通过！项目功能正常。")
        return True
    else:
        print(f"⚠️  有 {failed} 个测试失败。请检查上述错误信息。")
        return False

def main():
    """主函数"""
    print_header("Android UI Automator MCP 测试套件")
    
    print("🧪 开始运行项目测试...")
    
    # 改变到脚本目录
    os.chdir(project_root)
    
    # 运行各种测试
    results = {}
    
    # 1. 检查依赖
    results["依赖检查"] = check_dependencies()
    
    # 2. 语法检查  
    results["语法检查"] = run_syntax_check()
    
    # 3. 基本功能测试
    results["功能测试"] = run_functionality_test()
    
    # 4. 单元测试
    if results["依赖检查"]:
        results["单元测试"] = run_unit_tests()
    else:
        print("⏭️  跳过单元测试（依赖不满足）")
        results["单元测试"] = False
    
    # 5. 集成测试  
    results["集成测试"] = run_integration_tests()
    
    # 生成报告
    all_passed = generate_test_report(results)
    
    if all_passed:
        print("\n🚀 项目准备就绪！可以开始部署使用。")
        return 0
    else:
        print("\n🔧 请修复失败的测试后再继续。")
        return 1

if __name__ == "__main__":
    try:
        exit_code = main()
        sys.exit(exit_code)
    except KeyboardInterrupt:
        print("\n\n⏹️  测试被用户中断")
        sys.exit(1)
    except Exception as e:
        print(f"\n\n💥 测试运行器发生未预期错误: {e}")
        sys.exit(1)
