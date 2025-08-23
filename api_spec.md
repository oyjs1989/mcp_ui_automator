# Android UI Automator HTTP API 规范

## 概览

轻量级HTTP REST API，基于Android UI Automator框架，提供远程UI自动化能力。

**基础URL**: `http://device_ip:8080`

## API 端点

### 1. 获取UI信息

#### GET `/ui/dump`
获取当前屏幕的完整UI树结构

**响应示例**:
```json
{
  "root": {
    "resource_id": "",
    "text": "",
    "class_name": "android.widget.FrameLayout", 
    "bounds": {"left": 0, "top": 0, "right": 1080, "bottom": 1920},
    "clickable": false,
    "children": [
      {
        "resource_id": "com.example:id/button",
        "text": "Submit",
        "class_name": "android.widget.Button",
        "bounds": {"left": 100, "top": 500, "right": 300, "bottom": 600},
        "clickable": true,
        "children": []
      }
    ]
  },
  "timestamp": 1703123456789,
  "package_name": "com.example.app",
  "activity": "MainActivity"
}
```

#### GET `/ui/dump/xml`
获取原始XML格式的UI层次结构（兼容现有工具）

### 2. 元素交互

#### POST `/ui/click`
点击指定元素

**请求体**:
```json
{
  "selector": {
    "resource_id": "com.example:id/button"
  }
}
```

**可选选择器字段**:
- `resource_id`: 资源ID（推荐，最稳定）
- `text`: 精确文本匹配
- `text_contains`: 文本包含匹配
- `class_name`: 类名匹配
- `content_desc`: 内容描述匹配
- `bounds`: 坐标范围匹配

**响应**:
```json
{
  "success": true,
  "message": "Element clicked successfully",
  "timestamp": 1703123456789,
  "element_found": true
}
```

#### POST `/ui/input`
向输入框输入文本

**请求体**:
```json
{
  "selector": {
    "resource_id": "com.example:id/edittext"
  },
  "text": "Hello World",
  "clear_first": true
}
```

#### POST `/ui/scroll`
执行滚动操作

**请求体**:
```json
{
  "direction": "down",
  "steps": 3,
  "selector": {
    "resource_id": "com.example:id/recyclerview"
  }
}
```

**direction支持**: `"up"`, `"down"`, `"left"`, `"right"`

### 3. 等待操作

#### POST `/ui/wait`
等待元素出现

**请求体**:
```json
{
  "selector": {
    "resource_id": "com.example:id/loading"
  },
  "timeout": 5000,
  "condition": "visible"
}
```

**condition支持**: `"visible"`, `"gone"`, `"clickable"`

### 4. 设备操作

#### POST `/device/back`
按返回键

#### POST `/device/home`  
按Home键

#### POST `/device/recent`
按最近任务键

#### GET `/device/info`
获取设备信息

**响应**:
```json
{
  "screen_size": {"width": 1080, "height": 1920},
  "api_level": 30,
  "manufacturer": "Google",
  "model": "Pixel 5"
}
```

## 错误处理

所有API都返回标准的错误格式：

```json
{
  "success": false,
  "message": "Element not found",
  "error_code": "ELEMENT_NOT_FOUND",
  "timestamp": 1703123456789
}
```

**常见错误码**:
- `ELEMENT_NOT_FOUND`: 元素未找到
- `TIMEOUT`: 操作超时
- `INVALID_SELECTOR`: 选择器无效
- `OPERATION_FAILED`: 操作执行失败

## 使用示例

### 基本操作流程
```bash
# 1. 获取UI信息
curl http://192.168.1.100:8080/ui/dump

# 2. 点击按钮
curl -X POST http://192.168.1.100:8080/ui/click \
  -H "Content-Type: application/json" \
  -d '{"selector": {"resource_id": "com.example:id/button"}}'

# 3. 输入文本
curl -X POST http://192.168.1.100:8080/ui/input \
  -H "Content-Type: application/json" \
  -d '{"selector": {"resource_id": "com.example:id/search"}, "text": "hello"}'
```

### Python客户端示例
```python
import requests

class UIAutomatorClient:
    def __init__(self, host, port=8080):
        self.base_url = f"http://{host}:{port}"
    
    def get_ui_dump(self):
        response = requests.get(f"{self.base_url}/ui/dump")
        return response.json()
    
    def click_element(self, selector):
        response = requests.post(
            f"{self.base_url}/ui/click",
            json={"selector": selector}
        )
        return response.json()

# 使用示例
client = UIAutomatorClient("192.168.1.100")
ui_data = client.get_ui_dump()
result = client.click_element({"resource_id": "com.example:id/button"})
```
